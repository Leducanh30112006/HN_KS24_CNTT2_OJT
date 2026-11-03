import React, { useState, useEffect, useRef } from 'react';
import { Zap, Flame, ChevronRight, ChevronLeft } from 'lucide-react';

export default function FlashSaleSection({ products = [], onSelectProduct, onViewAllDeals }) {
  // Countdown Timer: 02:45:18
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 45, seconds: 18 });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

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

  // Duplicate items if list is short to ensure smooth continuous scrolling
  const flashList = products.length >= 4 
    ? [...products, ...products] 
    : [...products, ...products, ...products, ...products];

  const totalItems = flashList.length;

  // Auto-slide carousel continuously and sequentially every 2.5s (pauses on hover)
  useEffect(() => {
    if (isPaused || totalItems === 0) return;

    const interval = setInterval(() => {
      setCurrentIndex((prev) => (prev + 1) % totalItems);
    }, 2500);

    return () => clearInterval(interval);
  }, [isPaused, totalItems]);

  const handlePrev = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev - 1 + totalItems) % totalItems);
  };

  const handleNext = (e) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev + 1) % totalItems);
  };

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div 
      className="bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 rounded-3xl p-4 sm:p-6 shadow-md text-white relative overflow-hidden group"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
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
        onClick={handlePrev}
        className="absolute left-2 top-1/2 -translate-y-1/2 z-10 w-8 h-8 rounded-full bg-black/40 hover:bg-black/70 text-white flex items-center justify-center transition opacity-0 group-hover:opacity-100 shadow-lg"
        title="Sản phẩm trước"
      >
        <ChevronLeft className="w-5 h-5" />
      </button>

      <button
        onClick={handleNext}
        className="absolute right-2 top-1/2 -translate-y-1/2 z-10 w-8 h-8 rounded-full bg-black/40 hover:bg-black/70 text-white flex items-center justify-center transition opacity-0 group-hover:opacity-100 shadow-lg"
        title="Sản phẩm tiếp theo"
      >
        <ChevronRight className="w-5 h-5" />
      </button>

      {/* Continuous Carousel Container */}
      <div className="overflow-hidden py-1">
        <div
          className="flex transition-transform duration-500 ease-out"
          style={{
            transform: `translateX(-${currentIndex * (100 / 4)}%)`,
          }}
        >
          {flashList.map((p, idx) => {
            const discountPercent = 25;
            const originalPrice = p.minPrice ? p.minPrice * 1.33 : 1000000;
            const soldPercent = 70 + ((idx * 7) % 25);

            return (
              <div
                key={`${p.id}-${idx}`}
                className="w-1/2 sm:w-1/3 lg:w-1/4 flex-shrink-0 px-1.5"
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
