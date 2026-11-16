import React from 'react';
import { Star, ShoppingCart, MapPin, CheckCircle2 } from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';

export default function ProductCard({ product, onSelect, onRequireAuth, onSelectShop }) {
  const addToCart = useCartStore((state) => state.addToCart);
  const { isAuthenticated } = useAuthStore();

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleQuickAdd = (e) => {
    e.stopPropagation();
    // Bắt buộc đăng nhập trước khi mua hàng hoặc thêm vào giỏ
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
      onClick={() => onSelect(product)}
      className="product-card-item bg-white rounded-3xl border border-slate-200/80 overflow-hidden shadow-xs hover:shadow-xl hover:border-orange-400/80 transition-shadow transition-colors duration-300 cursor-pointer flex flex-col justify-between group relative select-none"
    >
      <div>
        {/* Product Image & Badges: Clean Aspect-[4/5] Rounded Framing */}
        <div className="relative aspect-[4/5] overflow-hidden bg-slate-100/80 m-2 sm:m-2.5 rounded-2xl">
          <img
            src={product.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 pointer-events-none"
            loading="lazy"
          />

          {/* Badges Top Left: Mall or Yêu Thích */}
          <div className="absolute top-2 left-2 flex flex-col gap-1 items-start pointer-events-none">
            {isMall ? (
              <span className="bg-rose-600 text-white text-[10px] font-black px-1.5 py-0.5 rounded shadow-sm tracking-wide uppercase">
                MALL
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

          {/* Floating 'Thêm vào giỏ' Quick-Action Pill that slides up on card hover */}
          <div className="absolute inset-x-2 bottom-2 translate-y-3 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-200 pointer-events-none">
            <button
              onClick={handleQuickAdd}
              className="w-full py-2 bg-slate-900/90 hover:bg-orange-600 backdrop-blur-md text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-lg shadow-slate-950/20 active:scale-95 transition-colors pointer-events-auto cursor-pointer"
              title="Thêm nhanh vào giỏ hàng (Yêu cầu đăng nhập)"
            >
              <ShoppingCart className="w-3.5 h-3.5 text-amber-400" />
              <span>Thêm vào giỏ</span>
            </button>
          </div>
        </div>

        {/* Content Info: Minimalist Typography & Meta */}
        <div className="px-3.5 pt-1 pb-3 space-y-1.5">
          <div className="flex items-center justify-between text-[11px] gap-1">
            <span className="text-orange-600 font-semibold truncate text-[11px]">
              {product.categoryName || 'Sản phẩm chính hãng'}
            </span>
            {product.shopName && (
              <span
                onClick={(e) => {
                  e.stopPropagation();
                  if (onSelectShop) {
                    onSelectShop({ id: product.shopId || 1, name: product.shopName });
                  }
                }}
                className="text-[10px] text-slate-400 hover:text-orange-600 hover:underline truncate max-w-[85px] cursor-pointer"
                title={`Xem gian hàng: ${product.shopName}`}
              >
                {product.shopName}
              </span>
            )}
          </div>

          <h3 
            className="text-xs sm:text-sm font-semibold text-slate-800 line-clamp-2 leading-relaxed group-hover:text-orange-600 transition-colors" 
            title={product.name}
          >
            {product.name}
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
