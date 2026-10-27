import React from 'react';
import { Star, ShoppingCart, MapPin, CheckCircle2 } from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';

export default function ProductCard({ product, onSelect }) {
  const addToCart = useCartStore((state) => state.addToCart);

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleQuickAdd = (e) => {
    e.stopPropagation();
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
      className="bg-white rounded-2xl border border-slate-200/90 overflow-hidden shadow-sm hover:shadow-xl hover:border-orange-400 transition-all duration-200 cursor-pointer flex flex-col justify-between group"
    >
      <div>
        {/* Product Image & Badges */}
        <div className="relative aspect-square overflow-hidden bg-slate-50">
          <img
            src={product.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
          />

          {/* Badges Top Left: Mall or Yêu Thích */}
          <div className="absolute top-2 left-2 flex flex-col gap-1 items-start">
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

          {/* Quick Add Button overlay */}
          <button
            onClick={handleQuickAdd}
            className="absolute bottom-2 right-2 w-9 h-9 rounded-xl bg-orange-600 hover:bg-orange-700 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-all shadow-lg shadow-orange-600/30 active:scale-90"
            title="Thêm nhanh vào giỏ hàng"
          >
            <ShoppingCart className="w-4 h-4" />
          </button>
        </div>

        {/* Content Info */}
        <div className="p-3 space-y-1.5">
          <div className="text-[11px] text-orange-600 font-semibold truncate">
            {product.categoryName || 'Sản phẩm chính hãng'}
          </div>

          <h3 
            className="text-xs font-medium text-slate-800 line-clamp-2 leading-relaxed group-hover:text-orange-600 transition" 
            title={product.name}
          >
            {product.name}
          </h3>

          {/* Price Section */}
          <div className="pt-1">
            <div className="flex items-baseline gap-1.5">
              <span className="text-sm sm:text-base font-bold text-orange-600">
                {formatVND(product.minPrice || 0)}
              </span>
              {originalPrice > 0 && (
                <span className="text-[10px] text-slate-400 line-through">
                  {formatVND(originalPrice)}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Footer: Rating, Sold Count, Location */}
      <div className="p-3 pt-0 border-t border-slate-100 mt-2 flex items-center justify-between text-[11px] text-slate-500">
        <div className="flex items-center gap-1">
          <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
          <span className="font-semibold text-slate-700">{rating}</span>
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
