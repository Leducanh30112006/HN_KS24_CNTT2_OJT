import React, { useState } from 'react';
import { ShoppingCart, Eye, Zap, Check, Star } from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';

export default function ProductChatCard({ product, onSelectProduct, onBuyNow, onToast }) {
  const addToCart = useCartStore((state) => state.addToCart);
  const [isAdded, setIsAdded] = useState(false);

  if (!product) return null;

  const currentPrice = product.price ?? product.minPrice ?? product.variants?.[0]?.price ?? 0;
  const originalPrice = product.originalPrice ?? product.maxPrice ?? (currentPrice > 0 ? Math.round(currentPrice * 1.15) : 0);
  const displayImage = product.imageUrl || product.mainImage || product.images?.[0] || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=200';
  const shopName = product.shopName || 'Shop Chính Hãng QuickShop';

  const formatVND = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  const handleAddToCart = async (e) => {
    e.stopPropagation();
    try {
      const firstVariant = product.variants?.[0];
      const defaultVariant = firstVariant
        ? {
            ...firstVariant,
            variantId: firstVariant.id || firstVariant.variantId || (product.id * 10 + 1),
            price: firstVariant.price || currentPrice,
          }
        : {
            variantId: product.id * 10 + 1,
            variantName: 'Mặc định',
            price: currentPrice,
          };

      const res = await addToCart(
        {
          id: product.id,
          name: product.name,
          price: currentPrice,
          mainImage: displayImage,
          shopId: product.shopId || 1,
          shopName: shopName,
        },
        defaultVariant,
        1
      );
      if (res && res.ok !== false) {
        setIsAdded(true);
        if (typeof onToast === 'function') {
          onToast(`Đã thêm "${product.name}" vào giỏ hàng thành công!`);
        }
        setTimeout(() => setIsAdded(false), 2000);
      }
    } catch (err) {
      console.warn('Lỗi thêm sản phẩm vào giỏ từ chat:', err);
    }
  };

  const handleViewDetail = (e) => {
    e.stopPropagation();
    if (onSelectProduct) {
      onSelectProduct(product.id);
    }
  };

  const handleBuyNow = async (e) => {
    e.stopPropagation();
    await handleAddToCart(e);
    if (onBuyNow) {
      onBuyNow({
        productId: product.id,
        productName: product.name,
        price: currentPrice,
        imageUrl: displayImage,
        shopId: product.shopId || 1,
        shopName: shopName,
        quantity: 1,
      });
    }
  };

  return (
    <div className="p-3 rounded-2xl bg-white dark:bg-[#1A1B25] border border-slate-200/90 dark:border-white/10 shadow-sm hover:border-orange-300 dark:hover:border-amber-500/40 transition-all text-left w-full">
      <div className="flex gap-3 items-start">
        <div className="relative w-16 h-16 rounded-xl overflow-hidden bg-slate-100 dark:bg-[#252632] flex-shrink-0 border border-slate-200/80 dark:border-white/10">
          <img
            src={displayImage}
            alt={product.name}
            className="w-full h-full object-cover"
          />
          {product.discountPercent && (
            <span className="absolute top-1 left-1 bg-rose-600 text-white text-[9px] font-black px-1 py-0.5 rounded shadow">
              -{product.discountPercent}%
            </span>
          )}
        </div>

        <div className="flex-1 min-w-0 space-y-1">
          <div className="flex items-center gap-1.5 text-[10px] text-orange-600 dark:text-amber-400 font-bold uppercase tracking-wider">
            <span>{shopName}</span>
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-emerald-500" />
          </div>
          <h4
            onClick={handleViewDetail}
            className="text-xs font-bold text-slate-900 dark:text-[#F4F0E8] line-clamp-2 leading-tight hover:text-orange-600 dark:hover:text-amber-400 cursor-pointer transition"
            title={product.name}
          >
            {product.name}
          </h4>
          <div className="flex items-baseline gap-2 flex-wrap">
            <span className="text-sm font-black text-orange-600 dark:text-amber-400 font-mono">
              {formatVND(currentPrice)}
            </span>
            {originalPrice > currentPrice && (
              <span className="text-[10px] text-slate-400 line-through font-mono">
                {formatVND(originalPrice)}
              </span>
            )}
          </div>
          {product.rating && (
            <div className="flex items-center gap-1 text-[10px] text-amber-500 font-bold">
              <Star className="w-3 h-3 fill-amber-400 text-amber-400" />
              <span>{product.rating}</span>
              {product.sold && <span className="text-slate-400 font-normal">({product.sold} đã bán)</span>}
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons: Thêm vào giỏ, Xem chi tiết, Mua ngay */}
      <div className="grid grid-cols-3 gap-1.5 mt-3 pt-2.5 border-t border-slate-100 dark:border-white/10">
        <button
          type="button"
          onClick={handleAddToCart}
          className={`px-2 py-1.5 text-[11px] font-bold rounded-xl border transition active:scale-95 flex items-center justify-center gap-1 ${
            isAdded
              ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border-emerald-300 dark:border-emerald-500/30'
              : 'bg-orange-50 dark:bg-orange-950/40 hover:bg-orange-100 dark:hover:bg-orange-900/60 text-orange-700 dark:text-amber-300 border-orange-200/80 dark:border-orange-500/30'
          }`}
          title="Thêm vào giỏ hàng"
        >
          {isAdded ? (
            <>
              <Check className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
              <span>Đã thêm</span>
            </>
          ) : (
            <>
              <ShoppingCart className="w-3 h-3 text-orange-600 dark:text-amber-400" />
              <span>Thêm giỏ</span>
            </>
          )}
        </button>

        <button
          type="button"
          onClick={handleViewDetail}
          className="px-2 py-1.5 bg-slate-50 dark:bg-[#252632] hover:bg-slate-100 dark:hover:bg-[#323342] text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white text-[11px] font-bold rounded-xl border border-slate-200 dark:border-white/10 transition active:scale-95 flex items-center justify-center gap-1"
          title="Xem chi tiết sản phẩm"
        >
          <Eye className="w-3 h-3 text-slate-500 dark:text-slate-400" />
          <span>Chi tiết</span>
        </button>

        <button
          type="button"
          onClick={handleBuyNow}
          className="px-2 py-1.5 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white text-[11px] font-bold rounded-xl shadow-sm transition active:scale-95 flex items-center justify-center gap-1"
          title="Mua ngay lập tức"
        >
          <Zap className="w-3 h-3 text-amber-100" />
          <span>Mua ngay</span>
        </button>
      </div>
    </div>
  );
}
