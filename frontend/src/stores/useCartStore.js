import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export const useCartStore = create(
  persist(
    (set, get) => ({
      items: [], // Array of { productId, variantId, sku, productName, color, size, price, mainImage, quantity, shopId, shopName }

      addToCart: (product, variant, quantity = 1) => {
        const currentItems = get().items;
        const variantId = variant?.id || product.id;
        const sku = variant?.sku || `SKU-${product.id}`;
        const price = variant?.price || product.minPrice || 0;
        const color = variant?.color || '';
        const size = variant?.size || '';
        const shopId = product.shopId || 1;
        const shopName = product.shopName || 'TechWorld Official Store';

        const existingIndex = currentItems.findIndex(
          (item) => item.productId === product.id && item.variantId === variantId
        );

        if (existingIndex > -1) {
          const updated = [...currentItems];
          updated[existingIndex].quantity += quantity;
          set({ items: updated });
        } else {
          set({
            items: [
              ...currentItems,
              {
                productId: product.id,
                variantId,
                sku,
                productName: product.name,
                color,
                size,
                price: Number(price),
                mainImage: product.mainImage,
                quantity,
                shopId,
                shopName,
              },
            ],
          });
        }
      },

      updateQuantity: (variantId, newQuantity) => {
        if (newQuantity <= 0) {
          get().removeFromCart(variantId);
          return;
        }
        const updated = get().items.map((item) =>
          item.variantId === variantId ? { ...item, quantity: newQuantity } : item
        );
        set({ items: updated });
      },

      removeFromCart: (variantId) => {
        set({ items: get().items.filter((item) => item.variantId !== variantId) });
      },

      clearCart: () => {
        set({ items: [] });
      },

      getTotalCount: () => {
        return get().items.reduce((sum, item) => sum + item.quantity, 0);
      },

      getTotalAmount: () => {
        return get().items.reduce((sum, item) => sum + item.price * item.quantity, 0);
      },

      // Gom nhóm sản phẩm theo từng Shop (chuẩn Multi-Vendor Marketplace)
      getItemsByShop: () => {
        const groups = {};
        for (const item of get().items) {
          const sId = item.shopId || 1;
          if (!groups[sId]) {
            groups[sId] = {
              shopId: sId,
              shopName: item.shopName || `Gian hàng #${sId}`,
              items: [],
            };
          }
          groups[sId].items.push(item);
        }
        return Object.values(groups);
      },
    }),
    {
      name: 'quickshop_cart',
      storage: createJSONStorage(() => localStorage),
    }
  )
);
