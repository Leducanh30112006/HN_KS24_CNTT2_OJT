import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, Send, ShoppingBag, Store, User, Check, Sparkles, 
  Clock, CheckCircle2, Eye, ShoppingCart, Zap, MessageSquare 
} from 'lucide-react';
import { 
  getStoredThreads, 
  sendChatMessage, 
  markChatThreadAsRead 
} from '../stores/useBuyerSellerChatStore';
import { defaultProducts } from '../data/sampleProducts';
import ProductChatCard from './ProductChatCard';

export default function SellerCustomerChat({
  shopId = 1,
  shopName = 'TechWorld Official Store',
  onSelectProduct,
  onToast,
}) {
  const [threads, setThreads] = useState([]);
  const [activeThreadId, setActiveThreadId] = useState('');
  const [searchCustomer, setSearchCustomer] = useState('');
  const [inputText, setInputText] = useState('');
  const [showProductPicker, setShowProductPicker] = useState(false);
  const messagesEndRef = useRef(null);

  // Đóng popover chọn sản phẩm khi bấm phím Escape
  useEffect(() => {
    if (!showProductPicker) return;
    const handleEscape = (e) => {
      if (e.key === 'Escape') setShowProductPicker(false);
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [showProductPicker]);

  // Lấy danh sách cuộc hội thoại của shop
  const loadThreads = () => {
    const all = getStoredThreads();
    const shopThreads = all.filter((t) => !shopId || t.shopId === shopId || t.shopId === 1);
    setThreads(shopThreads);
    if (!activeThreadId && shopThreads.length > 0) {
      setActiveThreadId(shopThreads[0].threadId);
      markChatThreadAsRead(shopThreads[0].threadId, 'SELLER');
    }
  };

  useEffect(() => {
    loadThreads();
  }, [shopId]);

  // Lắng nghe realtime event
  useEffect(() => {
    const handleSync = () => {
      const all = getStoredThreads();
      const shopThreads = all.filter((t) => !shopId || t.shopId === shopId || t.shopId === 1);
      setThreads(shopThreads);
      if (activeThreadId) {
        markChatThreadAsRead(activeThreadId, 'SELLER');
      }
    };
    window.addEventListener('quickshop_chat_update', handleSync);
    return () => window.removeEventListener('quickshop_chat_update', handleSync);
  }, [activeThreadId, shopId]);

  const activeThread = threads.find((t) => t.threadId === activeThreadId) || threads[0];

  useEffect(() => {
    if (activeThreadId) {
      markChatThreadAsRead(activeThreadId, 'SELLER');
    }
  }, [activeThreadId]);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [activeThread?.messages]);

  const handleSendMessage = (textToSend = inputText, productToSend = null) => {
    const trimmed = textToSend.trim();
    if (!trimmed && !productToSend) return;
    if (!activeThread) return;

    let extractedProduct = productToSend;
    if (!extractedProduct && trimmed) {
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
            shopId,
            shopName,
          };
        }
      }
    }

    sendChatMessage({
      threadId: activeThread.threadId,
      senderRole: 'SELLER',
      senderName: shopName,
      text: trimmed,
      productData: extractedProduct,
    });

    setInputText('');
    setShowProductPicker(false);
  };

  const handleSendProduct = (product) => {
    const pr = Number(product.price ?? product.minPrice ?? product.variants?.[0]?.price ?? 0);
    const origPr = Number(product.originalPrice ?? product.maxPrice ?? (pr > 0 ? Math.round(pr * 1.15) : 0)) || 0;
    const prodData = {
      id: product.id,
      name: product.name,
      price: pr,
      minPrice: pr,
      originalPrice: origPr,
      maxPrice: origPr,
      imageUrl: product.mainImage || product.imageUrl || product.images?.[0] || '',
      category: product.categoryName || product.category || '',
      rating: product.rating || product.averageRating || 5,
      shopId,
      shopName,
      variants: product.variants || [],
    };
    handleSendMessage(`Dạ shop gửi bạn thông tin chi tiết sản phẩm "${product.name}" đang có ưu đãi nhé:`, prodData);
  };

  const filteredThreads = threads.filter((t) =>
    (t.userName || '').toLowerCase().includes(searchCustomer.toLowerCase())
  );

  const shopProducts = defaultProducts.filter((p) => p.shopId === shopId || p.shopId === 1);

  return (
    <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col md:flex-row h-[720px] max-h-[85vh]">
      {/* CỘT TRÁI: Danh Sách Cuộc Hội Thoại Của Khách Hàng */}
      <div className="w-full md:w-80 lg:w-96 border-r border-slate-200/80 flex flex-col bg-slate-50/50">
        <div className="p-4 border-b border-slate-200/80 bg-white">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-orange-600" />
              <span>Tin Nhắn Khách Hàng</span>
            </h3>
            <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-orange-100 text-orange-700">
              {threads.length} hội thoại
            </span>
          </div>

          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchCustomer}
              onChange={(e) => setSearchCustomer(e.target.value)}
              placeholder="Tìm kiếm khách hàng..."
              className="w-full pl-9 pr-3 py-1.5 bg-slate-100 border border-slate-200 rounded-xl text-xs text-slate-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-orange-500/20"
            />
          </div>
        </div>

        {/* Danh sách hội thoại */}
        <div className="flex-1 overflow-y-auto divide-y divide-slate-100" data-lenis-prevent="true">
          {filteredThreads.length === 0 ? (
            <div className="p-8 text-center text-slate-400 text-xs">
              Chưa có tin nhắn nào từ khách hàng
            </div>
          ) : (
            filteredThreads.map((thread) => {
              const isActive = thread.threadId === activeThread?.threadId;
              const lastMsg = thread.messages?.[thread.messages.length - 1];
              return (
                <button
                  type="button"
                  key={thread.threadId}
                  onClick={() => setActiveThreadId(thread.threadId)}
                  className={`w-full text-left border-0 p-3.5 flex items-start gap-3 cursor-pointer transition ${
                    isActive ? 'bg-orange-50/70 border-l-2 border-orange-600' : 'bg-transparent hover:bg-slate-100/70'
                  }`}
                >
                  <div className="relative flex-shrink-0">
                    <img
                      src={thread.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                      alt={thread.userName}
                      className="w-10 h-10 rounded-2xl object-cover border border-slate-200"
                    />
                    <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-emerald-500 border-2 border-white rounded-full" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4 className="font-bold text-xs text-slate-900 truncate">
                        {thread.userName}
                      </h4>
                      <span className="text-[10px] text-slate-400">
                        {new Date(thread.updatedAt || Date.now()).toLocaleTimeString('vi-VN', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>

                    <p className="text-[11px] text-slate-500 truncate mt-0.5">
                      {lastMsg?.productData ? `[Thẻ sản phẩm: ${lastMsg.productData.name}]` : (lastMsg?.text || 'Bắt đầu cuộc trò chuyện')}
                    </p>
                  </div>

                  {thread.unreadBySeller > 0 && (
                    <span className="w-5 h-5 rounded-full bg-orange-600 text-white text-[10px] font-black flex items-center justify-center flex-shrink-0">
                      {thread.unreadBySeller}
                    </span>
                  )}
                </button>
              );
            })
          )}
        </div>
      </div>

      {/* CỘT PHẢI: Khung Chat Chi Tiết */}
      {activeThread ? (
        <div className="flex-1 flex flex-col bg-white">
          {/* Header hội thoại */}
          <div className="p-4 border-b border-slate-200/80 flex items-center justify-between bg-slate-50/40">
            <div className="flex items-center gap-3">
              <img
                src={activeThread.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                alt={activeThread.userName}
                className="w-10 h-10 rounded-2xl object-cover border border-slate-200"
              />
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-sm text-slate-900">{activeThread.userName}</h3>
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                    Khách Hàng Đã Mua Hàng
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  Mã tài khoản #{activeThread.userId} • Đang kết nối trực tuyến
                </p>
              </div>
            </div>

            <button
              onClick={() => setShowProductPicker(!showProductPicker)}
              className="px-3 py-1.5 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200/80 rounded-xl text-xs font-bold flex items-center gap-1.5 transition active:scale-95 shadow-xs"
            >
              <ShoppingBag className="w-3.5 h-3.5 text-orange-600" />
              <span>Gửi Mã / Thẻ Sản Phẩm</span>
            </button>
          </div>

          {/* Vùng tin nhắn */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/40" data-lenis-prevent="true">
            {activeThread.messages?.map((msg) => {
              const isSeller = msg.senderRole === 'SELLER';
              return (
                <div
                  key={msg.id}
                  className={`flex flex-col ${isSeller ? 'items-end' : 'items-start'} space-y-1`}
                >
                  <div className="flex items-end gap-2 max-w-[85%]">
                    {!isSeller && (
                      <img
                        src={activeThread.userAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}
                        alt={activeThread.userName}
                        className="w-7 h-7 rounded-xl object-cover flex-shrink-0 mb-1 border border-slate-200"
                      />
                    )}

                    <div
                      className={`p-3.5 rounded-2xl text-xs leading-relaxed shadow-xs ${
                        isSeller
                          ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-br-xs font-medium'
                          : 'bg-white text-slate-800 border border-slate-200/90 rounded-bl-xs'
                      }`}
                    >
                      {msg.text && <p className="whitespace-pre-wrap">{msg.text}</p>}

                      {msg.productData && (
                        <ProductChatCard
                          product={msg.productData}
                          onSelectProduct={onSelectProduct}
                          onToast={onToast}
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

          {/* Popover chọn sản phẩm của Shop để gửi */}
          {showProductPicker && (
            <div className="p-3 bg-white border-t border-slate-200 max-h-56 overflow-y-auto space-y-2 shadow-inner" data-lenis-prevent="true">
              <div className="flex items-center justify-between text-xs font-bold text-slate-700 px-1">
                <span>Chọn sản phẩm của Shop gửi cho khách:</span>
                <button
                  onClick={() => setShowProductPicker(false)}
                  className="text-slate-400 hover:text-slate-600 text-xs"
                >
                  ✕ Đóng
                </button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {shopProducts.map((prod) => (
                  <button
                    type="button"
                    key={prod.id}
                    onClick={() => handleSendProduct(prod)}
                    className="flex items-center gap-2.5 p-2 rounded-xl border border-slate-200 hover:border-orange-400 hover:bg-orange-50/50 cursor-pointer transition text-left group w-full bg-white"
                  >
                    <img
                      src={prod.mainImage || prod.imageUrl}
                      alt={prod.name}
                      className="w-11 h-11 rounded-lg object-cover flex-shrink-0"
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

          {/* Ô nhập tin nhắn */}
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
              placeholder="Nhập phản hồi cho khách hàng, hoặc mã sản phẩm (SP-1, #1)..."
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
      ) : (
        <div className="flex-1 flex items-center justify-center text-slate-400 text-xs">
          Chọn một cuộc hội thoại từ danh sách bên trái để bắt đầu chat
        </div>
      )}
    </div>
  );
}
