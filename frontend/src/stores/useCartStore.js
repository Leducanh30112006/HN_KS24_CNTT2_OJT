import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import axiosClient from '../api/axiosClient';
import { useAuthStore, registerOnLogoutListener } from './useAuthStore';

const CART_STORAGE_KEY = 'quickshop_cart';
const OFFLINE_CART_PREFIX = 'quickshop_offline_cart_';
const MOCK_JWT_TOKEN = 'mock_jwt_token_for_quickshop_local_demo';
const CART_CHANNEL_NAME = 'quickshop_cart_channel';

let lastLoginSyncKey = null;
let lastLoginSyncAt = 0;

// Kênh BroadcastChannel để đồng bộ tức thời giữa các tab / cửa sổ trình duyệt
let cartBroadcastChannel = null;
if (typeof window !== 'undefined' && typeof window.BroadcastChannel === 'function') {
  try {
    cartBroadcastChannel = new BroadcastChannel(CART_CHANNEL_NAME);
  } catch (e) {
    cartBroadcastChannel = null;
  }
}

const broadcastCartChange = (payload) => {
  if (cartBroadcastChannel) {
    try {
      cartBroadcastChannel.postMessage(payload);
    } catch (e) {
      // Bỏ qua lỗi gửi broadcast channel
    }
  }
};

const isNumericId = (val) => val !== null && val !== undefined && /^\d+$/.test(String(val).trim());

const getOfflineCartKey = (userId) => `${OFFLINE_CART_PREFIX}${userId}`;

