import React, { useState, useEffect, useRef } from 'react';
import { X, Send, Sparkles, AlertCircle } from 'lucide-react';
import axiosClient from '../api/axiosClient';
import { gsap, useGSAP } from '../lib/gsap';
import ProductChatCard from './ProductChatCard';
import { defaultProducts } from '../data/sampleProducts';

/**
 * Expressive Blinking Robot Mascot SVG Component
 * Driven purely by GSAP:
 * - Natural Blinking Loop (single & double blinks with randomized interval 2.8s-5.2s)
 * - Cursor Tracking via gsap.quickTo
 * - Hover Delight (antenna tilt)
 * - AI Thinking Expression (eyes pulse/scan, antenna wiggle)
 */
function RobotAvatar({
  size = 28,
  className = '',
  isThinking = false,
  isHovered = false,
  cursorOffset = { x: 0, y: 0 },
}) {
  const containerRef = useRef(null);
  const antennaRef = useRef(null);
  const eyeGroupRef = useRef(null);
  const leftEyeRef = useRef(null);
  const rightEyeRef = useRef(null);

  // 1. Natural Blinking Loop
  useEffect(() => {
    let blinkTimer = null;
    let isCancelled = false;

    const runBlink = () => {
      if (isCancelled || !leftEyeRef.current || !rightEyeRef.current) return;
      if (isThinking) {
        blinkTimer = setTimeout(runBlink, 3000);
        return;
      }

      const isDouble = Math.random() > 0.65;
      const tl = gsap.timeline({
        onComplete: () => {
          if (!isCancelled) {
            const nextDelay = gsap.utils.random(2.8, 5.2);
            blinkTimer = setTimeout(runBlink, nextDelay * 1000);
          }
        },
      });

      // Primary Blink: scaleY 1 -> 0.08 -> 1
      tl.to([leftEyeRef.current, rightEyeRef.current], {
        scaleY: 0.08,
        transformOrigin: '50% 50%',
        duration: 0.14,
        ease: 'power2.inOut',
      }).to([leftEyeRef.current, rightEyeRef.current], {
        scaleY: 1,
        transformOrigin: '50% 50%',
        duration: 0.14,
        ease: 'power2.inOut',
      });

      // Occasional Double Blink
      if (isDouble) {
        tl.to([leftEyeRef.current, rightEyeRef.current], {
          scaleY: 0.08,
          transformOrigin: '50% 50%',
          duration: 0.1,
          delay: 0.08,
          ease: 'power2.inOut',
        }).to([leftEyeRef.current, rightEyeRef.current], {
          scaleY: 1,
          transformOrigin: '50% 50%',
          duration: 0.12,
          ease: 'power2.inOut',
        });
      }
    };

    blinkTimer = setTimeout(runBlink, 2000);

    return () => {
      isCancelled = true;
      if (blinkTimer) clearTimeout(blinkTimer);
    };
  }, [isThinking]);

  // 2. Cursor Tracking via gsap.quickTo
  const quickX = useRef(null);
  const quickY = useRef(null);

  useEffect(() => {
    if (eyeGroupRef.current) {
      quickX.current = gsap.quickTo(eyeGroupRef.current, 'x', { duration: 0.28, ease: 'power2.out' });
      quickY.current = gsap.quickTo(eyeGroupRef.current, 'y', { duration: 0.28, ease: 'power2.out' });
    }
  }, []);

  useEffect(() => {
    if (isThinking) return;
    if (quickX.current && quickY.current) {
      const clampedX = Math.max(-2.5, Math.min(2.5, cursorOffset.x));
      const clampedY = Math.max(-2, Math.min(2, cursorOffset.y));
      quickX.current(clampedX);
      quickY.current(clampedY);
    }
  }, [cursorOffset, isThinking]);

  // 3. Hover Delight: Subtle antenna tilt (-12deg -> 8deg -> 0)
  useEffect(() => {
    if (isThinking || !antennaRef.current) return;
    if (isHovered) {
      gsap
        .timeline()
        .to(antennaRef.current, { rotate: -12, duration: 0.16, ease: 'power2.out', transformOrigin: '18px 9px' })
        .to(antennaRef.current, { rotate: 8, duration: 0.14, ease: 'power2.inOut', transformOrigin: '18px 9px' })
        .to(antennaRef.current, { rotate: 0, duration: 0.18, ease: 'back.out(2)', transformOrigin: '18px 9px' });
    }
  }, [isHovered, isThinking]);

  // 4. AI Thinking Expression: Alternating pulse/scan and subtle antenna wiggle
  useEffect(() => {
    if (!isThinking) {
      if (antennaRef.current) gsap.to(antennaRef.current, { rotate: 0, duration: 0.2 });
      if (eyeGroupRef.current) gsap.to(eyeGroupRef.current, { x: 0, y: 0, duration: 0.2 });
      if (leftEyeRef.current && rightEyeRef.current) {
        gsap.to([leftEyeRef.current, rightEyeRef.current], {
          opacity: 1,
          scaleY: 1,
          fill: '#38BDF8',
          duration: 0.2,
        });
      }
      return;
    }

    const thinkTl = gsap.timeline({ repeat: -1, yoyo: true });

    // Antenna wiggle
    if (antennaRef.current) {
      thinkTl.to(
        antennaRef.current,
        { rotate: -10, duration: 0.32, ease: 'sine.inOut', transformOrigin: '18px 9px' },
        0
      ).to(
        antennaRef.current,
        { rotate: 10, duration: 0.32, ease: 'sine.inOut', transformOrigin: '18px 9px' }
      );
    }

    // Eyes scan left-right
    if (eyeGroupRef.current) {
      thinkTl.to(eyeGroupRef.current, { x: 2.2, duration: 0.45, ease: 'sine.inOut' }, 0).to(
        eyeGroupRef.current,
        { x: -2.2, duration: 0.45, ease: 'sine.inOut' }
      );
    }

    // Eyes warm amber pulse
    if (leftEyeRef.current && rightEyeRef.current) {
      thinkTl.to(
        [leftEyeRef.current, rightEyeRef.current],
        {
          fill: '#F59E0B',
          opacity: 0.7,
          scaleY: 0.85,
          duration: 0.45,
          transformOrigin: '50% 50%',
          ease: 'sine.inOut',
        },
        0
      );
    }

    return () => {
      thinkTl.kill();
    };
  }, [isThinking]);

  return (
    <svg
      ref={containerRef}
      width={size}
      height={size}
      viewBox="0 0 36 36"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`overflow-visible select-none pointer-events-none ${className}`}
    >
      <defs>
        <linearGradient id="robotHeadGrad" x1="6" y1="9" x2="30" y2="29" gradientUnits="userSpaceOnUse">
          <stop stopColor="#FFFFFF" />
          <stop offset="1" stopColor="#E2E8F0" />
        </linearGradient>
        <filter id="eyeGlow" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="0.7" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      {/* Antenna */}
      <g ref={antennaRef} style={{ transformOrigin: '18px 9px' }}>
        <line x1="18" y1="9" x2="18" y2="3.8" stroke="white" strokeWidth="2" strokeLinecap="round" />
        <circle cx="18" cy="3" r="2.2" fill="#F97316" stroke="white" strokeWidth="1" />
      </g>

      {/* Side Ears */}
      <rect x="3.2" y="15" width="2.4" height="6" rx="1.2" fill="white" opacity="0.9" />
      <rect x="30.4" y="15" width="2.4" height="6" rx="1.2" fill="white" opacity="0.9" />

      {/* Rounded Head Frame */}
      <rect
        x="5.5"
        y="9"
        width="25"
        height="19"
        rx="7"
        fill="url(#robotHeadGrad)"
        stroke="#CBD5E1"
        strokeWidth="1.2"
      />

      {/* Visor Screen */}
      <rect x="8.5" y="12" width="19" height="13" rx="4.5" fill="#0B0F19" />

      {/* Vertical Pill Eyes Group */}
      <g ref={eyeGroupRef}>
        <rect
          ref={leftEyeRef}
          x="12"
          y="15"
          width="3.4"
          height="7"
          rx="1.7"
          fill="#38BDF8"
          filter="url(#eyeGlow)"
        />
        <rect
          ref={rightEyeRef}
          x="20.6"
          y="15"
          width="3.4"
          height="7"
          rx="1.7"
          fill="#38BDF8"
          filter="url(#eyeGlow)"
        />
      </g>
    </svg>
  );
}

