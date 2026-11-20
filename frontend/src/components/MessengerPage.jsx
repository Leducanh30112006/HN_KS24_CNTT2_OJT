import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, Send, ShoppingBag, Store, User, Check, Sparkles, 
  Clock, CheckCircle2, Eye, ShoppingCart, Zap, MessageSquare, 
  ArrowLeft, Info, ShieldCheck, ChevronRight, X, Image as ImageIcon, 
  Smile, ThumbsUp, Star, Phone, Video, MoreVertical, ExternalLink,
  Flame, CheckCircle, RotateCcw
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { 
  getStoredThreads, 
  getOrCreateThread, 
  sendChatMessage, 
  markChatThreadAsRead,
  saveStoredThreads
} from '../stores/useBuyerSellerChatStore';
import { defaultProducts } from '../data/sampleProducts';
import ProductChatCard from './ProductChatCard';
import { fixMojibakeText } from '../utils/vietnameseEncoding';

export default function MessengerPage({
  initialShopId = 1,
  initialProduct = null,
  onBackToHome,
  onSelectProduct,
  onSelectShop,
  onBuyNow,
}) {
  const { user } = useAuthStore();
  const addToCart = useCartStore((state) => state.addToCart);

  const [threads, setThreads] = useState([]);
  const [activeThreadId, setActiveThreadId] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterTab, setFilterTab] = useState('ALL'); // 'ALL' | 'UNREAD' | 'MALL'
  const [inputText, setInputText] = useState('');
  const [showProductPicker, setShowProductPicker] = useState(false);
  const [showRightInfo, setShowRightInfo] = useState(true);
  const [pinnedProduct, setPinnedProduct] = useState(initialProduct);
  const [toastMessage, setToastMessage] = useState('');

  // Đóng popover chọn sản phẩm khi bấm phím Escape
  useEffect(() => {
    if (!showProductPicker) return;
    const handleEscape = (e) => {
      if (e.key === 'Escape') setShowProductPicker(false);
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [showProductPicker]);
  
  // Mobile responsive view: 'list' (danh sách đoạn chat) | 'chat' (khung chat chính)
  const [mobileView, setMobileView] = useState('list');

  const messagesEndRef = useRef(null);
  const chatInputRef = useRef(null);

  const userId = user?.userId || 2;
  const userName = user?.fullName || user?.username || 'Khách Hàng QuickShop';

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  // Load threads và khởi tạo thread mục tiêu nếu có
  const loadThreads = () => {
    let all = getStoredThreads();
    
    // Nếu có initialShopId, đảm bảo thread đó tồn tại
    if (initialShopId) {
      const targetShop = defaultProducts.find((p) => p.shopId === initialShopId) || {
        shopId: initialShopId,
        shopName: initialShopId === 2 ? 'Thời Trang Coolmate Official' : initialShopId === 3 ? 'Gia Dụng Sunhouse Mall' : 'TechWorld Official Store',
      };
      getOrCreateThread(userId, initialShopId, {
        userName,
        shopName: targetShop.shopName,
      });
      all = getStoredThreads();
    }

    setThreads(all);

    // Xác định activeThreadId
    if (initialShopId) {
      const targetId = `buyer_${userId}_shop_${initialShopId}`;
      const found = all.find((t) => t.threadId === targetId);
      if (found) {
        setActiveThreadId(found.threadId);
        markChatThreadAsRead(found.threadId, 'BUYER');
        setMobileView('chat');
        return;
      }
    }

    if (!activeThreadId && all.length > 0) {
      setActiveThreadId(all[0].threadId);
      markChatThreadAsRead(all[0].threadId, 'BUYER');
    }
  };

  useEffect(() => {
    loadThreads();
  }, [initialShopId, userId]);

  // Lắng nghe sự kiện đồng bộ chéo giữa các tab / component
  useEffect(() => {
    const handleSync = () => {
      const all = getStoredThreads();
      setThreads(all);
      if (activeThreadId) {
        markChatThreadAsRead(activeThreadId, 'BUYER');
      }
    };
    window.addEventListener('quickshop_chat_update', handleSync);
    return () => window.removeEventListener('quickshop_chat_update', handleSync);
  }, [activeThreadId]);

  // Cuộn xuống tin nhắn mới nhất
  const scrollToBottom = (behavior = 'smooth') => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior });
    }
  };

  const activeThread = threads.find((t) => t.threadId === activeThreadId) || threads[0];

  useEffect(() => {
    if (activeThreadId) {
      markChatThreadAsRead(activeThreadId, 'BUYER');
      scrollToBottom('auto');
    }
  }, [activeThreadId]);

  useEffect(() => {
    scrollToBottom('smooth');
  }, [activeThread?.messages]);

  // Gửi tin nhắn
  const handleSendMessage = (textToSend = inputText, productToSend = null) => {
    const trimmed = textToSend.trim();
    if (!trimmed && !productToSend) return;
    if (!activeThread) return;

    let extractedProduct = productToSend;
    if (!extractedProduct && pinnedProduct) {
      // Nếu có sản phẩm đang xem/hỏi mua, tự động đính kèm thẻ sản phẩm
      extractedProduct = {
        id: pinnedProduct.id,
        name: pinnedProduct.name,
        price: pinnedProduct.price,
        originalPrice: pinnedProduct.originalPrice,
        imageUrl: pinnedProduct.mainImage || pinnedProduct.imageUrl,
        category: pinnedProduct.categoryName,
        rating: pinnedProduct.rating,
        shopId: pinnedProduct.shopId || activeThread.shopId,
        shopName: pinnedProduct.shopName || activeThread.shopName,
      };
    } else if (!extractedProduct && trimmed) {
      const match = trimmed.match(/\[PRODUCT:(\d+)\]/i) || trimmed.match(/SP[-_]?(\d+)/i) || trimmed.match(/#(\d+)/);
      if (match && match[1]) {
        const pId = Number.parseInt(match[1], 10);
        const found = defaultProducts.find((p) => p.id === pId);
        if (found) {
          extractedProduct = {
            id: found.id,
            name: found.name,
            price: found.price,
            originalPrice: found.originalPrice,
            imageUrl: found.mainImage || found.imageUrl,
            category: found.categoryName,
            rating: found.rating,
            shopId: found.shopId || activeThread.shopId,
            shopName: found.shopName || activeThread.shopName,
          };
        }
      } else {
        // Tự động tìm kiếm sản phẩm của shop nếu câu chat nhắc tới tên sản phẩm
        const lkw = trimmed.toLowerCase();
        const found = defaultProducts.find((p) =>
          (p.shopId === activeThread.shopId || p.shopId === 1) &&
          (lkw.includes(p.name.toLowerCase()) || p.name.toLowerCase().includes(lkw))
        );
        if (found) {
          extractedProduct = {
            id: found.id,
            name: found.name,
            price: found.price,
            originalPrice: found.originalPrice,
            imageUrl: found.mainImage || found.imageUrl,
            category: found.categoryName,
            rating: found.rating,
            shopId: found.shopId || activeThread.shopId,
            shopName: found.shopName || activeThread.shopName,
          };
        }
      }
    }

    sendChatMessage({
      threadId: activeThread.threadId,
      senderRole: 'BUYER',
      senderName: userName,
      text: trimmed,
      productData: extractedProduct,
    });

    setInputText('');
    setShowProductPicker(false);
    if (pinnedProduct && productToSend) {
      setPinnedProduct(null);
    }
  };

  // Gửi sản phẩm đang xem
  const handleSendPinnedProduct = () => {
    if (!pinnedProduct || !activeThread) return;
    const prodData = {
      id: pinnedProduct.id,
      name: pinnedProduct.name,
      price: pinnedProduct.price,
      originalPrice: pinnedProduct.originalPrice,
      imageUrl: pinnedProduct.mainImage || pinnedProduct.imageUrl,
      category: pinnedProduct.categoryName,
      rating: pinnedProduct.rating,
      shopId: pinnedProduct.shopId || activeThread.shopId,
      shopName: pinnedProduct.shopName || activeThread.shopName,
    };
    handleSendMessage('Shop ơi, mình đang quan tâm sản phẩm này, tư vấn thêm giúp mình nhé!', prodData);
  };

  // Chọn sản phẩm từ picker để gửi
  const handleSelectProductFromPicker = (p) => {
    const prodData = {
      id: p.id,
      name: p.name,
      price: p.price,
      originalPrice: p.originalPrice,
      imageUrl: p.mainImage || p.imageUrl,
      category: p.categoryName,
      rating: p.rating,
      shopId: p.shopId || activeThread.shopId,
      shopName: p.shopName || activeThread.shopName,
    };
    handleSendMessage(`Mình muốn hỏi về sản phẩm "${p.name}"`, prodData);
  };

  // Lọc danh sách đoạn chat
  const filteredThreads = threads.filter((t) => {
    const matchesSearch = (t.shopName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (t.userName || '').toLowerCase().includes(searchQuery.toLowerCase());
    if (!matchesSearch) return false;
    if (filterTab === 'UNREAD') return t.unreadByBuyer > 0;
    if (filterTab === 'MALL') return true;
    return true;
  });

  // Sản phẩm thuộc shop đang chat
  const shopProducts = defaultProducts.filter(
    (p) => !activeThread || p.shopId === activeThread.shopId || p.shopId === 1
  );

  // Danh sách các thẻ sản phẩm đã trao đổi trong đoạn chat hiện tại
  const sharedProducts = (activeThread?.messages || [])
    .filter((m) => m.productData)
    .map((m) => m.productData);

  const quickQuestions = [
    'Sản phẩm này còn sẵn hàng không shop?',
    'Shop có giao hỏa tốc 2h tại Hà Nội không?',
    'Sản phẩm được bảo hành chính hãng bao lâu?',
    'Shop có voucher giảm giá nào áp dụng được không?',
  ];

  return (
    <div className="w-full h-full flex flex-col bg-slate-50 overflow-hidden">
      {/* Toast thông báo hành động */}
      {toastMessage && (
        <div className="fixed top-20 left-1/2 -translate-x-1/2 z-50 bg-emerald-600 text-white text-xs font-bold px-4 py-2.5 rounded-full shadow-xl flex items-center gap-2 animate-pulse">
          <CheckCircle2 className="w-4 h-4" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Main Messenger Container */}
      <div className="flex-1 bg-white border-t border-slate-200/90 shadow-none overflow-hidden flex flex-row relative h-full">
        
        {/* ========================================================================= */}
        {/* 1. CỘT TRÁI: DANH SÁCH ĐOẠN CHAT (CHATS SIDEBAR) */}
        {/* ========================================================================= */}
        <div
          className={`w-full md:w-80 lg:w-96 border-r border-slate-200/80 flex flex-col bg-slate-50/50 ${
            mobileView === 'chat' ? 'hidden md:flex' : 'flex'
          }`}
        >
          {/* Header Sidebar */}
          <div className="p-4 border-b border-slate-200/80 bg-white space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                {onBackToHome && (
                  <button
                    onClick={onBackToHome}
                    className="p-1.5 -ml-1 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-xl transition md:hidden"
                    title="Về trang chủ"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>
                )}
                <h1 className="text-lg font-black text-slate-900 tracking-tight flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-orange-600" />
                  <span>Đoạn chat</span>
                </h1>
              </div>

              <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-orange-100 text-orange-700">
                {threads.length} hội thoại
              </span>
            </div>

            {/* Ô tìm kiếm đoạn chat */}
            <div className="relative">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm kiếm trên Messenger..."
                className="w-full pl-9 pr-3 py-2 bg-slate-100 border border-transparent focus:border-orange-500 focus:bg-white rounded-2xl text-xs text-slate-900 outline-none transition"
              />
            </div>

            {/* Filter Pills */}
            <div className="flex items-center gap-1.5 pt-1">
              {[
                { id: 'ALL', label: 'Tất cả' },
                { id: 'UNREAD', label: 'Chưa đọc' },
                { id: 'MALL', label: 'Shop Mall' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setFilterTab(tab.id)}
                  className={`px-3 py-1 rounded-full text-[11px] font-bold transition cursor-pointer ${
                    filterTab === tab.id
                      ? 'bg-orange-600 text-white shadow-xs'
                      : 'bg-slate-100 hover:bg-slate-200/80 text-slate-600'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          {/* Danh sách các cuộc trò chuyện */}
          <div className="flex-1 overflow-y-auto divide-y divide-slate-100 no-scrollbar" data-lenis-prevent="true">
            {filteredThreads.length === 0 ? (
              <div className="p-8 text-center text-slate-400 text-xs space-y-2">
                <MessageSquare className="w-8 h-8 mx-auto text-slate-300" />
                <p>Không tìm thấy đoạn chat nào</p>
              </div>
            ) : (
              filteredThreads.map((thread) => {
                const isActive = thread.threadId === activeThread?.threadId;
                const lastMsg = thread.messages?.[thread.messages.length - 1];

                return (
                  <button
                    type="button"
                    key={thread.threadId}
                    onClick={() => {
                      setActiveThreadId(thread.threadId);
                      setMobileView('chat');
                    }}
                    className={`w-full text-left border-0 p-3.5 flex items-start gap-3 cursor-pointer transition ${
                      isActive
                        ? 'bg-orange-50/70 border-l-2 border-orange-600'
                        : 'bg-transparent hover:bg-slate-100/70'
                    }`}
                  >
                    {/* Avatar with online status */}
                    <div className="relative flex-shrink-0">
                      <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center font-bold text-sm shadow-sm overflow-hidden border border-slate-200">
                        {thread.shopAvatar ? (
                          <img src={thread.shopAvatar} alt="" className="w-full h-full object-cover" />
                        ) : (
                          <Store className="w-6 h-6" />
                        )}
                      </div>
                      <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 border-2 border-white rounded-full" />
                    </div>

                    {/* Thread Info */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1">
                        <div className="flex items-center gap-1.5 min-w-0">
                          <h4 className="font-bold text-xs text-slate-900 truncate">
                            {thread.shopName || 'Shop Đối Tác'}
                          </h4>
                          <span className="bg-rose-100 text-rose-700 text-[9px] font-black px-1 rounded uppercase flex-shrink-0">
                            Mall
                          </span>
                        </div>
                        <span className="text-[10px] text-slate-400 font-mono flex-shrink-0">
                          {new Date(thread.updatedAt || Date.now()).toLocaleTimeString('vi-VN', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </span>
                      </div>

                      <p className={`text-[11px] truncate mt-1 ${thread.unreadByBuyer > 0 ? 'font-bold text-slate-900' : 'text-slate-500'}`}>
                        {lastMsg?.senderRole === 'BUYER' ? 'Bạn: ' : ''}
                        {lastMsg?.productData ? `[Thẻ sản phẩm: ${lastMsg.productData.name}]` : (lastMsg?.text || 'Bắt đầu cuộc trò chuyện')}
                      </p>
                    </div>

                    {/* Unread badge */}
                    {thread.unreadByBuyer > 0 && (
                      <span className="w-5 h-5 rounded-full bg-orange-600 text-white text-[10px] font-black flex items-center justify-center flex-shrink-0 self-center">
                        {thread.unreadByBuyer}
                      </span>
                    )}
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* ========================================================================= */}
        {/* 2. CỘT GIỮA: KHUNG CHAT CHÍNH (ACTIVE CHAT CONVERSATION) */}
        {/* ========================================================================= */}
        {activeThread ? (
          <div
            className={`flex-1 flex flex-col bg-white min-w-0 ${
              mobileView === 'list' ? 'hidden md:flex' : 'flex'
            }`}
          >
            {/* Header phòng chat */}
            <div className="h-16 px-4 border-b border-slate-200/80 flex items-center justify-between bg-white z-10">
              <div className="flex items-center gap-3 min-w-0">
                {/* Nút quay lại trên mobile */}
                <button
                  onClick={() => setMobileView('list')}
                  className="p-1.5 -ml-1 text-slate-600 hover:bg-slate-100 rounded-xl md:hidden cursor-pointer"
                  title="Danh sách đoạn chat"
                >
                  <ArrowLeft className="w-5 h-5" />
                </button>

                <div className="relative flex-shrink-0">
                  <div className="w-10 h-10 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-sm border border-orange-200 overflow-hidden">
                    {activeThread.shopAvatar ? (
                      <img src={activeThread.shopAvatar} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <Store className="w-5 h-5" />
                    )}
                  </div>
                  <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 border-2 border-white rounded-full" />
                </div>

                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h3 className="font-bold text-sm text-slate-900 truncate">
                      {activeThread.shopName || 'TechWorld Official Store'}
                    </h3>
                    <CheckCircle className="w-3.5 h-3.5 text-blue-500 fill-blue-500 flex-shrink-0" />
                    <span className="bg-rose-100 text-rose-700 text-[9px] font-black px-1 py-0.2 rounded uppercase">
                      Mall
                    </span>
                  </div>
                  <p className="text-[11px] text-emerald-600 flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                    <span>Đang hoạt động • Phản hồi trong 5 phút</span>
                  </p>
                </div>
              </div>

              {/* Action buttons header */}
              <div className="flex items-center gap-1 sm:gap-2">
                {onSelectShop && (
                  <button
                    onClick={() => onSelectShop({ id: activeThread.shopId, name: activeThread.shopName })}
                    className="hidden sm:inline-flex items-center gap-1 px-3 py-1.5 bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 rounded-xl text-xs font-bold transition cursor-pointer"
                    title="Ghé thăm gian hàng"
                  >
                    <Store className="w-3.5 h-3.5" />
                    <span>Xem Shop</span>
                  </button>
                )}

                <button
                  onClick={() => setShowProductPicker(!showProductPicker)}
                  className="px-3 py-1.5 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200 rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer active:scale-95 shadow-2xs"
                  title="Chọn sản phẩm để gửi vào chat"
                >
                  <ShoppingBag className="w-3.5 h-3.5 text-orange-600" />
                  <span className="hidden sm:inline">Gửi Sản Phẩm</span>
                </button>

                <button
                  type="button"
                  onClick={() => setShowRightInfo(!showRightInfo)}
                  className={`px-3 py-1.5 rounded-xl transition cursor-pointer text-xs font-bold flex items-center gap-1.5 active:scale-95 border ${
                    showRightInfo
                      ? 'bg-orange-50 text-orange-700 border-orange-200 shadow-2xs'
                      : 'bg-slate-100 text-slate-700 border-slate-200 hover:bg-slate-200/80'
                  }`}
                  title={showRightInfo ? 'Thu gọn thông tin Shop' : 'Mở xem thông tin Shop'}
                >
                  <Info className="w-3.5 h-3.5 text-orange-600" />
                  <span className="hidden sm:inline">{showRightInfo ? 'Thu gọn' : 'Chi tiết Shop'}</span>
                </button>
              </div>
            </div>

            {/* Pinned Product Banner (Sản phẩm đang hỏi mua) */}
            {pinnedProduct && (
              <div className="bg-gradient-to-r from-amber-50 to-orange-50 border-b border-amber-200/80 px-4 py-2.5 flex items-center justify-between gap-3 shadow-2xs">
                <div className="flex items-center gap-3 min-w-0">
                  <img
                    src={pinnedProduct.mainImage || pinnedProduct.imageUrl}
                    alt={pinnedProduct.name}
                    className="w-10 h-10 rounded-xl object-cover border border-amber-200 flex-shrink-0"
                  />
                  <div className="min-w-0">
                    <p className="text-[10px] uppercase font-bold text-orange-700 tracking-wider">Sản phẩm bạn đang xem:</p>
                    <p className="text-xs font-bold text-slate-900 truncate">{pinnedProduct.name}</p>
                    <p className="text-xs font-black text-orange-600 font-mono">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pinnedProduct.price)}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 flex-shrink-0">
                  <button
                    onClick={handleSendPinnedProduct}
                    className="px-3 py-1.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-sm transition active:scale-95 flex items-center gap-1 cursor-pointer"
                  >
                    <Send className="w-3 h-3" />
                    <span>Gửi vào chat</span>
                  </button>
                  <button
                    onClick={() => setPinnedProduct(null)}
                    className="p-1 text-slate-400 hover:text-slate-600 cursor-pointer"
                    title="Ẩn ghim sản phẩm"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}

            {/* Vùng hiển thị tin nhắn (Message Stream) */}
            <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-4 bg-slate-50/50" data-lenis-prevent="true">
              <div className="text-center my-3">
                <span className="px-3.5 py-1 bg-white border border-slate-200/80 rounded-full text-[10px] text-slate-500 font-medium shadow-2xs">
                  Bắt đầu cuộc trò chuyện với {activeThread.shopName} • Được bảo vệ bởi QuickShop
                </span>
              </div>

              {activeThread.messages?.map((msg) => {
                const isMe = msg.senderRole === 'BUYER';

                return (
                  <div
                    key={msg.id}
                    className={`flex flex-col ${isMe ? 'items-end' : 'items-start'} space-y-1`}
                  >
                    <div className="flex items-end gap-2 max-w-[85%] sm:max-w-[75%]">
                      {!isMe && (
                        <div className="w-8 h-8 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-xs flex-shrink-0 mb-1 border border-orange-200 overflow-hidden">
                          {activeThread.shopAvatar ? (
                            <img src={activeThread.shopAvatar} alt="" className="w-full h-full object-cover" />
                          ) : (
                            <Store className="w-4 h-4" />
                          )}
                        </div>
                      )}

                      <div
                        className={`p-3.5 rounded-2xl text-xs leading-relaxed shadow-2xs ${
                          isMe
                            ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-br-xs font-medium'
                            : 'bg-white text-slate-800 border border-slate-200/90 rounded-bl-xs'
                        }`}
                      >
                        {msg.text && <p className="whitespace-pre-wrap">{msg.text}</p>}

                        {/* Thẻ sản phẩm tương tác */}
                        {msg.productData && (
                          <ProductChatCard
                            product={msg.productData}
                            onSelectProduct={onSelectProduct}
                            onBuyNow={onBuyNow}
                            onToast={showToast}
                          />
                        )}
                      </div>
                    </div>

                    <span className="text-[10px] text-slate-400 px-2 font-mono">
                      {new Date(msg.createdAt || Date.now()).toLocaleTimeString('vi-VN', {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>
                );
              })}
              <div ref={messagesEndRef} />
            </div>

            {/* Popover / Drawer chọn sản phẩm của Shop để gửi */}
            {showProductPicker && (
              <div className="p-3.5 bg-white border-t border-slate-200 max-h-56 overflow-y-auto space-y-2 shadow-inner" data-lenis-prevent="true">
                <div className="flex items-center justify-between text-xs font-bold text-slate-700 px-1">
                  <span className="flex items-center gap-1.5 text-orange-600">
                    <ShoppingBag className="w-3.5 h-3.5" />
                    <span>Chọn sản phẩm để gửi vào đoạn chat:</span>
                  </span>
                  <button
                    onClick={() => setShowProductPicker(false)}
                    className="text-slate-400 hover:text-slate-600 text-xs cursor-pointer"
                  >
                    ✕ Đóng
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                  {shopProducts.slice(0, 9).map((prod) => (
                    <button
                      type="button"
                      key={prod.id}
                      onClick={() => handleSelectProductFromPicker(prod)}
                      className="flex items-center gap-2.5 p-2 rounded-xl border border-slate-200 hover:border-orange-400 hover:bg-orange-50/50 cursor-pointer transition text-left group w-full bg-white"
                    >
                      <img
                        src={prod.mainImage || prod.imageUrl}
                        alt={prod.name}
                        className="w-11 h-11 rounded-lg object-cover flex-shrink-0 border border-slate-100"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-[11px] font-bold text-slate-800 truncate group-hover:text-orange-600">
                          {prod.name}
                        </p>
                        <p className="text-[11px] font-black text-orange-600 font-mono">
                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(prod.price)}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Quick Suggestions Chips */}
            <div className="px-3 py-2 bg-slate-50 border-t border-slate-200/80 flex items-center gap-1.5 overflow-x-auto no-scrollbar">
              <button
                onClick={() => setShowProductPicker(!showProductPicker)}
                className="px-2.5 py-1 bg-white hover:bg-orange-50 border border-slate-200 hover:border-orange-300 text-slate-700 hover:text-orange-600 rounded-full text-[11px] font-bold flex items-center gap-1 transition flex-shrink-0 active:scale-95 cursor-pointer shadow-2xs"
              >
                <ShoppingBag className="w-3 h-3 text-orange-600" />
                <span>Gửi sản phẩm</span>
              </button>
              {quickQuestions.map((q, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => handleSendMessage(q, pinnedProduct)}
                  className="px-2.5 py-1 bg-white hover:bg-slate-100 border border-slate-200 text-slate-600 rounded-full text-[11px] transition flex-shrink-0 active:scale-95 cursor-pointer"
                >
                  {q}
                </button>
              ))}
            </div>

            {/* Input Bar (Thanh nhập tin nhắn) */}
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="p-3 sm:p-4 bg-white border-t border-slate-200 flex items-center gap-2"
            >
              <input
                ref={chatInputRef}
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder="Nhập tin nhắn hoặc mã sản phẩm (SP-1, #1)..."
                className="flex-1 px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-2xl text-xs text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition"
              />
              <button
                type="submit"
                disabled={!inputText.trim()}
                className="w-10 h-10 rounded-2xl bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 disabled:opacity-40 text-white flex items-center justify-center transition shadow-md active:scale-95 flex-shrink-0 cursor-pointer"
                title="Gửi tin nhắn"
              >
                <Send className="w-4 h-4" />
              </button>
            </form>
          </div>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center p-8 text-center text-slate-400 space-y-3">
            <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center">
              <MessageSquare className="w-8 h-8 text-slate-400" />
            </div>
            <h3 className="font-bold text-slate-700 text-sm">Chưa chọn cuộc trò chuyện nào</h3>
            <p className="text-xs text-slate-400 max-w-sm">
              Chọn một gian hàng từ danh sách bên trái hoặc bấm "Chat Ngay" từ bất kỳ sản phẩm nào để bắt đầu trò chuyện.
            </p>
          </div>
        )}

        {/* ========================================================================= */}
        {/* 3. CỘT PHẢI: THÔNG TIN GIAN HÀNG & SẢN PHẨM (CONTEXT INFO DRAWER) */}
        {/* ========================================================================= */}
        {showRightInfo && activeThread && (
          <div className="w-80 border-l border-slate-200/80 bg-slate-50/40 p-5 hidden lg:flex flex-col space-y-5 overflow-y-auto no-scrollbar" data-lenis-prevent="true">
            {/* Header Drawer có nút Thu Gọn */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-200/80">
              <span className="text-xs font-black text-slate-800 uppercase tracking-wider flex items-center gap-1.5">
                <Store className="w-4 h-4 text-orange-600" />
                <span>Thông tin Shop</span>
              </span>
              <button
                type="button"
                onClick={() => setShowRightInfo(false)}
                className="p-1 hover:bg-slate-200/80 rounded-xl text-slate-400 hover:text-slate-700 transition cursor-pointer"
                title="Thu gọn bảng thông tin"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Shop Profile Box */}
            <div className="text-center space-y-2 pb-4 border-b border-slate-200/80">
              <div className="w-20 h-20 mx-auto rounded-3xl bg-gradient-to-tr from-orange-500 to-amber-500 text-white flex items-center justify-center font-bold text-xl shadow-md overflow-hidden border-2 border-white">
                {activeThread.shopAvatar ? (
                  <img src={activeThread.shopAvatar} alt="" className="w-full h-full object-cover" />
                ) : (
                  <Store className="w-8 h-8" />
                )}
              </div>
              <div>
                <h3 className="font-bold text-sm text-slate-900 flex items-center justify-center gap-1">
                  <span>{activeThread.shopName}</span>
                  <CheckCircle className="w-3.5 h-3.5 text-blue-500 fill-blue-500" />
                </h3>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  Gian hàng chính hãng QuickShop Mall
                </p>
              </div>

              {onSelectShop && (
                <button
                  onClick={() => onSelectShop({ id: activeThread.shopId, name: activeThread.shopName })}
                  className="w-full py-2 bg-white hover:bg-orange-50 text-slate-700 hover:text-orange-600 border border-slate-200 rounded-xl text-xs font-bold transition active:scale-95 flex items-center justify-center gap-1.5 shadow-2xs cursor-pointer"
                >
                  <Store className="w-3.5 h-3.5 text-orange-600" />
                  <span>Xem Hồ Sơ Shop</span>
                </button>
              )}
            </div>

            {/* Shop Metrics */}
            <div className="grid grid-cols-2 gap-2 text-center text-xs">
              <div className="p-3 bg-white rounded-2xl border border-slate-200/80 space-y-0.5">
                <div className="flex items-center justify-center gap-1 font-black text-amber-500">
                  <Star className="w-3.5 h-3.5 fill-amber-400" />
                  <span>4.9 / 5.0</span>
                </div>
                <div className="text-[10px] text-slate-400 font-medium">Đánh giá Shop</div>
              </div>
              <div className="p-3 bg-white rounded-2xl border border-slate-200/80 space-y-0.5">
                <div className="font-black text-emerald-600">99%</div>
                <div className="text-[10px] text-slate-400 font-medium">Tỷ lệ phản hồi</div>
              </div>
            </div>

            {/* Chính sách bảo đảm QuickShop */}
            <div className="space-y-2">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-600" />
                <span>Cam Kết Từ QuickShop</span>
              </h4>
              <div className="p-3.5 bg-white rounded-2xl border border-slate-200/80 space-y-2 text-xs text-slate-600">
                <div className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                  <span>100% Sản phẩm chính hãng</span>
                </div>
                <div className="flex items-center gap-2">
                  <RotateCcw className="w-3.5 h-3.5 text-blue-600 flex-shrink-0" />
                  <span>Đổi trả miễn phí trong 15 ngày</span>
                </div>
                <div className="flex items-center gap-2">
                  <Zap className="w-3.5 h-3.5 text-amber-600 flex-shrink-0" />
                  <span>Bảo hộ thanh toán qua Escrow</span>
                </div>
              </div>
            </div>

            {/* Thẻ sản phẩm đã trao đổi trong đoạn chat */}
            {sharedProducts.length > 0 && (
              <div className="space-y-2">
                <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-orange-600" />
                  <span>Sản Phẩm Đã Trao Đổi</span>
                </h4>
                <div className="space-y-2">
                  {sharedProducts.slice(0, 3).map((sp, idx) => (
                    <button
                      type="button"
                      key={idx}
                      onClick={() => onSelectProduct && onSelectProduct(sp.id)}
                      className="w-full text-left p-2.5 bg-white rounded-2xl border border-slate-200/80 hover:border-orange-300 cursor-pointer transition flex items-center gap-2.5 group"
                    >
                      <img
                        src={sp.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=100'}
                        alt=""
                        className="w-10 h-10 rounded-xl object-cover border border-slate-100 flex-shrink-0"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-[11px] font-bold text-slate-800 truncate group-hover:text-orange-600">
                          {sp.name}
                        </p>
                        <p className="text-[10px] font-black text-orange-600 font-mono">
                          {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(sp.price)}
                        </p>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
