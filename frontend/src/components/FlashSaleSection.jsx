import React, { useState, useEffect } from 'react';
import { Zap, Flame, ChevronRight } from 'lucide-react';

export default function FlashSaleSection({ products = [], onSelectProduct }) {
  // Countdown Timer: 02:45:18
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 45, seconds: 18 });

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

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const flashProducts = products.slice(0, 4);

  return (
    <div className="bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 rounded-2xl p-4 sm:p-6 shadow-md text-white">
      {/* Header with Title and Countdown Timer */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 mb-4 pb-3 border-b border-white/20">
        <div className="flex items-center space-x-3">
          <div className="flex items-center gap-1.5 font-black text-xl sm:text-2xl tracking-tight text-white drop-shadow-sm uppercase">
            <Flame className="w-6 h-6 fill-amber-300 text-amber-300 animate-bounce" />
            Flash Sale
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

        <div className="text-xs font-medium text-white/90 hover:text-white cursor-pointer flex items-center gap-1">
          Xem tất cả deal sốc <ChevronRight className="w-4 h-4" />
        </div>
      </div>

      {/* Flash Sale Product Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {flashProducts.map((p) => {
          const discountPercent = 25; // Visual discount
          const originalPrice = p.minPrice ? p.minPrice * 1.33 : 1000000;

          return (
            <div
              key={p.id}
              onClick={() => onSelectProduct(p)}
              className="bg-white rounded-xl p-3 text-slate-900 shadow-sm hover:shadow-xl transition-all cursor-pointer group flex flex-col justify-between"
            >
              <div className="relative aspect-square overflow-hidden rounded-lg mb-2 bg-slate-100">
                <img
                  src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                  alt={p.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
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
                <h4 className="text-xs font-semibold text-slate-800 line-clamp-1 group-hover:text-orange-600 transition">
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
                    <div className="bg-gradient-to-r from-orange-500 to-rose-500 h-full rounded-full w-[78%]"></div>
                    <span className="absolute inset-0 flex items-center justify-center text-[9px] font-bold text-white drop-shadow-sm uppercase">
                      Đã bán 78%
                    </span>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
