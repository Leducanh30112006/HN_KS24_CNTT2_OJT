import React, { useState, useEffect } from 'react';
import { ShieldCheck, Truck, RotateCcw, Award, ChevronLeft, ChevronRight, Zap, Sparkles } from 'lucide-react';

export default function HeroBanner({ onSelectCategory }) {
  const [currentSlide, setCurrentSlide] = useState(0);

  const slides = [
    {
      title: 'LỄ HỘI CÔNG NGHỆ & MUA SẮM SIÊU TỐC',
      subtitle: 'Giảm đến 50% các dòng Flagship AI, Laptop & Phụ kiện chính hãng',
      tag: '🔥 SIÊU SALE HÔM NAY',
      bgGradient: 'from-orange-600 via-rose-600 to-purple-700',
      image: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
      badge: 'FreeShip Xtra 0đ',
    },
    {
      title: 'THỜI TRANG ĐỈNH CAO - CHUẨN XU HƯỚNG 2026',
      subtitle: 'Bộ sưu tập Polo sợi cafe sinh học kháng khuẩn & Sneaker thể thao',
      tag: '⚡ FLASH DEAL',
      bgGradient: 'from-amber-600 via-orange-600 to-rose-700',
      image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80',
      badge: 'Voucher 50K',
    },
    {
      title: 'NHÀ CỬA HIỆN ĐẠI - CÔNG NGHỆ TỰ ĐỘNG HÓA',
      subtitle: 'Robot hút bụi Omni 3000Pa cảm biến LiDAR & đồ gia dụng thông minh',
      tag: '✨ CHÍNH HÃNG MALL',
      bgGradient: 'from-rose-600 via-purple-600 to-indigo-800',
      image: 'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80',
      badge: 'Bảo hành 12T',
    },
  ];

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [slides.length]);

  return (
    <div className="space-y-4">
      {/* 1. Main Hero Carousel & Sub Banners */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
        {/* Main Banner Slide (Col span 2) */}
        <div className="lg:col-span-2 relative rounded-2xl overflow-hidden shadow-lg min-h-[220px] sm:min-h-[280px] flex items-center">
          {slides.map((s, index) => (
            <div
              key={index}
              className={`absolute inset-0 bg-gradient-to-r ${s.bgGradient} p-6 sm:p-10 flex flex-col justify-center text-white transition-opacity duration-700 ${
                currentSlide === index ? 'opacity-100 z-10 pointer-events-auto' : 'opacity-0 z-0 pointer-events-none'
              }`}
            >
              <div className="max-w-md space-y-2.5">
                <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-white text-xs font-bold tracking-wider">
                  <Zap className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
                  {s.tag}
                </div>
                <h2 className="text-xl sm:text-3xl font-black tracking-tight leading-tight drop-shadow-sm">
                  {s.title}
                </h2>
                <p className="text-xs sm:text-sm text-white/90 line-clamp-2 leading-relaxed">
                  {s.subtitle}
                </p>
                <div className="pt-2 flex items-center gap-3">
                  <span className="px-3.5 py-1.5 bg-white text-orange-600 rounded-xl text-xs font-bold shadow-md">
                    {s.badge}
                  </span>
                  <span className="text-xs text-white/80 font-medium flex items-center gap-1">
                    <Sparkles className="w-3.5 h-3.5" />
                    Bảo chứng bởi QuickShop
                  </span>
                </div>
              </div>

              {/* Decorative Image */}
              <div className="absolute right-4 bottom-4 w-36 h-36 sm:w-56 sm:h-56 hidden sm:block">
                <img
                  src={s.image}
                  alt={s.title}
                  className="w-full h-full object-cover rounded-2xl shadow-2xl border-2 border-white/20 transform rotate-3 hover:rotate-0 transition-transform duration-300"
                />
              </div>
            </div>
          ))}

          {/* Navigation Arrows */}
          <button
            onClick={() => setCurrentSlide((prev) => (prev - 1 + slides.length) % slides.length)}
            className="absolute left-2 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full bg-black/30 hover:bg-black/50 text-white flex items-center justify-center transition backdrop-blur-sm"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={() => setCurrentSlide((prev) => (prev + 1) % slides.length)}
            className="absolute right-2 top-1/2 -translate-y-1/2 z-20 w-8 h-8 rounded-full bg-black/30 hover:bg-black/50 text-white flex items-center justify-center transition backdrop-blur-sm"
          >
            <ChevronRight className="w-5 h-5" />
          </button>

          {/* Slide Indicator Dots */}
          <div className="absolute bottom-3 left-6 z-20 flex space-x-1.5">
            {slides.map((_, i) => (
              <button
                key={i}
                onClick={() => setCurrentSlide(i)}
                className={`h-1.5 rounded-full transition-all ${
                  currentSlide === i ? 'w-6 bg-white' : 'w-2 bg-white/40'
                }`}
              />
            ))}
          </div>
        </div>

        {/* 2 Sub Banners on the Right */}
        <div className="grid grid-cols-2 lg:grid-cols-1 gap-3">
          <div className="relative rounded-2xl overflow-hidden p-4 bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-md flex flex-col justify-between">
            <div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 bg-black/20 rounded-md">
                Độc quyền Sàn
              </span>
              <h3 className="text-sm font-bold mt-2">Hàng Hiệu QuickShop Mall</h3>
              <p className="text-[11px] text-white/90 mt-0.5">Cam kết 100% Chính hãng</p>
            </div>
            <div className="mt-2 text-xs font-semibold text-amber-200 flex items-center gap-1">
              Khám phá ngay →
            </div>
          </div>

          <div className="relative rounded-2xl overflow-hidden p-4 bg-gradient-to-br from-purple-700 to-rose-600 text-white shadow-md flex flex-col justify-between">
            <div>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 bg-black/20 rounded-md">
                Vận chuyển 0Đ
              </span>
              <h3 className="text-sm font-bold mt-2">FreeShip Xtra Toàn Quốc</h3>
              <p className="text-[11px] text-white/90 mt-0.5">Áp dụng đơn từ 99.000đ</p>
            </div>
            <div className="mt-2 text-xs font-semibold text-rose-200 flex items-center gap-1">
              Thu thập mã →
            </div>
          </div>
        </div>
      </div>

      {/* 2. 4 Gold Trust Commitments Bar (Bảo Chứng Vàng) */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-white border border-slate-200/90 rounded-2xl p-4 shadow-sm">
        <div className="flex items-center space-x-3 p-1">
          <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center flex-shrink-0">
            <Award className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-900">100% Chính Hãng</div>
            <div className="text-[11px] text-slate-500">Hoàn tiền 200% nếu giả</div>
          </div>
        </div>

        <div className="flex items-center space-x-3 p-1">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
            <Truck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-900">Giao Nhanh 2H</div>
            <div className="text-[11px] text-slate-500">FreeShip đơn từ 99K</div>
          </div>
        </div>

        <div className="flex items-center space-x-3 p-1">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
            <RotateCcw className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-900">Đổi Trả 7 Ngày</div>
            <div className="text-[11px] text-slate-500">Miễn phí trả hàng tận nhà</div>
          </div>
        </div>

        <div className="flex items-center space-x-3 p-1">
          <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center flex-shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-900">Bảo Vệ Người Mua</div>
            <div className="text-[11px] text-slate-500">Thanh toán Escrow an toàn</div>
          </div>
        </div>
      </div>
    </div>
  );
}
