import React, { useRef, useEffect } from 'react';
import { Star, ShoppingCart, MapPin, ShieldCheck } from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';
import { fixMojibakeText } from '../utils/vietnameseEncoding';
import { FALLBACK_PRODUCT_IMAGE, handleImageError } from '../utils/imageFallback';
import { gsap } from '../lib/gsap';
import { LucianButton, formatVND } from './ui/LucianPrimitives';

export default function ProductCard({ product, onSelect, onRequireAuth, onSelectShop }) {
  const addToCart = useCartStore((state) => state.addToCart);
  const { isAuthenticated } = useAuthStore();
  const cardRef = useRef(null);
  const sheenRef = useRef(null);

  // Hardware-accelerated 3D tilt with GSAP quickTo
  useEffect(() => {
    const card = cardRef.current;
    if (!card) return;

    const rotXTo = gsap.quickTo(card, 'rotationX', { duration: 0.32, ease: 'power2.out' });
    const rotYTo = gsap.quickTo(card, 'rotationY', { duration: 0.32, ease: 'power2.out' });

    const handleMouseMove = (e) => {
      const rect = card.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top;
      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      // Subtle +/- 3.5deg tilt
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
  }, []);

  const handleQuickAdd = (e) => {
    e.stopPropagation();
    if (!isAuthenticated) {
      if (onRequireAuth) {
        onRequireAuth('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
      }
      return;
    }
    const defaultVariant = product.variants && product.variants.length > 0 ? product.variants[0] : null;
    addToCart(product, defaultVariant, 1);
  };

  const isMall = product.shopId === 1;
  const originalPrice = product.minPrice ? product.minPrice * 1.25 : 0;
  const rating = product.averageRating || 5.0;
  const soldCount = product.reviewCount ? `${product.reviewCount * 12}+` : '1.2k+';

  return (
    <div
      ref={cardRef}
      style={{ transformStyle: 'preserve-3d', perspective: 1000 }}
      className="product-card-item bg-white rounded-3xl border border-slate-200/80 overflow-hidden shadow-xs hover:shadow-xl hover:border-orange-400/80 transition-shadow transition-colors duration-300 ease-out cursor-pointer flex flex-col justify-between group relative select-none"
    >
      {/* Specular Glare Sheen Overlay */}
      <div
        ref={sheenRef}
        className="pointer-events-none absolute inset-0 rounded-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-10"
        style={{
          background: 'radial-gradient(350px circle at var(--sheen-x, 50%) var(--sheen-y, 50%), rgba(255, 255, 255, 0.4), transparent 75%)',
        }}
      />

      <button
        type="button"
        onClick={() => onSelect(product)}
        aria-label={`Xem chi tiết ${fixMojibakeText(product.name)}`}
        className="absolute inset-0 z-0 w-full h-full bg-transparent border-0 cursor-pointer"
      />
      <div>
        {/* Product Image & Badges: Clean Aspect-[4/5] Rounded Framing */}
        <div className="relative aspect-[4/5] overflow-hidden bg-slate-100/80 m-2 sm:m-2.5 rounded-2xl pointer-events-none">
          <img
            src={product.mainImage || FALLBACK_PRODUCT_IMAGE}
            alt={product.name}
            onError={handleImageError}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out pointer-events-none"
            loading="lazy"
          />

          {/* Badges Top Left: Mall or Yêu Thích */}
          <div className="absolute top-2 left-2 flex flex-col gap-1 items-start pointer-events-none z-10">
            {isMall ? (
              <span className="bg-rose-600 text-white text-[10px] font-black px-1.5 py-0.5 rounded shadow-sm tracking-wide uppercase flex items-center gap-1">
                <ShieldCheck className="w-3 h-3 text-amber-300" />
                <span>MALL</span>
              </span>
            ) : (
              <span className="bg-orange-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded shadow-sm tracking-wide">
                Yêu Thích
              </span>
            )}
            <span className="bg-emerald-600 text-white text-[9px] font-bold px-1.5 py-0.5 rounded shadow-sm uppercase tracking-tight">
              FreeShip Xtra
            </span>
          </div>

          {/* Floating 'Thêm vào giỏ' Quick-Action Pill that slides up smoothly on card hover */}
          <div className="absolute inset-x-2 bottom-2 translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-300 ease-out pointer-events-none z-20">
            <LucianButton
              size="sm"
              variant="primary"
              onClick={handleQuickAdd}
              className="w-full py-2 pointer-events-auto text-xs"
              title="Thêm nhanh vào giỏ hàng (Yêu cầu đăng nhập)"
            >
              <ShoppingCart className="w-3.5 h-3.5 text-white" />
              <span>Thêm vào giỏ</span>
            </LucianButton>
          </div>
        </div>

        {/* Content Info: Minimalist Typography & Meta */}
        <div className="px-3.5 pt-1 pb-3 space-y-1.5">
          <div className="flex items-center justify-between text-[11px] gap-1">
            <span className="text-orange-600 font-semibold truncate text-[11px]">
              {fixMojibakeText(product.categoryName) || 'Sản phẩm chính hãng'}
            </span>
            {product.shopName && (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  if (onSelectShop) {
                    onSelectShop({ id: product.shopId || 1, name: fixMojibakeText(product.shopName) });
                  }
                }}
                className="relative z-10 text-[10px] text-slate-400 hover:text-orange-600 hover:underline truncate max-w-[85px] cursor-pointer bg-transparent border-0 p-0 text-left"
                title={`Xem gian hàng: ${fixMojibakeText(product.shopName)}`}
              >
                {fixMojibakeText(product.shopName)}
              </button>
            )}
          </div>

          <h3 
            className="text-xs sm:text-sm font-semibold text-slate-800 line-clamp-2 leading-relaxed group-hover:text-orange-600 transition-colors" 
            title={fixMojibakeText(product.name)}
          >
            {fixMojibakeText(product.name)}
          </h3>

          {/* Bold JetBrains Mono Price Section */}
          <div className="pt-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-base sm:text-lg font-black font-mono text-orange-600 tracking-tight">
                {formatVND(product.minPrice || 0)}
              </span>
              {originalPrice > 0 && (
                <span className="text-[11px] text-slate-400 font-mono line-through">
                  {formatVND(originalPrice)}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Footer: Rating, Sold Count, Location */}
      <div className="px-3.5 py-2.5 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
        <div className="flex items-center gap-1">
          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
          <span className="font-bold text-slate-800">{rating}</span>
          <span className="text-slate-300">|</span>
          <span>Đã bán {soldCount}</span>
        </div>
        <div className="flex items-center gap-0.5 text-slate-400 text-[10px]">
          <MapPin className="w-3 h-3" />
          <span>Hà Nội</span>
        </div>
      </div>
    </div>
  );
}
