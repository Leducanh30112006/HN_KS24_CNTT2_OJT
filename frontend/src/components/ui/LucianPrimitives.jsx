import React, { useRef, useEffect } from 'react';
import { cn } from '../../utils/cn';
import { gsap } from '../../lib/gsap';
import { fixMojibakeText } from '../../utils/vietnameseEncoding';
import { 
  Loader2, ShoppingCart, Eye, Star, ShieldCheck, Sparkles 
} from 'lucide-react';

/**
 * Format a number as Vietnamese Dong currency string (e.g. 19.990.000₫)
 */
export const formatVND = (num) => {
  if (num === null || num === undefined || isNaN(num)) return '0₫';
  return Number(num).toLocaleString('vi-VN') + '₫';
};

/**
 * LucianButton
 * Tactile button with optional GSAP magnetic cursor pull, springy press physics,
 * loading state, and high-craft styling across primary, obsidian, secondary, ghost, icon, and danger variants.
 */
export const LucianButton = React.forwardRef(function LucianButton({
  children,
  variant = 'primary', // 'primary' | 'obsidian' | 'secondary' | 'ghost' | 'icon' | 'danger'
  size = 'md',        // 'xs' | 'sm' | 'md' | 'lg' | 'icon' | 'icon-sm' | 'icon-lg'
  magnetic = false,
  loading = false,
  disabled = false,
  className = '',
  icon: IconComponent,
  rightIcon: RightIconComponent,
  type = 'button',
  onClick,
  ...props
}, forwardedRef) {
  const localRef = useRef(null);
  const buttonRef = forwardedRef || localRef;

  // Magnetic cursor physics using GSAP quickTo (subtle +/- 4px pull)
  useEffect(() => {
    if (!magnetic || disabled || loading) return;
    const btn = buttonRef.current;
    if (!btn) return;

    const xTo = gsap.quickTo(btn, 'x', { duration: 0.28, ease: 'power3.out' });
    const yTo = gsap.quickTo(btn, 'y', { duration: 0.28, ease: 'power3.out' });

    const handleMouseMove = (e) => {
      const rect = btn.getBoundingClientRect();
      const relX = e.clientX - (rect.left + rect.width / 2);
      const relY = e.clientY - (rect.top + rect.height / 2);
      xTo(Math.max(-4, Math.min(4, relX * 0.25)));
      yTo(Math.max(-4, Math.min(4, relY * 0.25)));
    };

    const handleMouseLeave = () => {
      xTo(0);
      yTo(0);
    };

    btn.addEventListener('mousemove', handleMouseMove);
    btn.addEventListener('mouseleave', handleMouseLeave);
    return () => {
      btn.removeEventListener('mousemove', handleMouseMove);
      btn.removeEventListener('mouseleave', handleMouseLeave);
      xTo(0);
      yTo(0);
    };
  }, [magnetic, disabled, loading, buttonRef]);

  // Variant styling
  const variantStyles = {
    primary:
      'bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white shadow-md shadow-orange-500/20 border border-orange-500/20 btn-shimmer relative overflow-hidden',
    obsidian:
      'bg-slate-900 text-white border border-slate-700/80 hover:bg-slate-800 hover:border-orange-500/40 shadow-sm',
    secondary:
      'bg-white border border-slate-200/90 text-slate-700 hover:bg-slate-50 hover:border-slate-300 hover:text-slate-900 shadow-xs',
    ghost:
      'bg-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100/80 active:bg-slate-200/60 border border-transparent',
    icon:
      'bg-white border border-slate-200/90 text-slate-600 hover:text-slate-900 hover:bg-slate-50 hover:border-slate-300 shadow-xs',
    danger:
      'bg-rose-50 border border-rose-200 text-rose-700 hover:bg-rose-100 hover:border-rose-300 shadow-xs',
  };

  // Size styling
  const sizeStyles = {
    xs: 'px-2.5 py-1 text-xs rounded-lg gap-1 font-semibold',
    sm: 'px-3.5 py-1.5 text-xs font-semibold rounded-xl gap-1.5',
    md: 'px-4 py-2 text-xs sm:text-sm font-bold rounded-xl gap-2',
    lg: 'px-6 py-3 text-sm sm:text-base font-bold rounded-2xl gap-2.5',
    icon: 'p-2 rounded-xl',
    'icon-sm': 'p-1.5 rounded-lg',
    'icon-lg': 'p-2.5 rounded-2xl',
  };

  const isDisabled = disabled || loading;

  return (
    <button
      ref={buttonRef}
      type={type}
      disabled={isDisabled}
      onClick={isDisabled ? undefined : onClick}
      className={cn(
        'inline-flex items-center justify-center select-none cursor-pointer font-sans transition-colors duration-150 active:scale-[0.94] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-orange-500/50',
        variantStyles[variant] || variantStyles.primary,
        sizeStyles[size] || sizeStyles.md,
        isDisabled && 'opacity-50 pointer-events-none cursor-not-allowed active:scale-100 shadow-none',
        className
      )}
      {...props}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : IconComponent ? (
        <IconComponent className="w-4 h-4 shrink-0" />
      ) : null}

      {children}

      {!loading && RightIconComponent && (
        <RightIconComponent className="w-4 h-4 shrink-0" />
      )}
    </button>
  );
});