const readOfflineUserCart = (userId) => {
  if (!userId || typeof localStorage === 'undefined') return [];
  try {
    const raw = localStorage.getItem(getOfflineCartKey(userId));
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const writeOfflineUserCart = (userId, items) => {
  if (!userId || typeof localStorage === 'undefined') return;
  try {
    localStorage.setItem(getOfflineCartKey(userId), JSON.stringify(items || []));
  } catch (err) {
    console.warn('Không thể lưu snapshot giỏ hàng cục bộ:', err);
  }
};

const clearOfflineUserCart = (userId) => {
  if (!userId || typeof localStorage === 'undefined') return;
  try {
    localStorage.removeItem(getOfflineCartKey(userId));
  } catch {
    // Bỏ qua lỗi xóa storage
  }
};

const parseVariantAttributes = (variantName, fallbackColor, fallbackSize) => {
  if (fallbackColor || fallbackSize) {
    return {
      color: fallbackColor || '',
      size: fallbackSize || '',
    };
  }
  if (!variantName || typeof variantName !== 'string') {
    return { color: '', size: '' };
  }
  const parts = variantName.split(/\s*[-/|]\s*/).filter(Boolean);
  if (parts.length >= 2) {
    return { color: parts[0], size: parts.slice(1).join(' - ') };
  }
  return { color: variantName, size: '' };
};

const normalizeCartItem = (raw, prevItem = null) => {
  const attrs = parseVariantAttributes(
    raw?.variantName,
    raw?.color ?? prevItem?.color,
    raw?.size ?? prevItem?.size
  );
  const variantLabel =
    raw?.variantName ??
    prevItem?.variantName ??
    ([attrs.color, attrs.size].filter(Boolean).join(' - ') || 'Mặc định');

  return {
    productId: raw?.productId ?? prevItem?.productId,
    variantId: raw?.variantId ?? prevItem?.variantId,
    sku: raw?.sku ?? prevItem?.sku ?? '',
    productName: raw?.productName ?? prevItem?.productName ?? 'Sản phẩm QuickShop',
    variantName: variantLabel,
    color: attrs.color,
    size: attrs.size,
    price: Number(raw?.price ?? prevItem?.price ?? 0),
    mainImage: raw?.imageUrl || raw?.mainImage || prevItem?.mainImage || '',
    quantity: Number(raw?.quantity ?? prevItem?.quantity ?? 1),
    shopId: raw?.shopId ?? prevItem?.shopId ?? 'shop-default',
    shopName: raw?.shopName ?? prevItem?.shopName ?? 'QuickShop Official',
    availableStock: raw?.availableStock ?? raw?.stock ?? prevItem?.availableStock ?? null,
    outOfStock: Boolean(raw?.outOfStock ?? false),
    quantityExceedsStock: Boolean(raw?.quantityExceedsStock ?? false),
  };
};

const mapServerCartToItems = (cartData, existingItems = []) => {
  if (!cartData || !Array.isArray(cartData.shops)) return [];
  const prevByVariantId = new Map(
    (existingItems || []).map((item) => [String(item.variantId), item])
  );

  const flattened = [];
  for (const shopGroup of cartData.shops) {
    const shopItems = Array.isArray(shopGroup?.items) ? shopGroup.items : [];
    for (const srvItem of shopItems) {
      const prev = prevByVariantId.get(String(srvItem.variantId));
      flattened.push(
        normalizeCartItem(
          {
            ...srvItem,
            shopId: srvItem.shopId ?? shopGroup.shopId,
            shopName: srvItem.shopName ?? shopGroup.shopName,
          },
          prev
        )
      );
    }
  }
  return flattened;
};

const extractCartResponse = (res) => {
  if (!res) return null;
  if (Array.isArray(res.shops)) return res;
  if (res.data && Array.isArray(res.data.shops)) return res.data;
  if (res.data?.data && Array.isArray(res.data.data.shops)) return res.data.data;
  return res.data || res;
};

const extractMergeResponse = (res) => {
  if (!res) return null;
  if (res.cart) return res;
  if (res.data?.cart) return res.data;
  if (res.data?.data?.cart) return res.data.data;
  return res.data || res;
};

const mergeLocalItemArrays = (baseItems = [], incomingItems = []) => {
  const byVariant = new Map();
  for (const item of baseItems) {
    byVariant.set(String(item.variantId), normalizeCartItem(item));
  }
  for (const incoming of incomingItems) {
    const key = String(incoming.variantId);
    const existing = byVariant.get(key);
    if (existing) {
      const maxStock = existing.availableStock ?? incoming.availableStock;
      const summed = existing.quantity + Number(incoming.quantity || 1);
      const nextQty = maxStock !== null && maxStock > 0 ? Math.min(summed, maxStock) : summed;
      byVariant.set(key, normalizeCartItem({ ...existing, ...incoming, quantity: nextQty }, existing));
    } else {
      byVariant.set(key, normalizeCartItem(incoming));
    }
  }
  return Array.from(byVariant.values());
};

const getAuthContext = () => {
  const { user, isAuthenticated, accessToken } = useAuthStore.getState();
  const userId = user?.userId ?? null;
  // Bất cứ khi nào tài khoản đã đăng nhập có userId hợp lệ, kích hoạt kết nối backend Redis
  const canUseServer = Boolean(isAuthenticated && userId);
  return { user, userId, isAuthenticated: Boolean(isAuthenticated), isMock: !accessToken || accessToken === MOCK_JWT_TOKEN, canUseServer };
};

const extractServerErrorMessage = (error, fallbackMessage) => {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    (error?.response || typeof error?.status === 'number' ? error?.message : null) ||
    (typeof error?.response?.data === 'string' ? error.response.data : null) ||
    fallbackMessage
  );
};

export const useCartStore = create(
  persist(
    (set, get) => ({
      items: [],
      ownerUserId: null,
      syncMode: 'guest',
      isSyncing: false,
      syncError: null,

      clearSyncError: () => set({ syncError: null }),

      // Lấy toàn bộ giỏ hàng từ order-service khi đã đăng nhập (Redis Cart)
      fetchCart: async (explicitUserId = null) => {
        const auth = getAuthContext();
        const userId = explicitUserId ?? auth.userId;
        if (!auth.isAuthenticated || !userId) {
          return { ok: false, offline: true };
        }

        set({ isSyncing: true, syncError: null });

        try {
          const res = await axiosClient.get('/api/cart', {
            params: { userId },
            headers: { 'X-User-Id': String(userId) },
          });
          const cartData = extractCartResponse(res);
          const mappedItems = mapServerCartToItems(cartData, get().items);

          // Cập nhật snapshot offline để đồng bộ backup
          writeOfflineUserCart(userId, mappedItems);
          set({
            items: mappedItems,
            ownerUserId: userId,
            syncMode: 'server',
            isSyncing: false,
            syncError: null,
          });

          // Phát tín hiệu đồng bộ cho các tab / cửa sổ khác
          broadcastCartChange({ type: 'CART_UPDATED', userId, items: mappedItems });
          return { ok: true, items: mappedItems };
        } catch (error) {
          // Khi server chưa sẵn sàng hoặc offline, sử dụng giỏ hàng offline lưu riêng theo userId
          const fallbackItems =
            String(get().ownerUserId) === String(userId) && get().items.length > 0
              ? get().items
              : readOfflineUserCart(userId);
          set({
            items: fallbackItems,
            ownerUserId: userId,
            syncMode: 'offline',
            isSyncing: false,
            syncError: error?.response?.status === 401 ? null : extractServerErrorMessage(error, null),
          });
          return { ok: false, offline: true, items: fallbackItems };
        }
      },

      // Đồng bộ giỏ hàng khách sang tài khoản người dùng sau khi đăng nhập
      mergeCartOnLogin: async (userId) => {
        const currentState = get();
        const guestItems =
          currentState.ownerUserId === null ? currentState.items : [];
        const auth = getAuthContext();
        const targetUserId = userId ?? auth.userId;

        if (!targetUserId) return { ok: false };

        const offlineSaved = readOfflineUserCart(targetUserId);

        const serverEligibleGuestItems = guestItems.filter((item) => isNumericId(item.variantId));
        if (serverEligibleGuestItems.length === 0) {
          return get().fetchCart(targetUserId);
        }

        set({ isSyncing: true, syncError: null });
        try {
          const requestId =
            typeof crypto !== 'undefined' && crypto.randomUUID
              ? crypto.randomUUID()
              : `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

          const res = await axiosClient.post(
            '/api/cart/merge',
            {
              requestId,
              items: serverEligibleGuestItems.map((item) => ({
                variantId: Number(item.variantId),
                quantity: item.quantity,
              })),
            },
            {
              params: { userId: targetUserId },
              headers: { 'X-User-Id': String(targetUserId) },
            }
          );

          const mergeData = extractMergeResponse(res);
          const mergedCart = mergeData?.cart;
          const mappedItems = mergedCart
            ? mapServerCartToItems(mergedCart, currentState.items)
            : [];

          writeOfflineUserCart(targetUserId, mappedItems);
          set({
            items: mappedItems,
            ownerUserId: targetUserId,
            syncMode: 'server',
            isSyncing: false,
            syncError:
              Array.isArray(mergeData?.warnings) && mergeData.warnings.length > 0
                ? mergeData.warnings.join(' | ')
                : null,
          });

          broadcastCartChange({ type: 'CART_UPDATED', userId: targetUserId, items: mappedItems });
          return { ok: true, items: mappedItems, warnings: mergeData?.warnings || [] };
        } catch (error) {
          const mergedFallback = mergeLocalItemArrays(offlineSaved, guestItems);
          writeOfflineUserCart(targetUserId, mergedFallback);
          set({
            items: mergedFallback,
            ownerUserId: targetUserId,
            syncMode: 'offline',
            isSyncing: false,
            syncError: extractServerErrorMessage(error, null),
          });
          broadcastCartChange({ type: 'CART_UPDATED', userId: targetUserId, items: mergedFallback });
          return { ok: false, offline: true, items: mergedFallback };
        }
      },

      // Điểm vào chuẩn hóa đồng bộ giỏ hàng ngay sau khi login hoặc khi tải lại app
      syncCartOnLogin: async (userId, options = {}) => {
        const auth = getAuthContext();
        const targetUserId = userId ?? auth.userId;
        if (!targetUserId || !auth.isAuthenticated) {
          return { ok: false };
        }

        const now = Date.now();
        const syncKey = String(targetUserId);
        if (options.dedupeRecent && lastLoginSyncKey === syncKey && now - lastLoginSyncAt < 1000) {
          return { ok: true, skipped: true, items: get().items };
        }

        const { ownerUserId, items, isSyncing } = get();
        if (isSyncing) {
          return { ok: true, skipped: true };
        }

        lastLoginSyncKey = syncKey;
        lastLoginSyncAt = now;

        // Chỉ merge nếu các item hiện tại thực sự thuộc phiên khách (ownerUserId === null)
        if (ownerUserId === null && items.length > 0) {
          return get().mergeCartOnLogin(targetUserId);
        }

        // Nếu vừa đổi tài khoản khác mà chưa qua logout, reset tránh rò rỉ dữ liệu
        if (ownerUserId !== null && String(ownerUserId) !== String(targetUserId)) {
          set({ items: [], ownerUserId: targetUserId, syncError: null });
        }

        return get().fetchCart(targetUserId);
      },

      // Xóa sạch giỏ hàng đang hiển thị khi đăng xuất (giữ an toàn tách biệt từng tài khoản)
      handleLogoutClear: (prevUserId = null) => {
        const { items, ownerUserId, syncMode } = get();
        const resolvedOwner = prevUserId ?? ownerUserId;

        if (resolvedOwner && syncMode !== 'server' && items.length > 0) {
          writeOfflineUserCart(resolvedOwner, items);
        }

        set({
          items: [],
          ownerUserId: null,
          syncMode: 'guest',
          isSyncing: false,
          syncError: null,
        });

        if (typeof localStorage !== 'undefined') {
          localStorage.removeItem(CART_STORAGE_KEY);
        }
      },

      // Thêm sản phẩm vào giỏ hàng + đồng bộ API nếu đã đăng nhập
      addToCart: async (product, variant, quantity = 1) => {
        const auth = getAuthContext();
        const prevItems = get().items;
        const targetVariantId = variant?.variantId || product?.defaultVariantId || product?.id;
        const existingItem = prevItems.find((item) => String(item.variantId) === String(targetVariantId));

        const maxStock =
          variant?.stock ??
          variant?.availableStock ??
          existingItem?.availableStock ??
          null;

        if (maxStock !== null && maxStock <= 0) {
          const msg = 'Sản phẩm hiện đã hết hàng.';
          set({ syncError: msg });
          return { ok: false, error: msg };
        }

        const desiredQty = (existingItem ? existingItem.quantity : 0) + quantity;
        if (maxStock !== null && desiredQty > maxStock) {
          const msg = `Chỉ còn ${maxStock} sản phẩm khả dụng trong kho.`;
          set({ syncError: msg });
          return { ok: false, error: msg };
        }

        const newItem = normalizeCartItem({
          productId: product.id || product.productId,
          variantId: targetVariantId,
          sku: variant?.sku || product.sku || '',
          productName: product.name || product.productName,
          variantName:
            variant?.variantName ||
            ([variant?.color, variant?.size].filter(Boolean).join(' - ') || 'Mặc định'),
          color: variant?.color || '',
          size: variant?.size || '',
          price: variant?.price ?? product.minPrice ?? product.price ?? 0,
          mainImage: variant?.imageUrl || product.mainImage || product.imageUrl || '',
          quantity: existingItem ? desiredQty : quantity,
          shopId: product.shopId || 'shop-default',
          shopName: product.shopName || 'QuickShop Official',
          availableStock: maxStock,
        });

        const nextItems = existingItem
          ? prevItems.map((item) =>
              String(item.variantId) === String(targetVariantId) ? newItem : item
            )
          : [...prevItems, newItem];

        set({
          items: nextItems,
          ownerUserId: auth.isAuthenticated ? auth.userId : null,
          syncError: null,
        });

        if (auth.canUseServer && isNumericId(targetVariantId)) {
          try {
            const res = await axiosClient.post(
              '/api/cart/items',
              {
                variantId: Number(targetVariantId),
                quantity,
              },
              {
                params: { userId: auth.userId },
                headers: { 'X-User-Id': String(auth.userId) },
              }
            );
            const cartData = extractCartResponse(res);
            const mapped = mapServerCartToItems(cartData, nextItems);
            writeOfflineUserCart(auth.userId, mapped);
            set({
              items: mapped,
              ownerUserId: auth.userId,
              syncMode: 'server',
              syncError: null,
            });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: mapped });
            return { ok: true, items: mapped };
          } catch (error) {
            const status = error?.response?.status ?? error?.status;
            if (status && status >= 400 && status < 500) {
              const msg = extractServerErrorMessage(error, 'Không thể thêm sản phẩm vào giỏ hàng.');
              set({ items: prevItems, syncError: msg });
              return { ok: false, error: msg };
            }
            writeOfflineUserCart(auth.userId, nextItems);
            set({ syncMode: 'offline' });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
            return { ok: true, offline: true, items: nextItems };
          }
        }

        if (auth.isAuthenticated && auth.userId) {
          writeOfflineUserCart(auth.userId, nextItems);
          set({ syncMode: 'offline' });
          broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
        }
        return { ok: true, items: nextItems };
      },

      // Cập nhật số lượng + đồng bộ backend
      updateQuantity: async (variantId, newQuantity) => {
        if (newQuantity <= 0) {
          return get().removeFromCart(variantId);
        }

        const auth = getAuthContext();
        const prevItems = get().items;
        const target = prevItems.find((item) => String(item.variantId) === String(variantId));
        if (target?.availableStock !== null && target?.availableStock !== undefined && newQuantity > target.availableStock) {
          const msg = `Số lượng tối đa còn lại trong kho là ${target.availableStock}.`;
          set({ syncError: msg });
          return { ok: false, error: msg };
        }

        const nextItems = prevItems.map((item) =>
          String(item.variantId) === String(variantId)
            ? { ...item, quantity: newQuantity, quantityExceedsStock: false }
            : item
        );

        set({ items: nextItems, syncError: null });

        if (auth.canUseServer && isNumericId(variantId)) {
          try {
            const res = await axiosClient.put(
              `/api/cart/items/${Number(variantId)}`,
              { quantity: newQuantity },
              {
                params: { userId: auth.userId },
                headers: { 'X-User-Id': String(auth.userId) },
              }
            );
            const cartData = extractCartResponse(res);
            const mapped = mapServerCartToItems(cartData, nextItems);
            writeOfflineUserCart(auth.userId, mapped);
            set({ items: mapped, ownerUserId: auth.userId, syncMode: 'server', syncError: null });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: mapped });
            return { ok: true, items: mapped };
          } catch (error) {
            const status = error?.response?.status ?? error?.status;
            if (status && status >= 400 && status < 500) {
              const msg = extractServerErrorMessage(error, 'Không thể cập nhật số lượng sản phẩm.');
              set({ items: prevItems, syncError: msg });
              return { ok: false, error: msg };
            }
            writeOfflineUserCart(auth.userId, nextItems);
            set({ syncMode: 'offline' });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
            return { ok: true, offline: true };
          }
        }

        if (auth.isAuthenticated && auth.userId) {
          writeOfflineUserCart(auth.userId, nextItems);
          set({ syncMode: 'offline' });
          broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
        }
        return { ok: true };
      },

      // Xóa 1 sản phẩm khỏi giỏ hàng
      removeFromCart: async (variantId) => {
        const auth = getAuthContext();
        const prevItems = get().items;
        const nextItems = prevItems.filter((item) => String(item.variantId) !== String(variantId));

        set({ items: nextItems, syncError: null });

        if (auth.canUseServer && isNumericId(variantId)) {
          try {
            const res = await axiosClient.delete(`/api/cart/items/${Number(variantId)}`, {
              params: { userId: auth.userId },
              headers: { 'X-User-Id': String(auth.userId) },
            });
            const cartData = extractCartResponse(res);
            const mapped = mapServerCartToItems(cartData, nextItems);
            writeOfflineUserCart(auth.userId, mapped);
            set({ items: mapped, ownerUserId: auth.userId, syncMode: 'server', syncError: null });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: mapped });
            return { ok: true, items: mapped };
          } catch (error) {
            const status = error?.response?.status ?? error?.status;
            if (status && status >= 400 && status < 500) {
              set({ items: prevItems, syncError: extractServerErrorMessage(error, 'Không thể xóa sản phẩm.') });
              return { ok: false };
            }
            writeOfflineUserCart(auth.userId, nextItems);
            set({ syncMode: 'offline' });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
            return { ok: true, offline: true };
          }
        }

        if (auth.isAuthenticated && auth.userId) {
          writeOfflineUserCart(auth.userId, nextItems);
          broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
        }
        return { ok: true };
      },

      // Xóa nhiều sản phẩm đã đặt hoặc được chọn cùng lúc
      removeMultipleFromCart: async (variantIds) => {
        const idSet = new Set((variantIds || []).map((id) => String(id)));
        if (idSet.size === 0) return { ok: true };

        const auth = getAuthContext();
        const prevItems = get().items;
        const nextItems = prevItems.filter((item) => !idSet.has(String(item.variantId)));

        set({ items: nextItems, syncError: null });

        const numericIds = Array.from(idSet).filter(isNumericId);
        if (auth.canUseServer && numericIds.length > 0) {
          try {
            const results = await Promise.allSettled(
              numericIds.map((id) =>
                axiosClient.delete(`/api/cart/items/${Number(id)}`, {
                  params: { userId: auth.userId },
                  headers: { 'X-User-Id': String(auth.userId) },
                })
              )
            );
            const lastSuccess = [...results]
              .reverse()
              .find((r) => r.status === 'fulfilled');
            if (lastSuccess) {
              const cartData = extractCartResponse(lastSuccess.value);
              const mapped = mapServerCartToItems(cartData, nextItems);
              writeOfflineUserCart(auth.userId, mapped);
              set({ items: mapped, ownerUserId: auth.userId, syncMode: 'server', syncError: null });
              broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: mapped });
              return { ok: true, items: mapped };
            }
            writeOfflineUserCart(auth.userId, nextItems);
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
            return { ok: false };
          } catch {
            writeOfflineUserCart(auth.userId, nextItems);
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
            return { ok: false, offline: true };
          }
        }

        if (auth.isAuthenticated && auth.userId) {
          writeOfflineUserCart(auth.userId, nextItems);
          broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: nextItems });
        }
        return { ok: true };
      },

      // Xóa toàn bộ giỏ hàng hiện tại của người dùng
      clearCart: async () => {
        const auth = getAuthContext();
        set({ items: [], syncError: null });

        if (auth.canUseServer) {
          try {
            await axiosClient.delete('/api/cart', {
              params: { userId: auth.userId },
              headers: { 'X-User-Id': String(auth.userId) },
            });
            clearOfflineUserCart(auth.userId);
            set({ syncMode: 'server' });
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: [] });
            return { ok: true };
          } catch {
            clearOfflineUserCart(auth.userId);
            broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: [] });
            return { ok: false, offline: true };
          }
        }

        if (auth.isAuthenticated && auth.userId) {
          clearOfflineUserCart(auth.userId);
          broadcastCartChange({ type: 'CART_UPDATED', userId: auth.userId, items: [] });
        }
        return { ok: true };
      },

      getTotalCount: () => {
        return get().items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
      },

      getTotalAmount: () => {
        return get().items.reduce(
          (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0),
          0
        );
      },

      getTotalPrice: () => {
        return get().getTotalAmount();
      },

      getItemsByShop: () => {
        const groups = {};
        for (const item of get().items) {
          const sid = item.shopId || 'shop-default';
          if (!groups[sid]) {
            groups[sid] = {
              shopId: sid,
              shopName: item.shopName || 'QuickShop Official',
              items: [],
            };
          }
          groups[sid].items.push(item);
        }
        return Object.values(groups);
      },
    }),
    {
      name: CART_STORAGE_KEY,
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        items: state.items,
        ownerUserId: state.ownerUserId,
        syncMode: state.syncMode,
      }),
    }
  )
);

// Listener nhận tín hiệu đồng bộ giữa các tab trên cùng trình duyệt
if (cartBroadcastChannel) {
  cartBroadcastChannel.onmessage = (event) => {
    const { type, userId, items } = event.data || {};
    if (type === 'CART_UPDATED') {
      const currentAuth = getAuthContext();
      if (String(currentAuth.userId) === String(userId)) {
        useCartStore.setState({
          items: items || [],
          ownerUserId: userId,
          syncMode: 'server',
        });
      }
    }
  };
}

// Tự động kiểm tra và làm mới giỏ hàng khi người dùng chuyển tab quay lại ứng dụng
if (typeof window !== 'undefined') {
  window.addEventListener('focus', () => {
    const auth = getAuthContext();
    if (auth.isAuthenticated && auth.userId) {
      useCartStore.getState().fetchCart(auth.userId);
    }
  });
}

// Tự động dọn sạch giỏ hàng đang hiển thị mỗi khi tài khoản đăng xuất hoặc hết phiên
registerOnLogoutListener((prevUserId) => {
  useCartStore.getState().handleLogoutClear(prevUserId);
});
