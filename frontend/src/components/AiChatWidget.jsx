import React, { useState, useEffect, useRef } from 'react';
import { Bot, X, Send, Sparkles, AlertCircle, Move } from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AiChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Position of the circular button (when closed)
  const [buttonPos, setButtonPos] = useState(() => {
    try {
      // Clear legacy offset if previously saved
      localStorage.removeItem('quickshop_chatbot_pos');
      const saved = localStorage.getItem('quickshop_chat_btn_pos');
      if (saved) return JSON.parse(saved);
    } catch (e) {}
    return null; // null defaults to CSS bottom-right: 24px
  });

  // Position of the chat window (when open)
  const [chatPos, setChatPos] = useState(null);

  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef({
    startX: 0,
    startY: 0,
    initialX: 0,
    initialY: 0,
    hasMoved: false,
  });

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

  // Open chat: keep buttonPos untouched, calculate initial chatPos if button was dragged
  const handleOpenChat = () => {
    if (buttonPos) {
      const chatW = Math.min(390, window.innerWidth - 32);
      const chatH = Math.min(520, window.innerHeight - 48);
      // Align bottom-right of chat window with bottom-right of the 60px button
      let cx = buttonPos.x + 60 - chatW;
      let cy = buttonPos.y + 60 - chatH;
      // Ensure it stays safely within viewport
      cx = Math.max(16, Math.min(window.innerWidth - chatW - 16, cx));
      cy = Math.max(16, Math.min(window.innerHeight - chatH - 16, cy));
      setChatPos({ x: cx, y: cy });
    } else {
      setChatPos(null); // Defaults to bottom-right: 24px
    }
    setIsOpen(true);
  };

  const handleCloseChat = () => {
    setIsOpen(false);
  };

  // Drag logic for the circular button (when closed)
  const handleButtonPointerDown = (e) => {
    if (e.button !== undefined && e.button !== 0) return;

    const btnEl = e.currentTarget;
    const rect = btnEl.getBoundingClientRect();

    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      initialX: rect.left,
      initialY: rect.top,
      hasMoved: false,
    };

    setIsDragging(true);

    const handlePointerMove = (moveEvent) => {
      const dx = moveEvent.clientX - dragRef.current.startX;
      const dy = moveEvent.clientY - dragRef.current.startY;

      if (!dragRef.current.hasMoved && Math.hypot(dx, dy) > 5) {
        dragRef.current.hasMoved = true;
      }

      if (dragRef.current.hasMoved) {
        const maxX = window.innerWidth - 68;
        const maxY = window.innerHeight - 68;
        const newX = Math.max(8, Math.min(maxX, dragRef.current.initialX + dx));
        const newY = Math.max(8, Math.min(maxY, dragRef.current.initialY + dy));
        setButtonPos({ x: newX, y: newY });
      }
    };

    const handlePointerUp = () => {
      setIsDragging(false);
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);

      if (dragRef.current.hasMoved) {
        setButtonPos((cur) => {
          if (cur) {
            try {
              localStorage.setItem('quickshop_chat_btn_pos', JSON.stringify(cur));
            } catch (err) {}
          }
          return cur;
        });
      } else {
        // Simple click without drag -> open chat
        handleOpenChat();
      }
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  };

  // Drag logic for the chat window header (when open)
  const handleHeaderPointerDown = (e) => {
    if (e.target.closest('button')) return;
    if (e.button !== undefined && e.button !== 0) return;

    const chatEl = e.currentTarget.closest('.chat-window-container') || e.currentTarget.parentElement;
    const rect = chatEl.getBoundingClientRect();

    dragRef.current = {
      startX: e.clientX,
      startY: e.clientY,
      initialX: rect.left,
      initialY: rect.top,
      hasMoved: false,
    };

    setIsDragging(true);

    const handlePointerMove = (moveEvent) => {
      const dx = moveEvent.clientX - dragRef.current.startX;
      const dy = moveEvent.clientY - dragRef.current.startY;

      if (!dragRef.current.hasMoved && Math.hypot(dx, dy) > 5) {
        dragRef.current.hasMoved = true;
      }

      if (dragRef.current.hasMoved) {
        const chatW = Math.min(390, window.innerWidth - 32);
        const chatH = Math.min(520, window.innerHeight - 48);
        const maxX = window.innerWidth - chatW - 8;
        const maxY = window.innerHeight - chatH - 8;
        const newX = Math.max(8, Math.min(maxX, dragRef.current.initialX + dx));
        const newY = Math.max(8, Math.min(maxY, dragRef.current.initialY + dy));
        setChatPos({ x: newX, y: newY });
      }
    };

    const handlePointerUp = () => {
      setIsDragging(false);
      window.removeEventListener('pointermove', handlePointerMove);
      window.removeEventListener('pointerup', handlePointerUp);
    };

    window.addEventListener('pointermove', handlePointerMove);
    window.addEventListener('pointerup', handlePointerUp);
  };

  const handleSendMessage = async (textToSend) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || loading) return;

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

  const buttonStyle = buttonPos
    ? { left: `${buttonPos.x}px`, top: `${buttonPos.y}px` }
    : { right: '24px', bottom: '24px' };

  const chatStyle = chatPos
    ? { left: `${chatPos.x}px`, top: `${chatPos.y}px` }
    : { right: '24px', bottom: '24px' };

  return (
    <>
      {/* 1. Floating Draggable Circular Button (When Closed) */}
      {!isOpen && (
        <div
          style={buttonStyle}
          className={`fixed z-50 select-none touch-none ${isDragging ? 'cursor-grabbing' : ''}`}
        >
          <div
            onPointerDown={handleButtonPointerDown}
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
        </div>
      )}

      {/* 2. Draggable Chat Window (When Open) */}
      {isOpen && (
        <div
          style={chatStyle}
          className={`fixed z-50 select-none chat-window-container ${isDragging ? 'cursor-grabbing' : ''}`}
        >
          <div className="w-[calc(100vw-32px)] sm:w-[390px] h-[520px] max-h-[calc(100vh-48px)] bg-white rounded-3xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            {/* Header - Drag Handle */}
            <div
              onPointerDown={handleHeaderPointerDown}
              className="p-4 bg-gradient-to-r from-orange-500 via-rose-500 to-purple-600 text-white flex justify-between items-center shadow-md cursor-grab active:cursor-grabbing touch-none"
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
                  onClick={handleCloseChat}
                  className="p-1 rounded-full hover:bg-white/20 transition text-white cursor-pointer"
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
                          className="text-[11px] px-2.5 py-1 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200/80 rounded-full font-medium transition text-left active:scale-95 shadow-sm cursor-pointer"
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
                  className="px-4 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white rounded-xl text-xs font-semibold transition disabled:opacity-40 flex items-center justify-center shadow-md shadow-orange-500/20 active:scale-95 cursor-pointer"
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
        </div>
      )}
    </>
  );
}