function formatAiText(text, hasCards) {
  if (!text) return '';
  if (!hasCards) return text;

  const lines = text.split('\n');
  const filtered = lines.filter((line) => {
    const trimmed = line.trim();
    if (/^[-*]\s*\*\*/.test(trimmed)) {
      return false;
    }
    return true;
  });

  const result = filtered.join('\n').replace(/\n{3,}/g, '\n\n').trim();
  return result || 'Dưới đây là các sản phẩm phù hợp nhất dành cho bạn:';
}

function findRelevantProducts(queryText = '', replyText = '', allProducts = []) {
  const poolMap = new Map();
  if (Array.isArray(allProducts)) {
    allProducts.forEach((p) => {
      if (p && p.id) poolMap.set(p.id, p);
    });
  }
  if (Array.isArray(defaultProducts)) {
    defaultProducts.forEach((p) => {
      if (p && p.id && !poolMap.has(p.id)) {
        poolMap.set(p.id, p);
      }
    });
  }
  const pool = Array.from(poolMap.values());
  if (pool.length === 0) return [];

  const q = (queryText || '').toLowerCase();
  const r = (replyText || '').toLowerCase();
  const combined = `${q} ${r}`;

  // 1. Trích xuất tên sản phẩm từ gạch đầu dòng in đậm của AI
  const bulletMatches = [...(replyText || '').matchAll(/[-*]\s*\*\*([^*]+)\*\*/g)].map((m) => m[1].trim());
  if (bulletMatches.length > 0) {
    const matched = [];
    const matchedIds = new Set();

    for (const bulletTitle of bulletMatches) {
      const bLower = bulletTitle.toLowerCase();
      const bTokens = bLower
        .replace(/[^a-z0-9à-ỹ\s]/gi, ' ')
        .split(/\s+/)
        .filter((w) => w.length >= 4 || ['ai', 'mac', 'pro', 'max', 'm3', 's24', 'anc', 'rgb', 'gan', 'omni'].includes(w));

      let bestProd = null;
      let maxScore = 0;

      for (const prod of pool) {
        if (matchedIds.has(prod.id)) continue;
        const pName = (prod.name || '').toLowerCase();
        let score = 0;

        if (pName.includes(bLower) || bLower.includes(pName)) score += 12;

        for (const token of bTokens) {
          if (pName.includes(token)) {
            score += token.length >= 5 ? 3 : 1;
          }
        }

        if (bLower.includes('quickphone') && pName.includes('quickphone')) score += 20;
        if (bLower.includes('quickpods') && pName.includes('quickpods')) score += 20;
        if (bLower.includes('quickbook') && pName.includes('quickbook')) score += 20;
        if (bLower.includes('quickwatch') && pName.includes('quickwatch')) score += 20;
        if (bLower.includes('quickbot') && pName.includes('quickbot')) score += 20;
        if (bLower.includes('quickrunner') && pName.includes('quickrunner')) score += 20;
        if (bLower.includes('quickstyle') && pName.includes('quickstyle')) score += 20;
        if (bLower.includes('galaxy') && pName.includes('galaxy')) score += 20;
        if (bLower.includes('polo') && pName.includes('polo')) score += 15;
        if (bLower.includes('serum') && pName.includes('serum')) score += 15;

        if (score > maxScore && score >= 3) {
          maxScore = score;
          bestProd = prod;
        }
      }

      if (bestProd) {
        matched.push(bestProd);
        matchedIds.add(bestProd.id);
      }
    }

    if (matched.length > 0) {
      return matched.slice(0, 3);
    }
  }

  // 2. Phân loại theo ngành hàng / danh mục từ khóa
  const categoryRules = [
    {
      triggers: ['điện thoại', 'smartphone', 'di động', 'iphone', 'samsung', 'galaxy', 'quickphone'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        const cat = (p.categoryName || '').toLowerCase();
        return (
          cat.includes('điện thoại') ||
          name.includes('quickphone') ||
          name.includes('galaxy s24') ||
          name.includes('điện thoại')
        );
      },
    },
    {
      triggers: ['tai nghe', 'headphone', 'airpods', 'quickpods', 'earphone', 'âm thanh'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        return name.includes('tai nghe') || name.includes('quickpods') || name.includes('airpods');
      },
    },
    {
      triggers: ['laptop', 'máy tính', 'macbook', 'pc', 'quickbook', 'asus rog', 'gaming'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        const cat = (p.categoryName || '').toLowerCase();
        return (
          cat.includes('laptop') ||
          name.includes('quickbook') ||
          name.includes('macbook') ||
          name.includes('rog')
        );
      },
    },
    {
      triggers: ['đồng hồ', 'watch', 'smartwatch', 'quickwatch'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        return name.includes('đồng hồ') || name.includes('quickwatch') || name.includes('watch');
      },
    },
    {
      triggers: ['áo', 'quần', 'thời trang', 'polo', 'jean', 'quickstyle'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        const cat = (p.categoryName || '').toLowerCase();
        return cat.includes('thời trang') || name.includes('polo') || name.includes('quần') || name.includes('áo');
      },
    },
    {
      triggers: ['giày', 'sneaker', 'dép', 'quickrunner', 'giày thể thao'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        return name.includes('giày') || name.includes('sneaker') || name.includes('quickrunner');
      },
    },
    {
      triggers: ['mỹ phẩm', 'serum', 'skincare', 'da', 'làm đẹp'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        const cat = (p.categoryName || '').toLowerCase();
        return cat.includes('mỹ phẩm') || name.includes('serum') || name.includes('niacinamide');
      },
    },
    {
      triggers: ['robot', 'hút bụi', 'nhà cửa', 'quickbot'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        return name.includes('robot') || name.includes('hút bụi') || name.includes('quickbot');
      },
    },
    {
      triggers: ['bàn phím', 'bàn phím cơ', 'keyboard', 'chuột', 'keychron'],
      matchFilter: (p) => {
        const name = (p.name || '').toLowerCase();
        return name.includes('bàn phím') || name.includes('keychron') || name.includes('chuột');
      },
    },
  ];

  for (const rule of categoryRules) {
    if (rule.triggers.some((t) => combined.includes(t))) {
      const prods = pool.filter(rule.matchFilter);
      if (prods.length > 0) {
        return prods.slice(0, 2);
      }
    }
  }

  // 3. Fallback khi người dùng hỏi chung chung
  if (
    combined.includes('gợi ý') ||
    combined.includes('sản phẩm') ||
    combined.includes('mua') ||
    combined.includes('hot') ||
    combined.includes('deal')
  ) {
    return pool.slice(0, 2);
  }

  return [];
}

