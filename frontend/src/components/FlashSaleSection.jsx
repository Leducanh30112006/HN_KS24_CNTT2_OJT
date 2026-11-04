import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Flame, ChevronRight, ChevronLeft } from 'lucide-react';

export default function FlashSaleSection({ products = [], onSelectProduct, onViewAllDeals }) {
  // Countdown Timer: 02:45:18
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 45, seconds: 18 });
  const [isPaused, setIsPaused] = useState(false);

  // Responsive items per view: mobile: 2, tablet: 3, desktop: 4
  const [itemsPerView, setItemsPerView] = useState(() => {
    if (typeof window === 'undefined') return 4;
    if (window.innerWidth < 640) return 2;
    if (window.innerWidth < 1024) return 3;
    return 4;
  });

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 640) {
        setItemsPerView(2);
      } else if (window.innerWidth < 1024) {
        setItemsPerView(3);
      } else {
        setItemsPerView(4);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Countdown timer interval
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) {
          return { ...prev, seconds: prev.seconds - 1 };
        } else if (prev.minutes > 0) {
          return { ...prev, minutes: 59, seconds: 59 };
        } else if (prev.hours > 0) {
          return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        }
        return { hours: 2, minutes: 0, seconds: 0 };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Ensure baseList has enough items (at least 8) for seamless infinite scrolling
  const baseList = useMemo(() => {
    if (!products || products.length === 0) return [];
    let list = [...products];
    while (list.length < 8) {
      list = [...list, ...products];
    }
    return list;
  }, [products]);

  // Infinite loop buffer: [Copy 0 (Head), Copy 1 (Middle - Main viewport), Copy 2 (Tail)]
  const displayList = useMemo(() => {
    if (baseList.length === 0) return [];
    return [...baseList, ...baseList, ...baseList];
  }, [baseList]);

  // Current position starts at Copy 1 (index = baseList.length)
  const [currentIndex, setCurrentIndex] = useState(baseList.length || 0);
  const [isTransitioning, setIsTransitioning] = useState(true);
  const isSlidingRef = useRef(false);

  // Reset to middle block whenever baseList changes
  useEffect(() => {
    if (baseList.length > 0) {
      setIsTransitioning(false);
      setCurrentIndex(baseList.length);
      const raf1 = requestAnimationFrame(() => {
        const raf2 = requestAnimationFrame(() => {
          setIsTransitioning(true);
          isSlidingRef.current = false;
        });
        return () => cancelAnimationFrame(raf2);
      });
      return () => cancelAnimationFrame(raf1);
    }
  }, [baseList.length]);

  const handleNext = () => {
    if (isSlidingRef.current || baseList.length === 0) return;
    isSlidingRef.current = true;
    setIsTransitioning(true);
    setCurrentIndex((prev) => prev + 1);
  };

  const handlePrev = () => {
    if (isSlidingRef.current || baseList.length === 0) return;
    isSlidingRef.current = true;
    setIsTransitioning(true);
    setCurrentIndex((prev) => prev - 1);
  };

  // Seamless jump without animation when crossing boundaries
  const handleTransitionEnd = (e) => {
    if (e.target !== e.currentTarget) return;
    if (e.propertyName !== 'transform') return;

    const L = baseList.length;
    if (L === 0) return;

    if (currentIndex >= 2 * L) {
      // Reached Copy 2 (end of middle): seamlessly teleport back to Copy 1
      setIsTransitioning(false);
      setCurrentIndex(currentIndex - L);

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setIsTransitioning(true);
          isSlidingRef.current = false;
        });
      });
    } else if (currentIndex < L) {
      // Reached Copy 0 (beginning of middle): seamlessly teleport forward to Copy 1
      setIsTransitioning(false);
      setCurrentIndex(currentIndex + L);

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setIsTransitioning(true);
          isSlidingRef.current = false;
        });
      });
    } else {
      isSlidingRef.current = false;
    }
  };

  // Safety watchdog to prevent animation lock
  useEffect(() => {
    const timer = setTimeout(() => {
      isSlidingRef.current = false;
    }, 600);
    return () => clearTimeout(timer);
  }, [currentIndex]);

  // Handle tab visibility change to keep slider properly synced
  useEffect(() => {
    const handleVisibility = () => {
      if (!document.hidden && baseList.length > 0) {
        const L = baseList.length;
        if (currentIndex >= 2 * L || currentIndex < L) {
          setIsTransitioning(false);
          const normalized = L + (currentIndex % L);
          setCurrentIndex(normalized);
          requestAnimationFrame(() => {
            requestAnimationFrame(() => {
              setIsTransitioning(true);
              isSlidingRef.current = false;
            });
          });
        }
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [currentIndex, baseList.length]);

  // Continuous auto-slide forward every 2.5s (pauses on hover)
  useEffect(() => {
    if (isPaused || baseList.length === 0) return;

    const interval = setInterval(() => {
      handleNext();
    }, 2500);

    return () => clearInterval(interval);
  }, [isPaused, baseList.length]);

  // Touch swipe support for mobile
  const touchStartXRef = useRef(null);
  const touchEndXRef = useRef(null);

  const handleTouchStart = (e) => {
    setIsPaused(true);
    touchStartXRef.current = e.targetTouches[0].clientX;
  };

  const handleTouchMove = (e) => {
    touchEndXRef.current = e.targetTouches[0].clientX;
  };

  const handleTouchEnd = () => {
    setIsPaused(false);
    if (touchStartXRef.current !== null && touchEndXRef.current !== null) {
      const distance = touchStartXRef.current - touchEndXRef.current;
      if (distance > 40) {
        handleNext();
      } else if (distance < -40) {
        handlePrev();
      }
    }
    touchStartXRef.current = null;
    touchEndXRef.current = null;
  };

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div 
      className="bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 rounded-3xl p-4 sm:p-6 shadow-md text-white relative overflow-hidden group select-none"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      {/* Header with Title, Countdown Timer and "Xem tất cả deal sốc" */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 mb-4 pb-3 border-b border-white/20">
        <div className="flex items-center space-x-3">
          <div className="flex items-center gap-1.5 font-black text-xl sm:text-2xl tracking-tight text-white drop-shadow-sm uppercase">
            <Flame className="w-6 h-6 fill-amber-300 text-amber-300 animate-bounce" />
            <span>Flash Sale</span>
          </div>

          {/* Countdown Chips */}
          <div className="flex items-center space-x-1 text-xs font-bold text-slate-900">
            <span className="bg-slate-900 text-white px-2 py-1 rounded-md shadow-inner">
              {String(timeLeft.hours).padStart(2, '0')}
            </span>
            <span className="text-white font-bold">:</span>
            <span className="bg-slate-900 text-white px-2 py-1 rounded-md shadow-inner">
              {String(timeLeft.minutes).padStart(2, '0')}
            </span>
            <span className="text-white font-bold">:</span>
            <span className="bg-slate-900 text-white px-2 py-1 rounded-md shadow-inner">
              {String(timeLeft.seconds).padStart(2, '0')}
            </span>
          </div>
        </div>

        {/* Nút Xem tất cả deal sốc chuyển sang trang chuyên biệt */}
        <button
          type="button"
          onClick={onViewAllDeals}
          className="text-xs font-bold text-white hover:text-amber-200 transition flex items-center gap-1 bg-white/10 hover:bg-white/20 px-3 py-1.5 rounded-xl backdrop-blur-sm cursor-pointer shadow-sm active:scale-95"
        >
          <span>Xem tất cả deal sốc</span>
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>

      {/* Navigation Arrows */}
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          handlePrev();
        }}
        className="absolute left-2 top-1/2 -translate-y-1/2 z-10 w-9 h-9 rounded-full bg-black/40 hover:bg-black/70 text-white flex items-center justify-center transition opacity-0 group-hover:opacity-100 shadow-lg cursor-pointer active:scale-90"
        title="Sản phẩm trước"
      >
        <ChevronLeft className="w-5 h-5" />
      </button>

      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation();
          handleNext();
        }}
        className="absolute right-2 top-1/2 -translate-y-1/2 z-10 w-9 h-9 rounded-full bg-black/40 hover:bg-black/70 text-white flex items-center justify-center transition opacity-0 group-hover:opacity-100 shadow-lg cursor-pointer active:scale-90"
        title="Sản phẩm tiếp theo"
      >
        <ChevronRight className="w-5 h-5" />
      </button>

      {/* Continuous Infinite Loop Carousel */}
      <div className="overflow-hidden py-1">
        <div
          className="flex"
          style={{
            transform: `translateX(-${currentIndex * (100 / itemsPerView)}%)`,
            transition: isTransitioning ? 'transform 450ms cubic-bezier(0.25, 1, 0.5, 1)' : 'none',
            willChange: 'transform',
          }}
          onTransitionEnd={handleTransitionEnd}
        >
          {displayList.map((p, idx) => {
            const discountPercent = 25;
            const originalPrice = p.minPrice ? Math.round(p.minPrice * 1.33) : 1000000;
            const soldPercent = 70 + ((idx * 7) % 25);

            return (
              <div
                key={`flash-${p.id}-${idx}`}
                className="flex-shrink-0 px-1.5"
                style={{ width: `${100 / itemsPerView}%` }}
              >
                <div
                  onClick={() => onSelectProduct && onSelectProduct(p)}
                  className="bg-white rounded-2xl p-3 text-slate-900 shadow-sm hover:shadow-xl transition-all cursor-pointer group/card flex flex-col justify-between h-full hover:-translate-y-1"
                >
                  <div className="relative aspect-square overflow-hidden rounded-xl mb-2 bg-slate-100">
                    <img
                      src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                      alt={p.name}
                      className="w-full h-full object-cover group-hover/card:scale-105 transition-transform duration-300"
                      onError={(e) => {
                        e.currentTarget.onerror = null;
                        e.currentTarget.src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80';
                      }}
                      loading="lazy"
                    />
                    {/* Discount Badge */}
                    <div className="absolute top-1.5 right-1.5 bg-rose-600 text-white text-[10px] font-black px-1.5 py-0.5 rounded-md shadow-sm">
                      -{discountPercent}%
                    </div>
                    <div className="absolute bottom-1 left-1 bg-amber-400 text-slate-950 text-[9px] font-bold px-1.5 py-0.5 rounded-md uppercase">
                      ⚡ GIẢM SỐC
                    </div>
                  </div>

                  <div>
                    <h4 className="text-xs font-semibold text-slate-800 line-clamp-1 group-hover/card:text-orange-600 transition">
                      {p.name}
                    </h4>

                    {/* Price Display */}
                    <div className="mt-1.5 flex items-baseline gap-1.5">
                      <span className="text-sm font-black text-rose-600">
                        {formatVND(p.minPrice || 0)}
                      </span>
                      <span className="text-[10px] text-slate-400 line-through">
                        {formatVND(originalPrice)}
                      </span>
                    </div>

                    {/* Progress Bar (Đã bán) */}
                    <div className="mt-2">
                      <div className="w-full bg-orange-100 rounded-full h-3 overflow-hidden relative">
                        <div 
                          className="bg-gradient-to-r from-orange-500 to-rose-500 h-full rounded-full transition-all duration-300"
                          style={{ width: `${soldPercent}%` }}
                        ></div>
                        <span className="absolute inset-0 flex items-center justify-center text-[9px] font-bold text-white drop-shadow-sm uppercase">
                          Đã bán {soldPercent}%
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
