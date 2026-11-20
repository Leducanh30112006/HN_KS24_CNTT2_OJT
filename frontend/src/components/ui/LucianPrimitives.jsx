import React, { useRef, useEffect, useState, createContext, useContext } from 'react';
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

  // Variant styling with Dark Mode awareness
  const variantStyles = {
    primary:
      'bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white shadow-md shadow-orange-500/20 border border-orange-500/20 btn-shimmer relative overflow-hidden',
    obsidian:
      'bg-slate-900 text-white border border-slate-700/80 hover:bg-slate-800 hover:border-orange-500/40 shadow-sm dark:bg-[#1A1B25] dark:border-white/10 dark:hover:border-amber-400/40',
    secondary:
      'bg-white dark:bg-[#1A1B25] border border-slate-200/90 dark:border-white/10 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-[#252632] hover:border-slate-300 dark:hover:border-white/20 hover:text-slate-900 dark:hover:text-white shadow-xs',
    ghost:
      'bg-transparent text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100/80 dark:hover:bg-white/10 active:bg-slate-200/60 dark:active:bg-white/15 border border-transparent',
    icon:
      'p-2 bg-slate-100 dark:bg-white/10 hover:bg-slate-200/80 dark:hover:bg-white/15 text-slate-700 dark:text-slate-200 active:bg-slate-300/80 dark:active:bg-white/20 border border-slate-200/80 dark:border-white/10 rounded-xl',
    danger:
      'bg-rose-600 hover:bg-rose-700 active:bg-rose-800 text-white shadow-sm border border-rose-500',
  };

  const sizeStyles = {
    xs: 'px-2.5 py-1 text-[11px] rounded-lg gap-1',
    sm: 'px-3 py-1.5 text-xs rounded-xl gap-1.5',
    md: 'px-4 py-2 text-xs sm:text-sm rounded-xl gap-2',
    lg: 'px-6 py-3 text-sm sm:text-base rounded-2xl gap-2.5 font-bold',
    icon: 'p-2 rounded-xl',
    'icon-sm': 'p-1.5 rounded-lg',
    'icon-lg': 'p-3 rounded-2xl',
  };

  return (
    <button
      ref={buttonRef}
      type={type}
      disabled={disabled || loading}
      onClick={onClick}
      className={cn(
        'inline-flex items-center justify-center font-bold tracking-tight select-none transition-all duration-150 active:scale-[0.94] cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 focus:outline-none focus:ring-2 focus:ring-orange-500/30',
        variantStyles[variant] || variantStyles.primary,
        sizeStyles[size] || sizeStyles.md,
        className
      )}
      {...props}
    >
      {loading ? (
        <>
          <Loader2 className="w-4 h-4 animate-spin flex-shrink-0" />
          {children && <span>{children}</span>}
        </>
      ) : (
        <>
          {IconComponent && <IconComponent className="w-4 h-4 flex-shrink-0" />}
          {children}
          {RightIconComponent && <RightIconComponent className="w-4 h-4 flex-shrink-0" />}
        </>
      )}
    </button>
  );
});

// Context for multi-card proximity tracking (Lucian #228)
const SpotlightGridContext = createContext(null);

/**
 * SpotlightGrid
 * Container implementing Lucian Labs Prompt #228 multi-card proximity physics:
 * Tracks cursor coordinates across the grid, updating per-card local offsets (--cx, --cy)
 * and smoothly lerping opacity (--o via requestAnimationFrame) on all neighboring cards.
 */
