/**
 * useBuyerSellerChatStore.js
 * Hệ thống quản lý tin nhắn 2 chiều thời gian thực giữa Khách hàng (Buyer) và Người bán (Seller / Shop).
 * Hỗ trợ:
 * - Gửi & nhận tin nhắn tức thì giữa 2 bên
 * - Gửi kèm thẻ thông tin sản phẩm (Product Card Preview)
 * - Tương tác trực tiếp trên Product Card: "Thêm vào giỏ", "Xem chi tiết", "Mua ngay"
 * - Đồng bộ chéo thời gian thực qua CustomEvent & LocalStorage
 */

const STORAGE_KEY = 'quickshop_buyer_seller_chat_v2';

const INITIAL_THREADS = [
  {
    threadId: 'buyer_2_shop_1',
    userId: 2,
    userName: 'Lê Đức Anh',
    userAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
    shopId: 1,
    shopName: 'TechWorld Official Store',
    shopAvatar: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
    unreadByBuyer: 0,
    unreadBySeller: 0,
    updatedAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    messages: [
      {
        id: 'msg_1',
        senderRole: 'SELLER',
        senderName: 'TechWorld Official Store',
        text: 'Dạ chào bạn! Cảm ơn bạn đã ghé thăm TechWorld Official Store. Shop luôn sẵn sàng hỗ trợ tư vấn cấu hình và ưu đãi tốt nhất cho bạn ạ!',
        createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
      },
      {
        id: 'msg_2',
        senderRole: 'BUYER',
        senderName: 'Lê Đức Anh',
        text: 'Shop ơi, mẫu iPhone 15 Pro Max 256GB Titan Tự Nhiên còn sẵn hàng tại kho Hà Nội không shop?',
        createdAt: new Date(Date.now() - 1000 * 60 * 20).toISOString(),
      },
      {
        id: 'msg_3',
        senderRole: 'SELLER',
        senderName: 'TechWorld Official Store',
        text: 'Dạ kho Hà Nội còn sẵn hàng nguyên seal, bảo hành 12 tháng chính hãng VN/A bạn nhé. Shop gửi bạn thông tin chi tiết sản phẩm kèm ưu đãi giảm ngay 100k hôm nay:',
        productData: {
          id: 1,
          name: 'Điện Thoại Apple iPhone 15 Pro Max 256GB Titan Tự Nhiên VN/A',
          price: 29990000,
          originalPrice: 34990000,
          imageUrl: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=300&auto=format&fit=crop&q=80',
          category: 'Điện thoại & Phụ kiện',
          rating: 4.9,
          sold: 1420,
          stock: 45,
          shopId: 1,
          shopName: 'TechWorld Official Store'
        },
        createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
      }
    ]
  },
  {
    threadId: 'buyer_2_shop_2',
    userId: 2,
    userName: 'Lê Đức Anh',
    userAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
    shopId: 2,
    shopName: 'Thời Trang Coolmate Official',
    shopAvatar: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=100&auto=format&fit=crop&q=80',
    unreadByBuyer: 0,
    unreadBySeller: 0,
    updatedAt: new Date(Date.now() - 1000 * 60 * 95).toISOString(),
    messages: [
      {
        id: 'msg_c1',
        senderRole: 'SELLER',
        senderName: 'Thời Trang Coolmate Official',
        text: 'Chào bạn! Coolmate hỗ trợ đổi trả miễn phí trong 60 ngày với bất kỳ lý do gì. Bạn cần tư vấn size áo quần nào ạ?',
        createdAt: new Date(Date.now() - 1000 * 60 * 95).toISOString(),
      }
    ]
  }
];

export const getStoredThreads = () => {
  if (typeof window === 'undefined') return INITIAL_THREADS;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_THREADS));
      return INITIAL_THREADS;
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) && parsed.length > 0 ? parsed : INITIAL_THREADS;
  } catch (e) {
    console.warn('Lỗi đọc chat threads từ localStorage:', e);
    return INITIAL_THREADS;
  }
};

export const saveStoredThreads = (threads) => {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(threads));
    window.dispatchEvent(new CustomEvent('quickshop_chat_update', { detail: { timestamp: Date.now() } }));
  } catch (e) {
    console.warn('Lỗi lưu chat threads:', e);
  }
};

/**
 * Lấy hoặc khởi tạo thread giữa buyer và shop
 */
export const getOrCreateThread = (userId, shopId, customMeta = {}) => {
  const threads = getStoredThreads();
  const threadId = `buyer_${userId || 2}_shop_${shopId || 1}`;
  let thread = threads.find((t) => t.threadId === threadId);

  if (!thread) {
    thread = {
      threadId,
      userId: userId || 2,
      userName: customMeta.userName || 'Khách Hàng',
      userAvatar: customMeta.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
      shopId: shopId || 1,
      shopName: customMeta.shopName || 'TechWorld Official Store',
      shopAvatar: customMeta.shopAvatar || 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=100&auto=format&fit=crop&q=80',
      unreadByBuyer: 0,
      unreadBySeller: 0,
      updatedAt: new Date().toISOString(),
      messages: [
        {
          id: `msg_welcome_${Date.now()}`,
          senderRole: 'SELLER',
          senderName: customMeta.shopName || 'TechWorld Official Store',
          text: `Dạ chào bạn! Cảm ơn bạn đã liên hệ gian hàng ${customMeta.shopName || 'của chúng tôi'}. Shop có thể giúp gì cho bạn hôm nay ạ?`,
          createdAt: new Date().toISOString(),
        }
      ]
    };
    threads.unshift(thread);
    saveStoredThreads(threads);
  }

  return thread;
};

/**
 * Gửi tin nhắn mới (có thể kèm dữ liệu sản phẩm productData)
 */
export const sendChatMessage = ({
  threadId,
  senderRole, // 'BUYER' | 'SELLER'
  senderName,
  text,
  productData = null
}) => {
  const threads = getStoredThreads();
  const index = threads.findIndex((t) => t.threadId === threadId);
  if (index === -1) return null;

  const now = new Date().toISOString();
  const newMessage = {
    id: `msg_${Date.now()}_${Math.floor(Math.random() * 1000)}`,
    senderRole,
    senderName,
    text: text || '',
    productData: productData ? { ...productData } : null,
    createdAt: now
  };

  const targetThread = threads[index];
  const updatedMessages = [...(targetThread.messages || []), newMessage];

  const updatedThread = {
    ...targetThread,
    messages: updatedMessages,
    updatedAt: now,
    unreadByBuyer: senderRole === 'SELLER' ? (targetThread.unreadByBuyer || 0) + 1 : 0,
    unreadBySeller: senderRole === 'BUYER' ? (targetThread.unreadBySeller || 0) + 1 : 0
  };

  threads.splice(index, 1);
  threads.unshift(updatedThread); // Đưa lên đầu danh sách
  saveStoredThreads(threads);

  return { thread: updatedThread, message: newMessage };
};

/**
 * Đánh dấu đã đọc cho role tương ứng
 */
export const markChatThreadAsRead = (threadId, readerRole) => {
  const threads = getStoredThreads();
  let changed = false;
  const updated = threads.map((t) => {
    if (t.threadId === threadId) {
      if (readerRole === 'BUYER' && t.unreadByBuyer > 0) {
        changed = true;
        return { ...t, unreadByBuyer: 0 };
      }
      if (readerRole === 'SELLER' && t.unreadBySeller > 0) {
        changed = true;
        return { ...t, unreadBySeller: 0 };
      }
    }
    return t;
  });

  if (changed) {
    saveStoredThreads(updated);
  }
};