/**
 * SpotlightCard
 * Interactive container that tracks cursor coordinates (--mouse-x, --mouse-y)
 * to project a subtle warm radial border and surface spotlight on hover.
 */
export const SpotlightCard = React.forwardRef(function SpotlightCard({
  children,
  variant = 'default', // 'default' (white) | 'obsidian' (dark) | 'subtle'
  className = '',
  spotlightColor,
  onClick,
  ...props
}, forwardedRef) {
  const localRef = useRef(null);
  const cardRef = forwardedRef || localRef;

  const handleMouseMove = (e) => {
    const el = cardRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    el.style.setProperty('--mouse-x', `${x}px`);
    el.style.setProperty('--mouse-y', `${y}px`);
  };

  const defaultSpotlight = variant === 'obsidian'
    ? 'rgba(249, 115, 22, 0.16)'
    : 'rgba(249, 115, 22, 0.08)';

  const activeSpotlightColor = spotlightColor || defaultSpotlight;

  return (
    <div
      ref={cardRef}
      onMouseMove={handleMouseMove}
      onClick={onClick}
      className={cn(
        'group relative overflow-hidden transition-all duration-300',
        variant === 'obsidian'
          ? 'bg-slate-900/90 text-white border border-slate-800 shadow-xl'
          : variant === 'subtle'
            ? 'bg-slate-50/70 border border-slate-200/70'
            : 'bg-white/90 backdrop-blur-xl border border-slate-200/80 shadow-xs hover:shadow-md hover:border-slate-300/90',
        'rounded-2xl',
        className
      )}
      {...props}
    >
      {/* Radial Spotlight Surface Overlay */}
      <div
        className="pointer-events-none absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-0"
        style={{
          background: `radial-gradient(420px circle at var(--mouse-x, 50%) var(--mouse-y, 50%), ${activeSpotlightColor}, transparent 75%)`,
        }}
      />
      <div className="relative z-10 w-full h-full">
        {children}
      </div>
    </div>
  );
});

/**
 * TiltProductCard
 * Flagship product card with subtle hardware-accelerated 3D tilt (rotateX/rotateY +/- 3.5deg),
 * dynamic specular glass sheen, smooth zoom, and slide-up quick action bar without layout shift.
 */