export function SpotlightGrid({ children, className = '', proximity = 260, ...props }) {
  const gridRef = useRef(null);
  const cardsRef = useRef(new Set());
  const rafRef = useRef(null);
  const mousePosRef = useRef({ x: -1000, y: -1000, active: false });
  const cardStatesRef = useRef(new Map());

  const registerCard = (cardEl) => {
    if (cardEl) {
      cardsRef.current.add(cardEl);
      cardStatesRef.current.set(cardEl, { currentO: 0, targetO: 0 });
    }
    return () => {
      cardsRef.current.delete(cardEl);
      cardStatesRef.current.delete(cardEl);
    };
  };

  useEffect(() => {
    const grid = gridRef.current;
    if (!grid) return;

    let lastTime = performance.now();
    const isReduced =
      typeof window !== 'undefined' &&
      window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const tick = (now) => {
      const dt = Math.min((now - lastTime) / 1000, 0.1);
      lastTime = now;

      const { x: mx, y: my, active } = mousePosRef.current;

      cardsRef.current.forEach((card) => {
        if (!card || !card.isConnected) return;
        const rect = card.getBoundingClientRect();
        const state = cardStatesRef.current.get(card) || { currentO: 0, targetO: 0 };

        if (!active) {
          state.targetO = 0;
        } else {
          // Distance from cursor to card bounding box
          const dx = Math.max(rect.left - mx, 0, mx - rect.right);
          const dy = Math.max(rect.top - my, 0, my - rect.bottom);
          const dist = Math.hypot(dx, dy);

          // Card local offsets
          const cx = mx - rect.left;
          const cy = my - rect.top;
          card.style.setProperty('--cx', `${Math.round(cx)}px`);
          card.style.setProperty('--cy', `${Math.round(cy)}px`);

          if (dist === 0) {
            // Cursor inside card
            state.targetO = 1.0;
          } else if (dist < proximity) {
            // In neighboring proximity
            state.targetO = Math.max(0, 1 - dist / proximity);
          } else {
            state.targetO = 0;
          }
        }

        // Lerp opacity: dt * 8 on approach, dt * 2.5 on decay
        if (isReduced) {
          state.currentO = state.targetO;
        } else {
          const lerpRate = state.targetO > state.currentO ? 8 : 2.5;
          state.currentO += (state.targetO - state.currentO) * Math.min(1, dt * lerpRate);
        }

        card.style.setProperty('--o', state.currentO.toFixed(3));
        cardStatesRef.current.set(card, state);
      });

      rafRef.current = requestAnimationFrame(tick);
    };

    const handlePointerMove = (e) => {
      mousePosRef.current = { x: e.clientX, y: e.clientY, active: true };
    };

    const handlePointerLeave = () => {
      mousePosRef.current = { x: -1000, y: -1000, active: false };
    };

    window.addEventListener('pointermove', handlePointerMove, { passive: true });
    document.addEventListener('pointerleave', handlePointerLeave);
    rafRef.current = requestAnimationFrame(tick);

    return () => {
      window.removeEventListener('pointermove', handlePointerMove);
      document.removeEventListener('pointerleave', handlePointerLeave);
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [proximity]);

  return (
    <SpotlightGridContext.Provider value={{ registerCard }}>
      <div ref={gridRef} className={className} {...props}>
        {children}
      </div>
    </SpotlightGridContext.Provider>
  );
}

/**
 * SpotlightCard
 * Lucian Labs Prompt #228 Proximity Border & Surface Glow Card
 * - Works standalone or seamlessly within a <SpotlightGrid>
 * - 1.5px glowing proximity border with warm amber gradient
 * - Sleek dark obsidian surface (#13141C) with subtle radial sheen
 */
export const SpotlightCard = React.forwardRef(function SpotlightCard({
  children,
  className = '',
  innerClassName = '',
  variant = 'default', // 'default' | 'obsidian' | 'subtle'
  spotlightColor,
  onClick,
  ...props
}, forwardedRef) {
  const localRef = useRef(null);
  const cardRef = forwardedRef || localRef;
  const gridCtx = useContext(SpotlightGridContext);

  // Self-contained fallback when not inside a SpotlightGrid
  useEffect(() => {
    const el = cardRef.current;
    if (!el) return;

    if (gridCtx && gridCtx.registerCard) {
      return gridCtx.registerCard(el);
    }

    // Standalone hover handler
    const handleMouseMove = (e) => {
      const rect = el.getBoundingClientRect();
      const cx = e.clientX - rect.left;
      const cy = e.clientY - rect.top;
      el.style.setProperty('--cx', `${cx}px`);
      el.style.setProperty('--cy', `${cy}px`);
      el.style.setProperty('--o', '1');
    };

    const handleMouseLeave = () => {
      el.style.setProperty('--o', '0');
    };

    el.addEventListener('mousemove', handleMouseMove);
    el.addEventListener('mouseleave', handleMouseLeave);
    return () => {
      el.removeEventListener('mousemove', handleMouseMove);
      el.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, [cardRef, gridCtx]);

  return (
    <div
      ref={cardRef}
      data-spotlight-card="true"
      onClick={onClick}
      style={{
        padding: '1.5px',
        background:
          'radial-gradient(180px circle at var(--cx, 50%) var(--cy, 50%), rgba(249,115,22, calc(var(--o, 0) * 0.95)), rgba(251,191,36, calc(var(--o, 0) * 0.45)) 42%, transparent 72%), var(--ux-line, rgba(226,232,240,0.8))',
      }}
      className={cn(
        'group relative rounded-3xl transition-shadow duration-300 shadow-xs hover:shadow-xl dark:shadow-black/40',
        className
      )}
      {...props}
    >
      {/* Inner Surface Card */}
      <div
        className={cn(
          'relative w-full h-full rounded-[calc(1.5rem-1.5px)] overflow-hidden transition-colors duration-300',
          variant === 'obsidian'
            ? 'bg-slate-900 text-white'
            : variant === 'subtle'
            ? 'bg-slate-50/90 dark:bg-[#1A1B25] text-slate-900 dark:text-[#F4F0E8]'
            : 'bg-white dark:bg-[#13141C] text-slate-900 dark:text-[#F4F0E8]',
          innerClassName
        )}
      >
        {/* Subtle Radial Surface Sheen */}
        <div
          className="pointer-events-none absolute inset-0 z-0 transition-opacity duration-300"
          style={{
            background:
              'radial-gradient(240px circle at var(--cx, 50%) var(--cy, 50%), rgba(249,115,22, calc(var(--o, 0) * 0.14)), transparent 60%)',
          }}
        />
        <div className="relative z-10 w-full h-full">
          {children}
        </div>
      </div>
    </div>
  );
});

/**
 * TiltProductCard
 * Flagship product card with subtle hardware-accelerated 3D tilt (rotateX/rotateY +/- 3.5deg),
 * dynamic specular glass sheen, smooth zoom, and slide-up quick action bar without layout shift.
 * Fully styled for Lucian Dark Mode (#13141C) & Proximity Border Glow.
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

  if (!product) return null;

  const {
    id,
    name,
    price = 0,
    originalPrice = 0,
    image,
    imageUrl,
    rating = 5.0,
    soldCount = 0,
    isMall = true,
    shopName,
    discountPercentage,
  } = product;

  const displayImage = image || imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&q=80';
  const cleanName = fixMojibakeText(name);
  const discount = discountPercentage || (originalPrice > price ? Math.round(((originalPrice - price) / originalPrice) * 100) : 0);

  return (
    <div
      ref={cardRef}
      style={{ perspective: '1000px', transformStyle: 'preserve-3d' }}
      className={cn(
        'group relative bg-white dark:bg-[#13141C] rounded-2xl border border-slate-200/90 dark:border-white/10 shadow-xs hover:shadow-xl hover:border-orange-500/50 dark:hover:border-amber-400/50 transition-all duration-300 flex flex-col justify-between overflow-hidden cursor-pointer select-none',
        className
      )}
      onClick={() => onSelect && onSelect(product)}
      {...props}
    >
      {/* Specular glass sheen highlight */}
      <div
        ref={sheenRef}
        className="pointer-events-none absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10"
        style={{
          background: 'radial-gradient(circle at var(--sheen-x, 50%) var(--sheen-y, 50%), rgba(255,255,255,0.18) 0%, transparent 60%)',
        }}
      />

      {/* Top Media Container */}
      <div className="relative aspect-square w-full overflow-hidden bg-slate-100 dark:bg-slate-900 rounded-t-2xl">
        <img
          src={displayImage}
          alt={cleanName}
          loading={priority ? 'eager' : 'lazy'}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&q=80';
          }}
        />

        {/* Mall & Discount Badges */}
        <div className="absolute top-2.5 left-2.5 flex flex-col gap-1 z-10 pointer-events-none">
          {isMall && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-rose-600 text-white font-extrabold text-[10px] tracking-wider shadow-sm">
              <ShieldCheck className="w-3 h-3" />
              MALL
            </span>
          )}
          {discount > 0 && (
            <span className="inline-block px-1.5 py-0.5 rounded-md bg-amber-500 text-white font-extrabold text-[10px] shadow-sm">
              -{discount}%
            </span>
          )}
        </div>

        {/* Slide-Up Quick Action Bar */}
        <div className="absolute inset-x-2 bottom-2 z-20 flex gap-1.5 translate-y-12 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-200 ease-out pointer-events-auto">
          <LucianButton
            size="xs"
            variant="primary"
            className="flex-1 py-2 text-[11px] shadow-lg"
            icon={ShoppingCart}
            onClick={(e) => {
              e.stopPropagation();
              if (onAddToCart) onAddToCart(product);
            }}
          >
            <span>Thêm vào giỏ</span>
          </LucianButton>

          {onQuickView && (
            <LucianButton
              size="xs"
              variant="secondary"
              className="px-2.5 py-2 shadow-lg"
              icon={Eye}
              onClick={(e) => {
                e.stopPropagation();
                onQuickView(product);
              }}
              title="Xem nhanh thông tin"
            />
          )}
        </div>
      </div>

      {/* Bottom Content Body */}
      <div className="p-3.5 flex flex-col flex-1 justify-between gap-2.5">
        <div className="space-y-1">
          {/* Shop Name & Rating */}
          <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400">
            <span className="truncate font-medium max-w-[130px] hover:text-orange-600 dark:hover:text-amber-400">
              {shopName || 'QuickShop Official'}
            </span>
            <div className="flex items-center gap-1 font-mono text-amber-500 dark:text-amber-400">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
              <span>{rating.toFixed(1)}</span>
            </div>
          </div>

          {/* Product Title */}
          <h3
            className="text-xs sm:text-sm font-semibold text-slate-800 dark:text-slate-100 line-clamp-2 leading-snug group-hover:text-orange-600 dark:group-hover:text-amber-400 transition-colors"
            title={cleanName}
          >
            {cleanName}
          </h3>
        </div>

        {/* Price & Sold Count */}
        <div className="pt-1 flex items-baseline justify-between border-t border-slate-100 dark:border-white/5">
          <div>
            <div className="text-sm sm:text-base font-extrabold font-mono text-orange-600 dark:text-orange-500 tracking-tight">
              {formatVND(price)}
            </div>
            <div className="text-[10px] text-slate-400 dark:text-slate-500 line-through font-mono">
              {formatVND(originalPrice)}
            </div>
          </div>
          <div className="text-[10px] text-slate-400 dark:text-slate-500 font-medium">
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
        'relative rounded-2xl bg-white/85 dark:bg-[#13141C]/85 backdrop-blur-xl border border-slate-200/80 dark:border-white/10 p-2 shadow-xs',
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
              : 'bg-slate-50 dark:bg-white/5 hover:bg-slate-100 dark:hover:bg-white/10 text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200/60 dark:border-white/10'
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
                  : 'bg-slate-50 dark:bg-white/5 hover:bg-slate-100 dark:hover:bg-white/10 text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200/60 dark:border-white/10'
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
  SpotlightGrid,
  SpotlightCard,
  TiltProductCard,
  LucianDock,
  formatVND,
};
