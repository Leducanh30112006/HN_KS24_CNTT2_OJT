import React, { useState, useEffect, useRef } from 'react';
import { Bot, X, Send, Sparkles, AlertCircle, Move, MessageSquare } from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AiChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Position state (Draggable)
  const [position, setPosition] = useState(() => {
    try {
      const saved = localStorage.getItem('quickshop_chatbot_pos');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return null;
  });

  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef({
    startX: 0,
    startY: 0,
    initialX: 0,
    initialY: 0,
    hasMoved: false,
  });

  // Calculate default position on first mount if not saved
  useEffect(() => {
    if (!position) {
      const defaultX = Math.max(16, window.innerWidth - 88);
      const defaultY = Math.max(16, window.innerHeight - 88);
      setPosition({ x: defaultX, y: defaultY });
    }
  }, []);

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

  // Pointer drag logic
  const handlePointerDown = (e) => {
    // If target is inside an input, button, or textarea, don't drag
    if (e.target.closest('button') && isOpen) return;
    if (e.target.closest('input') || e.target.closest('textarea')) return;

    // Current position
    const currentX = position?.x ?? (window.innerWidth - 88);
    const currentY = position?.y ?? (window.innerHeight - 88);

    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      initialX: currentX,
      initialY: currentY,
      hasMoved: false,
    };

    setIsDragging(true);

    const handlePointerMove = (moveEvent) => {
      const dx = moveEvent.clientX - dragRef.current.startX;
      const dy = moveEvent.clientY - dragRef.current.startY;

      if (!dragRef.current.hasMoved && Math.hypot(dx, dy) > 4) {
        dragRef.current.hasMoved = true;
      }

      if (dragRef.current.hasMoved) {
        const widgetWidth = isOpen ? 380 : 64;
        const widgetHeight = isOpen ? 520 : 64;
        const maxX = window.innerWidth - widgetWidth - 8;
        const maxY = window.innerHeight - widgetHeight - 8;

        const newX = Math.max(8, Math.min(maxX, dragRef.current.initialX + dx));
        const newY = Math.max(8, Math.min(maxY, dragRef.current.initialY + dy));

        setPosition({ x: newX, y: newY });
      }
    };

    const handlePointerUp = () => {
      setIsDragging(false);
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);

      // If moved, save position to localStorage
      if (dragRef.current.hasMoved) {
        setPosition((currentPos) => {
          if (currentPos) {
            try {
              localStorage.setItem('quickshop_chatbot_pos', JSON.stringify(currentPos));
            } catch (err) {}
          }
          return currentPos;
        });
      } else {
        // If it was a simple click and widget is closed, toggle open
        if (!isOpen) {
          // Adjust position so chat window stays inside screen if too close to right edge
          setPosition((prev) => {
            if (!prev) return null;
            const newX = Math.max(10, Math.min(window.innerWidth - 390, prev.x));
            const newY = Math.max(10, Math.min(window.innerHeight - 530, prev.y));
            return { x: newX, y: newY };
          });
          setIsOpen(true);
        }
      }
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  };

  const handleSendMessage = async (textToSend) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || loading) return;

    // Add user message to thread
    const newThread = [...messages, { sender: 'user', text }];
    setMessages(newThread);
    setInputMessage('');
    setLoading(true);

    try {
      const res = await axiosClient.post('/api/ai/chat', { message: text }, { timeout: 35000 });
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

  const stylePosition = position
    ? { left: `${position.x}px`, top: `${position.y}px` }
    : { right: '24px', bottom: '24px' };

  return (
    <div
      style={stylePosition}
      className={`fixed z-50 select-none ${isDragging ? 'cursor-grabbing' : ''}`}
    >
      {/* 1. Floating Draggable Circular Button */}
      {!isOpen && (
        <div
          onPointerDown={handlePointerDown}
          className={`relative group p-4 rounded-full bg-gradient-to-tr from-orange-500 via-rose-500 to-purple-600 text-white shadow-2xl shadow-orange-500/40 hover:scale-105 active:scale-95 transition-all flex items-center justify-center cursor-grab ${
            isDragging ? 'scale-110 shadow-2xl ring-4 ring-orange-400/40' : ''
          }`}
          title="Kéo thả để di chuyển vị trí • Bấm để chat AI"
        >
          <Bot className="w-7 h-7 pointer-events-none" />
          <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-emerald-400 border-2 border-white rounded-full"></span>

          {/* Drag Handle Indicator */}
          <div className="absolute -top-2 -left-2 bg-slate-900/80 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition shadow">
            <Move className="w-2.5 h-2.5" />
          </div>

          {/* Hover Tooltip */}
          <div className="absolute right-full mr-3 bg-slate-900/95 backdrop-blur-md text-white text-xs font-semibold px-3 py-1.5 rounded-xl whitespace-nowrap opacity-0 group-hover:opacity-100 transition shadow-xl pointer-events-none flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Kéo thả di chuyển • Bấm để chat AI</span>
          </div>
        </div>
      )}

      {/* 2. Draggable Chat Window */}
      {isOpen && (
        <div className="w-[360px] sm:w-[390px] h-[520px] bg-white rounded-3xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
          {/* Header - Drag Handle */}
          <div
            onPointerDown={handlePointerDown}
            className="p-4 bg-gradient-to-r from-orange-500 via-rose-500 to-purple-600 text-white flex justify-between items-center shadow-md cursor-grab active:cursor-grabbing"
            title="Kéo giữ để di chuyển cửa sổ chat"
          >
            <div className="flex items-center space-x-2.5 pointer-events-none">
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
                  Trực tuyến 24/7 • Kéo thả tự do
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1">
              <div className="text-white/60 p-1" title="Kéo để di chuyển">
                <Move className="w-3.5 h-3.5" />
              </div>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="p-1 rounded-full hover:bg-white/20 transition text-white"
                title="Đóng cửa sổ chat"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
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
                        type="button"
                        onClick={() => handleSendMessage(sug)}
                        className="text-[11px] px-2.5 py-1 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200/80 rounded-full font-medium transition text-left active:scale-95 shadow-sm"
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
                className="px-4 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white rounded-xl text-xs font-semibold transition disabled:opacity-40 flex items-center justify-center shadow-md shadow-orange-500/20 active:scale-95"
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