export const TiltProductCard = React.forwardRef(function TiltProductCard({
  product,
  onSelect,
  onAddToCart,
  onQuickView,
  onSelectShop,
  className = '',
  priority = false,
  ...props
}, forwardedRef) {
  const localRef = useRef(null);
  const cardRef = forwardedRef || localRef;
  const sheenRef = useRef(null);

  // GSAP quickTo for smooth 3D tilt
  useEffect(() => {
    const card = cardRef.current;
    if (!card) return;

    const rotXTo = gsap.quickTo(card, 'rotationX', { duration: 0.35, ease: 'power2.out' });
    const rotYTo = gsap.quickTo(card, 'rotationY', { duration: 0.35, ease: 'power2.out' });

    const handleMouseMove = (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      // +/- 3.5deg subtle tilt
      const rotX = -((y - centerY) / centerY) * 3.5;
      const rotY = ((x - centerX) / centerX) * 3.5;

      rotXTo(rotX);
      rotYTo(rotY);

      if (sheenRef.current) {
        sheenRef.current.style.setProperty('--sheen-x', `${(x / rect.width) * 100}%`);
        sheenRef.current.style.setProperty('--sheen-y', `${(y / rect.height) * 100}%`);
      }
    };

    const handleMouseLeave = () => {
      rotXTo(0);
      rotYTo(0);
    };

    card.addEventListener('mousemove', handleMouseMove);
    card.addEventListener('mouseleave', handleMouseLeave);
    return () => {
      card.removeEventListener('mousemove', handleMouseMove);
      card.removeEventListener('mouseleave', handleMouseLeave);
      rotXTo(0);
      rotYTo(0);
    };
  }, [cardRef]);

  // Normalize product fields
  const cleanName = fixMojibakeText(product?.name || 'Sản phẩm QuickShop');
  const cleanShopName = fixMojibakeText(product?.shopName || 'TechWorld Official Store');
  const price = product?.minPrice || product?.price || 299000;
  const originalPrice = Math.round(price * 1.25);
  const image = product?.mainImage || product?.image || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80';
  const rating = product?.averageRating || 5.0;
  const soldCount = product?.soldCount || 120;

  return (
    <div
      ref={cardRef}
      style={{ transformStyle: 'preserve-3d', perspective: 1000 }}
      className={cn(
        'group relative flex flex-col rounded-2xl bg-white border border-slate-200/80 p-3 shadow-xs hover:shadow-lg hover:border-slate-300/90 transition-all duration-300 cursor-pointer overflow-hidden',
        className
      )}
      onClick={() => onSelect && onSelect(product)}
      {...props}
    >
      {/* Specular Glare Sheen Overlay */}
      <div
        ref={sheenRef}
        className="pointer-events-none absolute inset-0 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10"
        style={{
          background: 'radial-gradient(350px circle at var(--sheen-x, 50%) var(--sheen-y, 50%), rgba(255, 255, 255, 0.35), transparent 75%)',
        }}
      />

      {/* Image Container with Zoom & Slide-up Quick Actions */}
      <div className="relative aspect-square w-full rounded-xl overflow-hidden bg-slate-50 flex items-center justify-center">
        <img
          src={image}
          alt={cleanName}
          loading={priority ? 'eager' : 'lazy'}
          className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500 ease-out"
        />

        {/* Discount Badge */}
        <div className="absolute top-2 left-2 z-20 px-2 py-0.5 rounded-lg bg-gradient-to-r from-orange-500 to-rose-500 text-[10px] font-black text-white shadow-sm tracking-tight">
          -25%
        </div>

        {/* Mall Official Badge */}
        <div className="absolute top-2 right-2 z-20 px-1.5 py-0.5 rounded-md bg-slate-900/85 backdrop-blur-md text-[9px] font-black text-white flex items-center gap-1 border border-slate-700/60 shadow-sm">
          <ShieldCheck className="w-2.5 h-2.5 text-amber-400" />
          <span>MALL</span>
        </div>

        {/* Quick Action Bar (Slide up on hover without layout shift) */}
        <div className="absolute inset-x-2 bottom-2 z-20 translate-y-12 group-hover:translate-y-0 transition-transform duration-300 ease-out flex items-center gap-1.5 p-1.5 bg-white/90 backdrop-blur-md rounded-xl border border-slate-200/70 shadow-lg">
          <LucianButton
            size="xs"
            variant="primary"
            className="flex-1 text-[11px] py-1.5"
            onClick={(e) => {
              e.stopPropagation();
              if (onAddToCart) onAddToCart(product, e);
            }}
          >
            <ShoppingCart className="w-3.5 h-3.5" />
            <span>Thêm giỏ</span>
          </LucianButton>
          <LucianButton
            size="icon-sm"
            variant="secondary"
            title="Xem chi tiết"
            onClick={(e) => {
              e.stopPropagation();
              if (onQuickView) onQuickView(product, e);
              else if (onSelect) onSelect(product);
            }}
          >
            <Eye className="w-3.5 h-3.5 text-slate-600" />
          </LucianButton>
        </div>
      </div>

      {/* Product Information */}
      <div className="flex-1 flex flex-col justify-between pt-3 space-y-2">
        <div className="space-y-1">
          {/* Shop Name & Rating */}
          <div className="flex items-center justify-between text-[11px] text-slate-400 font-medium">
            <span
              className="hover:text-orange-600 transition-colors truncate max-w-[65%]"
              onClick={(e) => {
                if (onSelectShop) {
                  e.stopPropagation();
                  onSelectShop({ id: product.shopId || 1, name: cleanShopName });
                }
              }}
            >
              {cleanShopName}
            </span>
            <div className="flex items-center gap-0.5 text-amber-500 font-bold">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
              <span>{rating.toFixed(1)}</span>
            </div>
          </div>

          {/* Product Title */}
          <h3
            className="text-xs sm:text-sm font-semibold text-slate-800 line-clamp-2 leading-snug group-hover:text-orange-600 transition-colors"
            title={cleanName}
          >
            {cleanName}
          </h3>
        </div>

        {/* Price & Sold Count */}
        <div className="pt-1 flex items-baseline justify-between border-t border-slate-100">
          <div>
            <div className="text-sm sm:text-base font-extrabold font-mono text-orange-600 tracking-tight">
              {formatVND(price)}
            </div>
            <div className="text-[10px] text-slate-400 line-through font-mono">
              {formatVND(originalPrice)}
            </div>
          </div>
          <div className="text-[10px] text-slate-400 font-medium">
            Đã bán {soldCount}
          </div>
        </div>
      </div>
    </div>
  );
});