/**
 * MessageRow with GSAP Choreography on New Messages
 * (y: 16 -> 0, scale: 0.96 -> 1, opacity: 0 -> 1, duration: 0.32, ease: "power3.out")
 */
function MessageRow({
  message,
  index,
  isInitialWelcome,
  onSelectProduct,
  onBuyNow,
  onToast,
  onSendMessage,
}) {
  const rowRef = useRef(null);

  useGSAP(() => {
    if (!isInitialWelcome && rowRef.current) {
      gsap.fromTo(
        rowRef.current,
        { y: 16, scale: 0.96, opacity: 0 },
        { y: 0, scale: 1, opacity: 1, duration: 0.32, ease: 'power3.out' }
      );
    }
  }, { scope: rowRef });

  const hasCards = Array.isArray(message.matchedProducts) && message.matchedProducts.length > 0;
  const displayText = message.sender === 'ai' ? formatAiText(message.text, hasCards) : message.text;

  return (
    <div
      ref={rowRef}
      className={`flex flex-col ${message.sender === 'user' ? 'items-end' : 'items-start'}`}
    >
      {displayText && (
        <div
          className={`max-w-[85%] rounded-2xl p-3 text-xs leading-relaxed whitespace-pre-line shadow-sm ${
            message.sender === 'user'
              ? 'bg-gradient-to-r from-orange-500 to-rose-500 text-white rounded-tr-none'
              : 'bg-white dark:bg-[#1A1B25] border border-slate-200 dark:border-white/10 text-slate-800 dark:text-[#F4F0E8] rounded-tl-none'
          } ${isInitialWelcome ? 'chat-cascade-item' : ''}`}
        >
          {displayText}
        </div>
      )}

      {/* Danh sách thẻ sản phẩm trực quan */}
      {hasCards && (
        <div className="w-full space-y-2 mt-2">
          {message.matchedProducts.map((prod) => (
            <ProductChatCard
              key={prod.id}
              product={prod}
              onSelectProduct={onSelectProduct}
              onBuyNow={onBuyNow}
              onToast={onToast}
            />
          ))}
        </div>
      )}

      {/* Quick-Prompt Suggestions Chips */}
      {message.suggestions && message.suggestions.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-2">
          {message.suggestions.map((sug, sIdx) => (
            <button
              key={sIdx}
              type="button"
              onClick={() => onSendMessage(sug)}
              className={`text-[11px] px-2.5 py-1 bg-orange-50 dark:bg-orange-950/40 hover:bg-orange-100 dark:hover:bg-orange-900/60 text-orange-700 dark:text-amber-300 border border-orange-200/80 dark:border-orange-500/30 rounded-full font-medium transition active:scale-95 shadow-sm cursor-pointer ${
                isInitialWelcome ? 'chat-cascade-item' : ''
              }`}
            >
              {sug}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function AiChatWidget({ products = [], onSelectProduct, onBuyNow, onToast }) {
  const [isOpen, setIsOpen] = useState(false);
  const [isClosing, setIsClosing] = useState(false);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);

  // Xóa các tọa độ kéo thả cũ trong localStorage để cố định vĩnh viễn ở góc dưới bên trái
  useEffect(() => {
    try {
      localStorage.removeItem('quickshop_chatbot_pos');
      localStorage.removeItem('quickshop_chat_btn_pos');
    } catch (e) {}
  }, []);

  // Hover & Cursor tracking states cho logo robot
  const [isLauncherHovered, setIsLauncherHovered] = useState(false);
  const [cursorOffset, setCursorOffset] = useState({ x: 0, y: 0 });

  const triggerBtnRef = useRef(null);
  const chatWindowRef = useRef(null);
  const messagesEndRef = useRef(null);

  const [messages, setMessages] = useState([
    {
      sender: 'ai',
      text: 'Chào bạn! Mình là Trợ lý Mua sắm QuickShop AI thông minh. Mình có thể tư vấn sản phẩm, gợi ý cấu hình thông số, cập nhật mã giảm giá và hướng dẫn mua hàng. Bạn đang cần tìm sản phẩm gì hôm nay?',
      suggestions: [
        'Gợi ý điện thoại dưới 30 triệu',
        'Laptop cho sinh viên CNTT',
        'Đồng hồ thông minh thể thao',
        'Mã giảm giá hôm nay',
      ],
    },
  ]);

  // Tọa độ tâm xuất phát hiệu ứng radial spill-out từ nút tròn ở góc dưới bên trái
  const getRadialOrigin = () => {
    const chatH = Math.min(520, typeof window !== 'undefined' ? window.innerHeight - 48 : 520);
    return { x: 30, y: Math.max(30, chatH - 30) };
  };

  // Liquid Radial "Spill-Out" Open Animation từ góc dưới bên trái
  useGSAP(() => {
    if (!isOpen || isClosing || !chatWindowRef.current) return;

    const { x: ox, y: oy } = getRadialOrigin();

    // 1. Expand chat container with GSAP clipPath
    gsap.fromTo(
      chatWindowRef.current,
      {
        clipPath: `circle(28px at ${ox}px ${oy}px)`,
        scale: 0.92,
        opacity: 1,
        transformOrigin: `${ox}px ${oy}px`,
      },
      {
        clipPath: `circle(150% at ${ox}px ${oy}px)`,
        scale: 1,
        duration: 0.55,
        ease: 'expo.out',
      }
    );

    // 2. Cascade header elements, welcome message bubble, and 4 quick chips
    const cascadeItems = chatWindowRef.current.querySelectorAll('.chat-cascade-item');
    if (cascadeItems.length > 0) {
      gsap.fromTo(
        cascadeItems,
        { y: 14, opacity: 0 },
        {
          y: 0,
          opacity: 1,
          stagger: 0.045,
          duration: 0.35,
          ease: 'power2.out',
          delay: 0.1,
        }
      );
    }
  }, [isOpen, isClosing]);

  // Open chat: Mở khung chat bung tỏa từ logo ở góc dưới bên trái
  const handleOpenChat = () => {
    const btnEl = triggerBtnRef.current;
    if (btnEl) {
      gsap.to(btnEl, {
        scale: 0.5,
        opacity: 0,
        duration: 0.18,
        ease: 'power2.in',
      });
    }

    setIsOpen(true);
    setIsClosing(false);
  };

  // Reverse Contraction on Close: clipPath thu nhỏ ngược về tâm logo góc trái
  const handleCloseChat = () => {
    if (!isOpen || isClosing) return;
    setIsClosing(true);

    const { x: ox, y: oy } = getRadialOrigin();

    if (chatWindowRef.current) {
      gsap.to(chatWindowRef.current, {
        clipPath: `circle(28px at ${ox}px ${oy}px)`,
        scale: 0.92,
        duration: 0.38,
        ease: 'power3.inOut',
        transformOrigin: `${ox}px ${oy}px`,
        onComplete: () => {
          setIsOpen(false);
          setIsClosing(false);

          // Nảy logo robot trở lại với hiệu ứng spring pop
          if (triggerBtnRef.current) {
            gsap.fromTo(
              triggerBtnRef.current,
              { scale: 0.8, opacity: 0 },
              { scale: 1, opacity: 1, duration: 0.4, ease: 'back.out(1.7)' }
            );
          }
        },
      });
    } else {
      setIsOpen(false);
      setIsClosing(false);
    }
  };

  // Đóng khi nhấn phím Escape
  useEffect(() => {
    if (!isOpen) return;
    const handleEscape = (e) => {
      if (e.key === 'Escape') handleCloseChat();
    };
    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [isOpen, isClosing]);

  // Tự động cuộn xuống tin nhắn mới nhất
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  // Launcher cursor tracking & hover handlers
  const handleLauncherPointerMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const dx = (e.clientX - centerX) / (rect.width / 2);
    const dy = (e.clientY - centerY) / (rect.height / 2);
    setCursorOffset({ x: dx * 2.5, y: dy * 2 });
  };

  const handleLauncherPointerEnter = () => {
    setIsLauncherHovered(true);
  };

  const handleLauncherPointerLeave = () => {
    setIsLauncherHovered(false);
    setCursorOffset({ x: 0, y: 0 });
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
      const replyText = res?.data?.reply || 'Dưới đây là các gợi ý tốt nhất từ QuickShop AI dành cho bạn:';
      const suggestions = res?.data?.suggestions || [
        'Xem thêm chi tiết sản phẩm',
        'Mã giảm giá hôm nay',
        'Ưu đãi thanh toán 24/7',
      ];
      const matched = findRelevantProducts(text, replyText, products);

      setMessages([
        ...newThread,
        {
          sender: 'ai',
          text: replyText,
          matchedProducts: matched,
          suggestions,
        },
      ]);
    } catch (err) {
      const matched = findRelevantProducts(text, '', products);
      setMessages([
        ...newThread,
        {
          sender: 'ai',
          text:
            matched.length > 0
              ? 'Dưới đây là một số sản phẩm phù hợp nhất trong kho hàng mà bạn có thể tham khảo ngay:'
              : 'Rất tiếc, kết nối đến dịch vụ AI đang bị quá tải hoặc phản hồi chậm. Bạn có thể tham khảo các deal hot hôm nay nhé!',
          matchedProducts: matched.length > 0 ? matched : products && products.slice(0, 2),
          suggestions: ['Xem sản phẩm hot', 'Thử lại câu hỏi', 'Deal Flash Sale'],
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* 1. Nút tròn Logo AI - CỐ ĐỊNH Ở GÓC DƯỚI BÊN TRÁI (Fixed left-6 bottom-6) */}
      {(!isOpen || isClosing) && (
        <div
          ref={triggerBtnRef}
          onPointerMove={handleLauncherPointerMove}
          onPointerEnter={handleLauncherPointerEnter}
          onPointerLeave={handleLauncherPointerLeave}
          className="fixed left-6 bottom-6 z-50 select-none"
        >
          <div
            onClick={handleOpenChat}
            className="btn-shimmer relative group p-3.5 rounded-full bg-gradient-to-tr from-orange-500 via-rose-500 to-amber-500 text-white shadow-2xl shadow-orange-500/40 hover:scale-105 active:scale-95 flex items-center justify-center cursor-pointer transition-transform duration-150"
            title="Bấm để trò chuyện cùng QuickShop AI"
            aria-label="Mở trợ lý ảo QuickShop AI"
            role="button"
            tabIndex={0}
          >
            {/* Vòng hào quang màu cam ấm khi hover */}
            <div className="absolute inset-0 -m-1.5 rounded-full bg-orange-500/25 blur-md pointer-events-none transition-opacity duration-300 opacity-0 group-hover:opacity-100 ring-2 ring-orange-400/50" />

            {/* Mascot Robot vector chớp mắt */}
            <RobotAvatar
              size={32}
              isThinking={loading}
              isHovered={isLauncherHovered}
              cursorOffset={cursorOffset}
            />

            {/* Chấm tròn trạng thái trực tuyến */}
            <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-emerald-400 border-2 border-white rounded-full"></span>

            {/* Tooltip xuất hiện bên phải logo (vì nút ở góc dưới bên trái màn hình) */}
            <div className="absolute left-full ml-3 bg-slate-900/95 backdrop-blur-md text-white text-xs font-semibold px-3 py-1.5 rounded-xl whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 shadow-xl pointer-events-none flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" />
              <span>Bấm để chat cùng QuickShop AI</span>
            </div>
          </div>
        </div>
      )}

      {/* 2. Cửa sổ chat AI - CỐ ĐỊNH Ở GÓC DƯỚI BÊN TRÁI (Fixed left-4 sm:left-6 bottom-4 sm:bottom-6) */}
      {(isOpen || isClosing) && (
        <div
          ref={chatWindowRef}
          className="fixed left-4 sm:left-6 bottom-4 sm:bottom-6 z-50 select-none chat-window-container will-change-[clip-path,transform]"
          data-lenis-prevent="true"
        >
          <div className="w-[calc(100vw-32px)] sm:w-[390px] h-[520px] max-h-[calc(100vh-48px)] bg-white dark:bg-[#13141C] rounded-3xl shadow-2xl border border-slate-200 dark:border-white/10 flex flex-col overflow-hidden">
            {/* Header Cố Định Không Di Chuyển */}
            <div
              className="p-4 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 text-white flex justify-between items-center shadow-md select-none"
              title="Trợ lý mua sắm QuickShop AI"
            >
              <div className="flex items-center space-x-2.5 pointer-events-none chat-cascade-item">
                <div className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center p-0.5">
                  <RobotAvatar size={24} isThinking={loading} />
                </div>
                <div>
                  <h3 className="font-bold text-xs sm:text-sm flex items-center gap-1.5">
                    Trợ lý Mua sắm QuickShop AI
                    <Sparkles className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
                  </h3>
                  <div className="text-[10px] text-white/80 flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block animate-ping"></span>
                    Trực tuyến 24/7 • Trợ lý AI
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-1 chat-cascade-item">
                <button
                  type="button"
                  onClick={handleCloseChat}
                  className="p-1 rounded-full hover:bg-white/20 transition-colors text-white active:scale-90 cursor-pointer"
                  title="Đóng cửa sổ chat"
                  aria-label="Đóng cửa sổ chat AI"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Messages Body */}
            <div
              data-lenis-prevent
              className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/50 dark:bg-[#07080D]/50"
              role="log"
              aria-live="polite"
            >
              {messages.map((m, i) => (
                <MessageRow
                  key={i}
                  message={m}
                  index={i}
                  isInitialWelcome={i === 0}
                  onSelectProduct={onSelectProduct}
                  onBuyNow={onBuyNow}
                  onToast={onToast}
                  onSendMessage={handleSendMessage}
                />
              ))}

              {loading && (
                <div className="flex items-center space-x-2 text-xs text-slate-400 p-2">
                  <span className="w-2 h-2 rounded-full bg-orange-400 animate-pulse"></span>
                  <span className="w-2 h-2 rounded-full bg-orange-500 animate-pulse [animation-delay:0.2s]"></span>
                  <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse [animation-delay:0.4s]"></span>
                  <span>AI đang suy nghĩ câu trả lời...</span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Footer */}
            <div className="p-3 bg-white dark:bg-[#13141C] border-t border-slate-100 dark:border-white/10 space-y-1.5">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  void handleSendMessage();
                }}
                className="flex gap-2"
              >
                <input
                  type="text"
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  placeholder="Hỏi về sản phẩm, giá, voucher..."
                  aria-label="Nhập câu hỏi gửi trợ lý QuickShop AI"
                  className="flex-1 bg-slate-50 dark:bg-[#1A1B25] border border-slate-300 dark:border-white/10 rounded-xl px-3.5 py-2 text-xs text-slate-800 dark:text-[#F4F0E8] focus:outline-none focus:border-orange-500 focus:bg-white dark:focus:bg-[#252632]"
                />
                <button
                  type="submit"
                  disabled={loading || !inputMessage.trim()}
                  aria-label="Gửi câu hỏi cho trợ lý AI"
                  className="px-4 py-2 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white rounded-xl text-xs font-semibold disabled:opacity-40 flex items-center justify-center shadow-md shadow-orange-500/20 active:scale-95 transition-transform duration-100 cursor-pointer"
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
