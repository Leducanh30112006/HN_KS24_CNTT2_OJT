import React, { useState, useEffect } from 'react';
import { Flame, Clock, ArrowLeft, Zap, Sparkles, Filter, ChevronRight, CheckCircle2 } from 'lucide-react';

export default function FlashSalePage({ products = [], onBack, onSelectProduct }) {
  // Live Countdown Timer
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 45, seconds: 18 });
  const [selectedSlot, setSelectedSlot] = useState(0);
  const [selectedCategoryTab, setSelectedCategoryTab] = useState('all');

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
        return { hours: 3, minutes: 0, seconds: 0 };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const timeSlots = [
    { id: 0, time: '09:00', label: 'Đang diễn ra', active: true },
    { id: 1, time: '12:00', label: 'Sắp diễn ra', active: false },
    { id: 2, time: '16:00', label: 'Sắp diễn ra', active: false },
    { id: 3, time: '20:00', label: 'Sắp diễn ra', active: false },
  ];

  const categoryTabs = [
    { id: 'all', label: 'Tất cả deal' },
    { id: 'tech', label: 'Thiết bị & Phụ kiện' },
    { id: 'fashion', label: 'Thời trang nam nữ' },
    { id: 'home', label: 'Đời sống & Gia đình' },
    { id: 'beauty', label: 'Mỹ phẩm & Làm đẹp' },
  ];

  // Enrich & filter flash sale products
  const flashSaleItems = products.map((p, idx) => {
    const discountPercent = 25 + ((idx * 7) % 25);
    const originalPrice = p.minPrice ? Math.round(p.minPrice * (1 + discountPercent / 100)) : 1000000;
    const soldPercent = Math.min(95, 60 + ((idx * 9) % 35));
    return {
      ...p,
      discountPercent,
      originalPrice,
      soldPercent,
    };
  });

  const filteredDeals = flashSaleItems.filter((item) => {
    if (selectedCategoryTab === 'all') return true;
    if (selectedCategoryTab === 'tech') {
      return item.categoryName?.includes('Điện tử') || item.categoryName?.includes('Laptop') || item.categoryName?.includes('Phụ kiện');
    }
    if (selectedCategoryTab === 'fashion') {
      return item.categoryName?.includes('Thời trang') || item.categoryName?.includes('Giày');
    }
    if (selectedCategoryTab === 'home') {
      return item.categoryName?.includes('Nhà') || item.categoryName?.includes('Đời sống');
    }
    if (selectedCategoryTab === 'beauty') {
      return item.categoryName?.includes('Mỹ phẩm') || item.categoryName?.includes('Làm đẹp');
    }
    return true;
  });

  return (
    <div className="space-y-6 pb-12 animate-in fade-in duration-300">
      {/* 1. Header Navigation & Back Button */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <button
          onClick={onBack}
          className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-bold active:scale-95 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại Mua sắm</span>
        </button>

        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <span className="text-slate-400">Trang chủ</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-orange-600 font-bold">Flash Sale Giá Sốc</span>
        </div>
      </div>

      {/* 2. Grand Flash Sale Hero Banner */}
      <div className="bg-gradient-to-r from-orange-600 via-rose-600 to-amber-500 rounded-3xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
        {/* Background Decorative Rings */}
        <div className="absolute -right-10 -bottom-10 w-60 h-60 bg-white/10 rounded-full blur-2xl pointer-events-none"></div>
        <div className="absolute right-40 -top-10 w-40 h-40 bg-amber-300/20 rounded-full blur-xl pointer-events-none"></div>

        <div className="relative z-10 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-amber-200 text-xs font-bold uppercase tracking-wider">
              <Zap className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
              Săn Deal Cực Khủng - Giảm Đến 50%
            </div>
            <h1 className="text-2xl sm:text-4xl font-black uppercase tracking-tight flex items-center gap-2">
              <Flame className="w-8 h-8 sm:w-10 sm:h-10 fill-amber-300 text-amber-300 animate-bounce" />
              <span>Flash Sale Deal Sốc</span>
            </h1>
            <p className="text-xs sm:text-sm text-white/90 max-w-xl">
              Hàng ngàn sản phẩm chính hãng với mức trợ giá đặc biệt chỉ diễn ra trong khung giờ vàng. Số lượng có hạn, nhanh tay săn ngay!
            </p>
          </div>

          {/* Countdown Clock Box */}
          <div className="bg-white/15 backdrop-blur-md rounded-2xl p-4 sm:p-5 border border-white/20 text-center shadow-lg">
            <div className="text-xs font-bold text-amber-200 uppercase tracking-wider flex items-center justify-center gap-1.5 mb-2">
              <Clock className="w-4 h-4" />
              <span>Kết thúc trong</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="bg-slate-950 text-white font-mono font-black text-xl sm:text-2xl w-12 h-12 rounded-xl flex items-center justify-center shadow-inner">
                {String(timeLeft.hours).padStart(2, '0')}
              </div>
              <span className="font-black text-2xl text-amber-300">:</span>
              <div className="bg-slate-950 text-white font-mono font-black text-xl sm:text-2xl w-12 h-12 rounded-xl flex items-center justify-center shadow-inner">
                {String(timeLeft.minutes).padStart(2, '0')}
              </div>
              <span className="font-black text-2xl text-amber-300">:</span>
              <div className="bg-slate-950 text-white font-mono font-black text-xl sm:text-2xl w-12 h-12 rounded-xl flex items-center justify-center shadow-inner animate-pulse">
                {String(timeLeft.seconds).padStart(2, '0')}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 3. Time Slots Selector */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-2 shadow-sm">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {timeSlots.map((slot) => {
            const isSelected = selectedSlot === slot.id;
            return (
              <button
                key={slot.id}
                onClick={() => setSelectedSlot(slot.id)}
                className={`py-3 px-4 rounded-2xl transition flex flex-col items-center justify-center text-center ${
                  isSelected
                    ? 'bg-gradient-to-r from-orange-500 to-rose-600 text-white shadow-md font-bold'
                    : 'hover:bg-slate-50 text-slate-700'
                }`}
              >
                <div className={`text-base sm:text-lg font-black ${isSelected ? 'text-white' : 'text-slate-900'}`}>
                  {slot.time}
                </div>
                <div className={`text-[11px] font-semibold mt-0.5 ${isSelected ? 'text-amber-100' : 'text-slate-400'}`}>
                  {slot.label}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 4. Category Filter Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
        {categoryTabs.map((cat) => (
          <button
            key={cat.id}
            onClick={() => setSelectedCategoryTab(cat.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition shadow-sm ${
              selectedCategoryTab === cat.id
                ? 'bg-orange-600 text-white shadow-orange-500/20'
                : 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-200'
            }`}
          >
            {cat.label}
          </button>
        ))}
      </div>

      {/* 5. Flash Sale Products Grid */}
      {filteredDeals.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-slate-200/90 space-y-3">
          <div className="w-16 h-16 rounded-full bg-orange-50 text-orange-400 mx-auto flex items-center justify-center">
            <Zap className="w-8 h-8" />
          </div>
          <h3 className="font-bold text-sm text-slate-800">Không có deal nào thuộc danh mục này</h3>
          <p className="text-xs text-slate-400">Vui lòng chọn danh mục khác hoặc quay lại xem tất cả deal sốc.</p>
          <button
            onClick={() => setSelectedCategoryTab('all')}
            className="px-4 py-2 bg-orange-600 text-white text-xs font-semibold rounded-xl hover:bg-orange-700 transition"
          >
            Xem tất cả deal
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4">
          {filteredDeals.map((deal) => {
            return (
              <div
                key={deal.id}
                onClick={() => onSelectProduct && onSelectProduct(deal)}
                className="bg-white rounded-3xl border border-slate-200/90 overflow-hidden shadow-sm hover:shadow-xl hover:border-orange-400 transition-all duration-300 cursor-pointer flex flex-col justify-between group hover:-translate-y-1"
              >
                <div>
                  {/* Thumbnail & Badges */}
                  <div className="relative aspect-square overflow-hidden bg-slate-50">
                    <img
                      src={deal.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                      alt={deal.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />

                    {/* Discount badge */}
                    <div className="absolute top-2.5 right-2.5 bg-rose-600 text-white text-xs font-black px-2 py-0.5 rounded-lg shadow-md uppercase tracking-wide">
                      -{deal.discountPercent}%
                    </div>

                    {/* Flash sale tag */}
                    <div className="absolute bottom-2 left-2 bg-amber-400 text-slate-950 text-[10px] font-black px-2 py-0.5 rounded-md uppercase tracking-tight flex items-center gap-1 shadow-sm">
                      <Zap className="w-3 h-3 fill-slate-950" />
                      <span>GIÁ SỐC</span>
                    </div>
                  </div>

                  {/* Body Content */}
                  <div className="p-4 space-y-2">
                    <div className="text-[11px] font-semibold text-orange-600 truncate">
                      {deal.shopName || 'TechWorld Official Store'}
                    </div>

                    <h3 className="text-xs font-semibold text-slate-800 line-clamp-2 leading-relaxed group-hover:text-orange-600 transition">
                      {deal.name}
                    </h3>

                    {/* Price Row */}
                    <div className="pt-1 flex items-baseline gap-2">
                      <span className="text-base sm:text-lg font-black text-rose-600">
                        {formatVND(deal.minPrice || 0)}
                      </span>
                      <span className="text-xs text-slate-400 line-through">
                        {formatVND(deal.originalPrice)}
                      </span>
                    </div>

                    {/* Progress Bar (Đã bán) */}
                    <div className="pt-1">
                      <div className="w-full bg-orange-100 rounded-full h-4 overflow-hidden relative shadow-inner">
                        <div
                          className="bg-gradient-to-r from-orange-500 to-rose-600 h-full rounded-full transition-all duration-500"
                          style={{ width: `${deal.soldPercent}%` }}
                        ></div>
                        <span className="absolute inset-0 flex items-center justify-center text-[10px] font-bold text-white drop-shadow-sm uppercase tracking-wider">
                          ĐÃ BÁN {deal.soldPercent}%
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Footer Action Button */}
                <div className="p-4 pt-0">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectProduct && onSelectProduct(deal);
                    }}
                    className="w-full py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white text-xs font-bold rounded-xl transition shadow-md shadow-orange-500/20 active:scale-95 flex items-center justify-center gap-1.5"
                  >
                    <span>Săn Ngay</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