/**
 * LucianDock
 * Architectural horizontal category dock with active warm indicator glow and smooth hover states.
 */
export const LucianDock = React.forwardRef(function LucianDock({
  categories = [],
  selectedCategory,
  onSelectCategory,
  className = '',
  ...props
}, forwardedRef) {
  const scrollRef = useRef(null);

  return (
    <div
      ref={forwardedRef}
      className={cn(
        'relative rounded-2xl bg-white/85 backdrop-blur-xl border border-slate-200/80 p-2 shadow-xs',
        className
      )}
      {...props}
    >
      <div
        ref={scrollRef}
        className="flex items-center gap-2 overflow-x-auto no-scrollbar scroll-smooth px-1 py-0.5"
      >
        <button
          type="button"
          onClick={() => onSelectCategory && onSelectCategory(null)}
          className={cn(
            'flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all duration-150 select-none active:scale-[0.94] cursor-pointer',
            selectedCategory === null
              ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-500/20 ring-1 ring-orange-500'
              : 'bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200/60'
          )}
        >
          <Sparkles className={cn('w-3.5 h-3.5', selectedCategory === null ? 'text-amber-200' : 'text-slate-400')} />
          <span>Tất cả ngành hàng</span>
        </button>

        {categories.map((cat) => {
          const isSelected = selectedCategory === cat.id;
          return (
            <button
              key={cat.id}
              type="button"
              onClick={() => onSelectCategory && onSelectCategory(cat.id)}
              className={cn(
                'flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all duration-150 select-none active:scale-[0.94] cursor-pointer',
                isSelected
                  ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-500/20 ring-1 ring-orange-500'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-600 hover:text-slate-900 border border-slate-200/60'
              )}
            >
              <span>{fixMojibakeText(cat.name)}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
});

export default {
  LucianButton,
  SpotlightCard,
  TiltProductCard,
  LucianDock,
  formatVND,
};
