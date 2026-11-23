import React, { useState, useEffect, useRef } from 'react';
import { 
  X, Send, ShoppingBag, Store, Check, Sparkles, Image as ImageIcon, 
  ChevronRight, AlertCircle, ShoppingCart, CheckCircle2 
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { 
  getStoredThreads, 
  getOrCreateThread, 
  sendChatMessage, 
  markChatThreadAsRead 
} from '../stores/useBuyerSellerChatStore';
import { defaultProducts } from '../data/sampleProducts';
import ProductChatCard from './ProductChatCard';
import { AnimatedPresence } from './motion/MotionPrimitives';

export default function BuyerSellerChatModal({
  isOpen,
  onClose,
  targetShopId = 1,
  targetShopName = 'TechWorld Official Store',
  targetProduct = null,
  onSelectProduct,
  onBuyNow,
  onRequireAuth,
}) {
  const { user } = useAuthStore();
  const [messages, setMessages] = useState([]);
  const [inputText, setInputText] = useState('');
  const [currentThreadId, setCurrentThreadId] = useState('');
  const [showProductPicker, setShowProductPicker] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const messagesEndRef = useRef(null);

  // Đóng popover chọn sản phẩm khi bấm phím Escape (trước khi đóng cả modal)
  useEffect(() => {
    if (!showProductPicker) return;
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        setShowProductPicker(false);
      }
    };
    window.addEventListener('keydown', handleEscape, true);
    return () => window.removeEventListener('keydown', handleEscape, true);
  }, [showProductPicker]);

  const userId = user?.userId ?? (user?.id ?? 'guest');
  const userName = user?.fullName || user?.username || 'Khách Hàng QuickShop';

  // Load hoặc tạo thread khi modal mở
  const syncChatThread = () => {
    const thread = getOrCreateThread(userId, targetShopId, {
      userName,
      shopName: targetShopName,
    });
    if (thread) {
      setCurrentThreadId(thread.threadId);
      setMessages(thread.messages || []);
      markChatThreadAsRead(thread.threadId, 'BUYER');
    }
  };

  useEffect(() => {
    if (isOpen) {
      syncChatThread();
    }
  }, [isOpen, targetShopId, userId]);

  // Lắng nghe sự kiện đồng bộ chéo giữa các component / tab
  useEffect(() => {
    const handleSync = () => {
      if (isOpen && currentThreadId) {
        const threads = getStoredThreads();
        const found = threads.find((t) => t.threadId === currentThreadId && String(t.userId) === String(userId));
        if (found) {
          setMessages(found.messages || []);
          markChatThreadAsRead(currentThreadId, 'BUYER');
        }
      }
    };
    window.addEventListener('quickshop_chat_update', handleSync);
    return () => window.removeEventListener('quickshop_chat_update', handleSync);
  }, [isOpen, currentThreadId, userId]);

  // Tự động cuộn xuống tin nhắn cuối
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  const handleSendMessage = (textToSend = inputText, productToSend = null) => {
    const trimmed = textToSend.trim();
    if (!trimmed && !productToSend) return;

    // Kiểm tra xem tin nhắn có chứa mã sản phẩm dạng [PRODUCT:id] hoặc SP-id hoặc #id không
    let extractedProduct = productToSend;
    if (!extractedProduct && trimmed) {
      const match = trimmed.match(/\[PRODUCT:(\d+)\]/i) || trimmed.match(/SP[-_]?(\d+)/i) || trimmed.match(/#(\d+)/);
      if (match && match[1]) {
        const pId = Number.parseInt(match[1], 10);
        const found = defaultProducts.find((p) => p.id === pId);
        if (found) {
          const pr = Number(found.price ?? found.minPrice ?? found.variants?.[0]?.price ?? 0);
          const origPr = Number(found.originalPrice ?? found.maxPrice ?? (pr > 0 ? Math.round(pr * 1.15) : 0)) || 0;
          extractedProduct = {
            id: found.id,
            name: found.name,
            price: pr,
            minPrice: pr,
            originalPrice: origPr,
            maxPrice: origPr,
            imageUrl: found.mainImage || found.imageUrl,
            category: found.categoryName,
            rating: found.rating,
            shopId: found.shopId || targetShopId,
            shopName: found.shopName || targetShopName,
            variants: found.variants || [],
          };
        }
      }
    }

    const res = sendChatMessage({
      threadId: currentThreadId,
      senderRole: 'BUYER',
      senderName: userName,
      text: trimmed,
      productData: extractedProduct,
    });

    if (res) {
      setMessages(res.thread.messages);
      setInputText('');
      setShowProductPicker(false);
    }
  };

  const handleSendCurrentProduct = () => {
    if (!targetProduct) return;
    const prodData = {
      id: targetProduct.id,
      name: targetProduct.name,
      price: targetProduct.price,
      originalPrice: targetProduct.originalPrice,
      imageUrl: targetProduct.mainImage || targetProduct.imageUrl,
      category: targetProduct.categoryName,
      rating: targetProduct.rating,
      shopId: targetProduct.shopId || targetShopId,
      shopName: targetProduct.shopName || targetShopName,
    };
    handleSendMessage('Shop ơi, mình đang quan tâm sản phẩm này, tư vấn giúp mình nhé!', prodData);
  };

  const handleSelectProductFromPicker = (p) => {
    const prodData = {
      id: p.id,
      name: p.name,
      price: p.price,
      originalPrice: p.originalPrice,
      imageUrl: p.mainImage || p.imageUrl,
      category: p.categoryName,
      rating: p.rating,
      shopId: p.shopId || targetShopId,
      shopName: p.shopName || targetShopName,
    };
    handleSendMessage(`Mình muốn hỏi về sản phẩm "${p.name}"`, prodData);
  };

  // Lọc sản phẩm của shop hiện tại để hiển thị trong picker
  const shopProducts = defaultProducts.filter(
    (p) => !targetShopId || p.shopId === targetShopId || p.shopId === 1
  );

  const quickQuestions = [
    'Sản phẩm này còn sẵn hàng không shop?',
    'Shop có giao hỏa tốc trong ngày tại Hà Nội không?',
    'Sản phẩm có được bảo hành chính hãng không shop?',
  ];

  if (!isOpen) return null;

  return (
    <AnimatedPresence show={isOpen} mode="modal" dataLenisPrevent={true} className="p-2 sm:p-4" onClose={onClose}>
      <div className="bg-white rounded-3xl w-full max-w-lg shadow-2xl border border-slate-200 overflow-hidden flex flex-col h-[600px] max-h-[92vh] relative">
        {/* Toast thông báo */}
        {toastMessage && (
          <div className="absolute top-16 left-1/2 -translate-x-1/2 z-50 bg-emerald-600 text-white text-xs font-bold px-4 py-2 rounded-full shadow-lg flex items-center gap-2 animate-pulse">
            <CheckCircle2 className="w-4 h-4" />
            <span>{toastMessage}</span>
          </div>
        )}

        {/* 1. Header Khung Chat */}
        <div className="p-4 bg-gradient-to-r from-orange-500 to-amber-500 text-white flex items-center justify-between shadow-sm z-10">
          <div className="flex items-center space-x-3 min-w-0">
            <div className="relative">
              <div className="w-10 h-10 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-white border border-white/30 overflow-hidden">
                <Store className="w-5 h-5" />
              </div>
              <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-400 border-2 border-orange-500 rounded-full" />
            </div>

            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-sm text-white truncate">{targetShopName}</h3>
                <span className="bg-white/20 text-white text-[9px] font-bold px-1.5 py-0.5 rounded-full uppercase">
                  Mall
                </span>
              </div>
              <div className="text-[11px] text-orange-100 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-300" />
                <span>Đang hoạt động • Thường phản hồi sau vài phút</span>
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 text-white flex items-center justify-center transition active:scale-95 cursor-pointer"
            title="Đóng cửa sổ chat"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* 2. Banner Sản Phẩm Đang Xem (nếu có) */}
        {targetProduct && (
          <div className="bg-amber-50/90 border-b border-amber-200/80 px-4 py-2.5 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5 min-w-0">
              <img
                src={targetProduct.mainImage || targetProduct.imageUrl}
                alt={targetProduct.name}
                className="w-9 h-9 rounded-lg object-cover border border-amber-200 flex-shrink-0"
              />
              <div className="min-w-0">
                <p className="text-[11px] text-slate-500">Sản phẩm bạn đang xem:</p>
                <p className="text-xs font-bold text-slate-900 truncate">{targetProduct.name}</p>
              </div>
            </div>
            <button
              onClick={handleSendCurrentProduct}
              className="px-2.5 py-1.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-[11px] font-bold shadow-sm transition active:scale-95 flex-shrink-0 flex items-center gap-1"
            >
              <Send className="w-3 h-3" />
              <span>Gửi Shop</span>
            </button>
          </div>
        )}

        {/* 3. Danh Sách Tin Nhắn */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/60" data-lenis-prevent="true">
          <div className="text-center my-2">
            <span className="px-3 py-1 bg-white border border-slate-200/80 rounded-full text-[10px] text-slate-400 font-medium shadow-xs">
              Hôm nay • Trò chuyện an toàn được bảo vệ bởi QuickShop
            </span>
          </div>

          {messages.map((msg) => {
            const isMe = msg.senderRole === 'BUYER';
            return (
              <div
                key={msg.id}
                className={`flex flex-col ${isMe ? 'items-end' : 'items-start'} space-y-1`}
              >
                <div className="flex items-end gap-2 max-w-[85%]">
                  {!isMe && (
                    <div className="w-7 h-7 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center font-bold text-xs flex-shrink-0 mb-1 border border-orange-200">
                      <Store className="w-3.5 h-3.5" />
                    </div>
                  )}

                  <div
                    className={`p-3.5 rounded-2xl text-xs leading-relaxed shadow-xs ${
                      isMe
                        ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-br-xs font-medium'
                        : 'bg-white text-slate-800 border border-slate-200/90 rounded-bl-xs'
                    }`}
                  >
                    {msg.text && <p className="whitespace-pre-wrap">{msg.text}</p>}

                    {/* Khung Thẻ Sản Phẩm Tương Tác */}
                    {msg.productData && (
                      <ProductChatCard
                        product={msg.productData}
                        onSelectProduct={(pId) => {
                          if (onSelectProduct) onSelectProduct(pId);
                          onClose();
                        }}
                        onBuyNow={(item) => {
                          if (onBuyNow) onBuyNow(item);
                          onClose();
                        }}
                        onToast={showToast}
                        onRequireAuth={onRequireAuth}
                      />
                    )}
                  </div>
                </div>

                <span className="text-[10px] text-slate-400 px-1">
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

        {/* 4. Quick Questions / Product Picker Popover */}
        {showProductPicker && (
          <div className="p-3 bg-white border-t border-slate-200 max-h-48 overflow-y-auto space-y-2 shadow-inner" data-lenis-prevent="true">
            <div className="flex items-center justify-between text-xs font-bold text-slate-700 px-1">
              <span>Chọn sản phẩm để gửi vào chat:</span>
              <button
                onClick={() => setShowProductPicker(false)}
                className="text-slate-400 hover:text-slate-600 text-xs"
              >
                ✕ Đóng
              </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {shopProducts.slice(0, 6).map((prod) => (
                <button
                  type="button"
                  key={prod.id}
                  onClick={() => handleSelectProductFromPicker(prod)}
                  className="flex items-center gap-2 p-2 rounded-xl border border-slate-200 hover:border-orange-400 hover:bg-orange-50/50 cursor-pointer transition text-left group w-full bg-white"
                >
                  <img
                    src={prod.mainImage || prod.imageUrl}
                    alt={prod.name}
                    className="w-10 h-10 rounded-lg object-cover flex-shrink-0"
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

        {/* Quick Suggestion Pills */}
        <div className="px-3 py-2 bg-slate-50 border-t border-slate-200/80 flex items-center gap-1.5 overflow-x-auto no-scrollbar">
          <button
            onClick={() => setShowProductPicker(!showProductPicker)}
            className="px-2.5 py-1 bg-white hover:bg-orange-50 border border-slate-200 hover:border-orange-300 text-slate-700 hover:text-orange-600 rounded-full text-[11px] font-bold flex items-center gap-1 transition flex-shrink-0 active:scale-95"
          >
            <ShoppingBag className="w-3 h-3 text-orange-600" />
            <span>Gửi sản phẩm</span>
          </button>
          {quickQuestions.map((q, idx) => (
            <button
              key={idx}
              onClick={() => handleSendMessage(q)}
              className="px-2.5 py-1 bg-white hover:bg-slate-100 border border-slate-200 text-slate-600 rounded-full text-[11px] transition flex-shrink-0 active:scale-95"
            >
              {q}
            </button>
          ))}
        </div>

        {/* 5. Input Bar */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSendMessage();
          }}
          className="p-3 bg-white border-t border-slate-200 flex items-center gap-2"
        >
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            placeholder="Nhập tin nhắn hoặc mã sản phẩm (VD: SP-1, [PRODUCT:1])..."
            className="flex-1 px-4 py-2.5 bg-slate-100 border border-slate-200 rounded-2xl text-xs text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 transition"
          />
          <button
            type="submit"
            disabled={!inputText.trim()}
            className="w-10 h-10 rounded-2xl bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 disabled:opacity-40 text-white flex items-center justify-center transition shadow-md active:scale-95 flex-shrink-0 cursor-pointer"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </AnimatedPresence>
  );
}
