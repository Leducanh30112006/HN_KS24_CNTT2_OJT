import React, { useState } from 'react';
import { Bot, X, Send, Sparkles, AlertCircle, ShoppingBag, MessageSquare } from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AiChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const [messages, setMessages] = useState([
    {
      sender: 'ai',
      text: 'Chào bạn! Mình là Trợ lý Mua sắm QuickShop AI thông minh. Mình có thể tư vấn sản phẩm, gợi ý cấu hình thông số, cập nhật mã giảm giá và hướng dẫn mua hàng. Bạn đang cần tìm sản phẩm gì hôm nay?',
      suggestions: [
        'Gợi ý điện thoại dưới 30 triệu',
        'Laptop cho sinh viên CNTT',
        'Mã giảm giá hôm nay',
        'Chính sách đổi trả 7 ngày',
      ],
    },
  ]);

  const handleSendMessage = async (textToSend) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || loading) return;

    // Add user message to thread
    const newThread = [...messages, { sender: 'user', text }];
    setMessages(newThread);
    setInputMessage('');
    setLoading(true);

    try {
      const res = await axiosClient.post('/api/ai/chat', { message: text });
      if (res.success && res.data) {
        setMessages([
          ...newThread,
          {
            sender: 'ai',
            text: res.data.reply,
            suggestions: res.data.suggestions || [],
          },
        ]);
      }
    } catch (err) {
      setMessages([
        ...newThread,
        {
          sender: 'ai',
          text: 'Rất tiếc, hiện tại kết nối đến hệ sinh thái Notification & AI Service (Port 8086) đang bị gián đoạn. Bạn vui lòng thử lại sau giây lát nhé!',
          suggestions: ['Thử lại câu hỏi', 'Xem sản phẩm hot'],
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {/* Floating Toggle Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="relative group p-4 rounded-full bg-gradient-to-tr from-orange-500 via-rose-500 to-purple-600 text-white shadow-xl shadow-orange-500/30 hover:scale-105 transition-all flex items-center justify-center animate-bounce duration-1000"
          title="Chat với Trợ lý AI QuickShop"
        >
          <Bot className="w-7 h-7" />
          <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-emerald-400 border-2 border-white rounded-full"></span>
          <div className="absolute right-full mr-3 bg-slate-900 text-white text-xs font-semibold px-3 py-1.5 rounded-xl whitespace-nowrap opacity-0 group-hover:opacity-100 transition shadow-lg pointer-events-none">
            Trợ lý QuickShop AI ✨
          </div>
        </button>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div className="w-[360px] sm:w-[400px] h-[520px] bg-white rounded-3xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
          {/* Header */}
          <div className="p-4 bg-gradient-to-r from-orange-500 via-rose-500 to-purple-600 text-white flex justify-between items-center shadow-md">
            <div className="flex items-center space-x-2.5">
              <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center">
                <Bot className="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 className="font-bold text-xs sm:text-sm flex items-center gap-1.5">
                  Trợ lý Mua sắm QuickShop AI
                  <Sparkles className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
                </h3>
                <div className="text-[10px] text-white/80 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block animate-ping"></span>
                  Trực tuyến 24/7 • Chống Injection
                </div>
              </div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="p-1 rounded-full hover:bg-white/20 transition text-white"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Messages Body */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/50">
            {messages.map((m, i) => (
              <div
                key={i}
                className={`flex flex-col ${m.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[85%] rounded-2xl p-3 text-xs leading-relaxed whitespace-pre-line shadow-sm ${
                    m.sender === 'user'
                      ? 'bg-gradient-to-r from-orange-500 to-rose-500 text-white rounded-tr-none'
                      : 'bg-white border border-slate-200 text-slate-800 rounded-tl-none'
                  }`}
                >
                  {m.text}
                </div>

                {/* Suggestions Chips from AI */}
                {m.suggestions && m.suggestions.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {m.suggestions.map((sug, sIdx) => (
                      <button
                        key={sIdx}
                        onClick={() => handleSendMessage(sug)}
                        className="text-[11px] px-2.5 py-1 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200/80 rounded-full font-medium transition text-left active:scale-95"
                      >
                        {sug}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div className="flex items-center space-x-2 text-xs text-slate-400 p-2">
                <span className="w-2 h-2 rounded-full bg-orange-400 animate-bounce"></span>
                <span className="w-2 h-2 rounded-full bg-orange-500 animate-bounce [animation-delay:0.2s]"></span>
                <span className="w-2 h-2 rounded-full bg-rose-500 animate-bounce [animation-delay:0.4s]"></span>
                <span>AI đang suy nghĩ câu trả lời...</span>
              </div>
            )}
          </div>

          {/* Input Footer */}
          <div className="p-3 bg-white border-t border-slate-100 space-y-1.5">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="flex gap-2"
            >
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="Hỏi về sản phẩm, giá, voucher..."
                className="flex-1 bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-xs text-slate-800 focus:outline-none focus:border-orange-500 focus:bg-white"
              />
              <button
                type="submit"
                disabled={loading || !inputMessage.trim()}
                className="px-4 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white rounded-xl text-xs font-semibold transition disabled:opacity-40 flex items-center justify-center shadow-md shadow-orange-500/20"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </form>
            <div className="text-[10px] text-slate-400 flex items-center justify-center gap-1">
              <AlertCircle className="w-3 h-3 text-orange-400" />
              Mọi truy vấn đều được bảo vệ bởi bộ lọc Prompt Sanitization (FR-10)
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
