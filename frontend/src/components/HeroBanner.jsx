import React, { useState, useEffect, useRef } from 'react';
import { 
  ShieldCheck, Truck, RotateCcw, Award, ArrowRight, Zap, Sparkles, 
  Flame, Clock, ChevronRight, Star, Tag, ShoppingBag 
} from 'lucide-react';
import { gsap, useGSAP } from '../lib/gsap';
import { LucianButton, SpotlightCard } from './ui/LucianPrimitives';

export default function HeroBanner({ 
  onSelectCategory, 
  onSelectProduct, 
  onNavigateFlashSale, 
  products = [] 
}) {
  const heroRef = useRef(null);
  const spotlightRef = useRef(null);
  const secondsBadgeRef = useRef(null);
  const heroCtaRef = useRef(null);

  // Live countdown timer for the Bento Flash Sale card
  const [timeLeft, setTimeLeft] = useState({ hours: 2, minutes: 45, seconds: 18 });

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) return { ...prev, seconds: prev.seconds - 1 };
        if (prev.minutes > 0) return { ...prev, minutes: 59, seconds: 59 };
        if (prev.hours > 0) return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        return { hours: 2, minutes: 0, seconds: 0 };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Micro-tick transition for seconds badge
  useEffect(() => {
    if (secondsBadgeRef.current) {
      gsap.fromTo(
        secondsBadgeRef.current,
        { scale: 1.15, opacity: 0.8 },
        { scale: 1, opacity: 1, duration: 0.28, ease: 'power2.out' }
      );
    }
  }, [timeLeft.seconds]);

  // Expressive GSAP Choreography: Staggered Bento reveal & cursor spotlight
  useGSAP(() => {
    const bentoItems = heroRef.current?.querySelectorAll('.bento-reveal-item');
    if (bentoItems && bentoItems.length > 0) {
      gsap.fromTo(
        bentoItems,
        { opacity: 0, y: 40, scale: 0.96 },
        {
          opacity: 1,
          y: 0,
          scale: 1,
          duration: 0.75,
          stagger: 0.09,
          ease: 'expo.out',
          clearProps: 'transform',
        }
      );
    }

    // Cursor-tracking warm ambient spotlight follower
    if (heroRef.current && spotlightRef.current) {
      const xTo = gsap.quickTo(spotlightRef.current, 'x', { duration: 0.45, ease: 'power3.out' });
      const yTo = gsap.quickTo(spotlightRef.current, 'y', { duration: 0.45, ease: 'power3.out' });

      const onMouseMove = (e) => {
        const rect = heroRef.current?.getBoundingClientRect();
        if (!rect) return;
        xTo(e.clientX - rect.left - 192);
        yTo(e.clientY - rect.top - 192);
      };

      const heroEl = heroRef.current;
      heroEl.addEventListener('mousemove', onMouseMove);
      return () => {
        heroEl.removeEventListener('mousemove', onMouseMove);
      };
    }

    // Subtle magnetic hover pull on Hero primary CTA
    if (heroCtaRef.current) {
      const el = heroCtaRef.current;
      const xTo = gsap.quickTo(el, 'x', { duration: 0.35, ease: 'power3.out' });
      const yTo = gsap.quickTo(el, 'y', { duration: 0.35, ease: 'power3.out' });

      const onBtnMouseMove = (e) => {
        const rect = el.getBoundingClientRect();
        const x = (e.clientX - (rect.left + rect.width / 2)) * 0.18;
        const y = (e.clientY - (rect.top + rect.height / 2)) * 0.18;
        xTo(Math.max(-4, Math.min(4, x)));
        yTo(Math.max(-4, Math.min(4, y)));
      };

      const onBtnMouseLeave = () => {
        xTo(0);
        yTo(0);
      };

      el.addEventListener('mousemove', onBtnMouseMove);
      el.addEventListener('mouseleave', onBtnMouseLeave);
      return () => {
        el.removeEventListener('mousemove', onBtnMouseMove);
        el.removeEventListener('mouseleave', onBtnMouseLeave);
      };
    }
  });

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const pad = (n) => String(n).padStart(2, '0');

  // Featured spotlight product selection
  const spotlightProduct = products.find((p) => (p.minPrice || 0) > 4000000) || products[0] || {
    id: 1,
    name: 'Điện Thoại QuickPhone 16 Pro Max 256GB Titanium Tự Nhiên',
    categoryName: 'Điện thoại & Phụ kiện',
    minPrice: 29990000,
    mainImage: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
    shopName: 'QuickShop Mall Official',
  };

  return (
    <div ref={heroRef} className="space-y-4">
      {/* ============================================================== */}
      {/* ASYMMETRICAL 3-CARD BENTO HERO GRID (LUCIAN LABS STYLE)         */}
      {/* ============================================================== */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-3.5 sm:gap-4 items-stretch">
        
        {/* CARD 1: MAIN LEFT HERO CARD (OBSIDIAN DARK #0F172A) - SPANS 7 COLS */}
        <div className="bento-reveal-item lg:col-span-7 bg-[#0F172A] border border-slate-800/80 rounded-3xl p-6 sm:p-10 relative overflow-hidden flex flex-col justify-between shadow-2xl shadow-slate-950/20 group select-none">
          {/* Cursor-Tracking Ambient Warm Glow */}
          <div
            ref={spotlightRef}
            className="pointer-events-none absolute -top-12 -left-12 w-96 h-96 rounded-full bg-gradient-to-tr from-orange-500/15 via-amber-400/10 to-transparent blur-3xl -z-0"
            style={{ willChange: 'transform' }}
          />

          {/* Top Metadata Badges */}
          <div className="relative z-10 flex flex-wrap items-center justify-between gap-2.5">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/10 text-amber-300 text-xs font-bold tracking-wide">
              <Zap className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
              <span>QUICKSHOP FLAGSHIP 2026</span>
            </div>

            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 text-xs font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
              <span>7/7 Microservices Up</span>
            </div>
          </div>

          {/* Core Display Typography */}
          <div className="relative z-10 space-y-3 py-6 sm:py-8">
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-white leading-[1.08]">
              KỶ NGUYÊN MUA SẮM <br />
              <span className="text-amber-400">
                TỐC ĐỘ & ĐẲNG CẤP
              </span>
            </h1>

            <p className="text-xs sm:text-sm text-slate-300 max-w-lg leading-relaxed">
              Trải nghiệm mua sắm siêu mượt mà với hàng ngàn ưu đãi hàng hiệu chính hãng. Cam kết bồi thường 200% nếu phát hiện giả mạo, thanh toán ký quỹ Escrow bảo vệ người mua tối đa.
            </p>

            {/* Quick Feature Badges */}
            <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px] text-slate-300">
              <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 flex items-center gap-1">
                <Award className="w-3.5 h-3.5 text-amber-400" /> 100% Chính Hãng
              </span>
              <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 flex items-center gap-1">
                <Truck className="w-3.5 h-3.5 text-emerald-400" /> FreeShip 0Đ
              </span>
              <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 flex items-center gap-1">
                <RotateCcw className="w-3.5 h-3.5 text-blue-400" /> Đổi Trả 7 Ngày
              </span>
            </div>
          </div>

          {/* Primary High-Contrast Conversion CTAs */}
          <div className="relative z-10 flex flex-wrap items-center gap-3 pt-2">
            <LucianButton
              ref={heroCtaRef}
              magnetic
              size="lg"
              variant="primary"
              onClick={() => {
                if (onNavigateFlashSale) onNavigateFlashSale();
                else if (onSelectCategory) onSelectCategory(1);
              }}
              rightIcon={ArrowRight}
            >
              <span>Khám Phá Siêu Deal Sốc</span>
            </LucianButton>

            <LucianButton
              size="lg"
              variant="obsidian"
              onClick={() => {
                const el = document.getElementById('catalog-product-grid');
                if (el) el.scrollIntoView({ behavior: 'smooth' });
              }}
              className="bg-white/10 hover:bg-white/15 text-white border-white/10 backdrop-blur-md"
            >
              Xem Toàn Bộ Sản Phẩm
            </LucianButton>
          </div>
        </div>

        {/* TWO RIGHT STACKED CARDS - SPANS 5 COLS */}
        <div className="lg:col-span-5 flex flex-col gap-3.5 sm:gap-4">
          
          {/* CARD 2: INTERACTIVE FEATURED PRODUCT SPOTLIGHT CARD */}
          <button
            type="button"
            onClick={() => onSelectProduct && onSelectProduct(spotlightProduct)}
            className="bento-reveal-item w-full text-left flex-1 bg-white rounded-3xl p-5 border border-slate-200/90 shadow-sm hover:shadow-xl hover:border-orange-400/80 transition-all duration-300 flex flex-col justify-between cursor-pointer group relative overflow-hidden"
          >
            <div>
              <div className="flex items-center justify-between gap-2 pb-2.5">
                <div className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-orange-50 text-orange-600 font-bold text-[10px] tracking-wide uppercase">
                  <Sparkles className="w-3 h-3 text-orange-500" />
                  <span>Sản Phẩm Nổi Bật Sàn</span>
                </div>
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-full bg-rose-600 text-white shadow-xs">
                  -28% GIẢM SỐC
                </span>
              </div>

              <div className="flex gap-4 items-center pt-1">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl overflow-hidden bg-slate-100/90 flex-shrink-0 border border-slate-100">
                  <img
                    src={spotlightProduct.mainImage}
                    alt={spotlightProduct.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    loading="lazy"
                  />
                </div>

                <div className="space-y-1.5 flex-1 min-w-0">
                  <div className="text-[11px] font-semibold text-orange-600 truncate">
                    {spotlightProduct.categoryName || 'Flagship Chính Hãng'}
                  </div>
                  <h3 className="text-xs sm:text-sm font-bold text-slate-900 line-clamp-2 leading-snug group-hover:text-orange-600 transition">
                    {spotlightProduct.name}
                  </h3>
                  <div className="flex items-baseline gap-2 pt-0.5">
                    <span className="text-base sm:text-lg font-black font-mono text-orange-600">
                      {formatVND(spotlightProduct.minPrice || 0)}
                    </span>
                    <span className="text-[11px] text-slate-400 font-mono line-through">
                      {formatVND((spotlightProduct.minPrice || 0) * 1.28)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-3 pt-2.5 border-t border-slate-100 flex items-center justify-between text-[11px]">
              <span className="text-slate-500 flex items-center gap-1">
                <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                <strong className="text-slate-800">5.0</strong> (1.2k+ đánh giá)
              </span>
              <span className="text-orange-600 font-bold flex items-center gap-0.5 group-hover:translate-x-0.5 transition-transform">
                Xem chi tiết →
              </span>
            </div>
          </button>

          {/* CARD 3: LIVE FLASH SALE COUNTDOWN CARD WITH TABULAR MONOSPACE DIGITS */}
          <div className="bento-reveal-item bg-[#18181B] rounded-3xl p-5 border border-slate-800 text-white shadow-xl flex flex-col justify-between relative overflow-hidden group select-none">
            {/* Subtle Amber Glow Background Accent */}
            <div className="pointer-events-none absolute -right-12 -bottom-12 w-48 h-48 rounded-full bg-orange-600/10 blur-2xl -z-0" />

            <div className="relative z-10 flex items-center justify-between gap-2">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-400/20 text-amber-300 text-[10px] font-bold uppercase tracking-wider">
                <Flame className="w-3.5 h-3.5 fill-amber-300 text-amber-300" />
                <span>FLASH DEAL GIỜ VÀNG</span>
              </div>
              <span className="text-[11px] text-slate-400 font-medium">Kết thúc sau</span>
            </div>

            {/* Tabular Monospace Countdown Display */}
            <div className="relative z-10 flex items-center justify-center gap-2 py-3.5">
              <div className="flex flex-col items-center">
                <span className="w-12 h-11 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-lg sm:text-xl font-black font-mono text-amber-300 shadow-inner">
                  {pad(timeLeft.hours)}
                </span>
                <span className="text-[9px] text-slate-400 mt-1 uppercase font-semibold">Giờ</span>
              </div>

              <span className="text-amber-400 font-mono font-bold text-lg pb-4">:</span>

              <div className="flex flex-col items-center">
                <span className="w-12 h-11 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-lg sm:text-xl font-black font-mono text-amber-300 shadow-inner">
                  {pad(timeLeft.minutes)}
                </span>
                <span className="text-[9px] text-slate-400 mt-1 uppercase font-semibold">Phút</span>
              </div>

              <span className="text-amber-400 font-mono font-bold text-lg pb-4">:</span>

              <div className="flex flex-col items-center">
                <span
                  ref={secondsBadgeRef}
                  className="w-12 h-11 rounded-xl bg-orange-600/30 border border-orange-500/50 flex items-center justify-center text-lg sm:text-xl font-black font-mono text-orange-400 shadow-inner"
                >
                  {pad(timeLeft.seconds)}
                </span>
                <span className="text-[9px] text-orange-400 mt-1 uppercase font-semibold">Giây</span>
              </div>
            </div>

            {/* Quota Progress & Quick Action */}
            <div className="relative z-10 space-y-2">
              <div className="flex justify-between text-[11px]">
                <span className="text-amber-300 font-semibold">Đã bán 82%</span>
                <span className="text-slate-400">Chỉ còn 18 suất ưu đãi</span>
              </div>
              <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                <div className="h-full bg-gradient-to-r from-orange-500 to-amber-400 rounded-full" style={{ width: '82%' }} />
              </div>
              <LucianButton
                size="sm"
                variant="obsidian"
                onClick={onNavigateFlashSale}
                className="w-full mt-2 text-amber-300 hover:text-amber-200 border-slate-800"
                rightIcon={ChevronRight}
              >
                <span>Xem 50+ Deal Sốc Trong Ngày</span>
              </LucianButton>
            </div>
          </div>

        </div>
      </div>

      {/* ============================================================== */}
      {/* 4 GOLD TRUST COMMITMENTS BAR (BẢO CHỨNG VÀNG QUICKSHOP)         */}
      {/* ============================================================== */}
      <div className="bento-reveal-item grid grid-cols-2 sm:grid-cols-4 gap-3 bg-white border border-slate-200/90 rounded-2xl p-4 shadow-sm">
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
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
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
