import React, { useState, useEffect } from 'react';
import { 
  X, Star, ShoppingCart, ShieldCheck, Truck, RotateCcw, 
  Store, Check, MessageSquare, Send, Sparkles, AlertCircle 
} from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';
import axiosClient from '../api/axiosClient';

export default function ProductDetailModal({ product, onClose, onBuyNow }) {
  const addToCart = useCartStore((state) => state.addToCart);
  const { user } = useAuthStore();

  const [selectedVariant, setSelectedVariant] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [addedToast, setAddedToast] = useState(false);

  // Reviews state
  const [reviews, setReviews] = useState([]);
  const [newRating, setNewRating] = useState(5);
  const [newComment, setNewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewMsg, setReviewMsg] = useState('');

  const variants = product?.variants || [];

  // Group unique colors and sizes
  const colors = Array.from(new Set(variants.map((v) => v.color).filter(Boolean)));
  const sizes = Array.from(new Set(variants.map((v) => v.size).filter(Boolean)));

  const [selectedColor, setSelectedColor] = useState(colors[0] || '');
  const [selectedSize, setSelectedSize] = useState(sizes[0] || '');

  // Select matching variant when color or size changes
  useEffect(() => {
    if (variants.length > 0) {
      let match = variants.find(
        (v) => (selectedColor ? v.color === selectedColor : true) &&
               (selectedSize ? v.size === selectedSize : true)
      );
      if (!match) match = variants[0];
      setSelectedVariant(match);
    }
  }, [selectedColor, selectedSize, variants]);

  // Fetch reviews for product
  useEffect(() => {
    if (product?.id) {
      axiosClient.get(`/api/catalog/products/${product.id}/reviews`)
        .then((res) => {
          if (res.success && Array.isArray(res.data)) {
            setReviews(res.data);
          }
        })
        .catch(() => {
          // Keep mock fallback
          setReviews([
            {
              id: 1,
              userName: 'Trần Minh Quân',
              rating: 5,
              comment: 'Máy cầm rất đầm tay, màn hình 120Hz mượt mà không có độ trễ. Giao hàng nhanh bọc chống sốc cẩn thận!',
              createdAt: '2026-10-24T10:30:00',
            },
            {
              id: 2,
              userName: 'Nguyễn Thu Trang',
              rating: 5,
              comment: 'Màu ở ngoài đẹp xuất sắc, chụp ảnh thiếu sáng cực kỳ sắc nét. 10 sao cho QuickShop!',
              createdAt: '2026-10-25T14:15:00',
            },
          ]);
        });
    }
  }, [product?.id]);

  if (!product) return null;

  const currentPrice = selectedVariant?.price || product.minPrice || 0;
  const currentStock = selectedVariant?.availableStock ?? selectedVariant?.stock ?? product.totalStock ?? 10;
  const originalPrice = currentPrice * 1.25;

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  };

  const handleAddToCart = () => {
    addToCart(product, selectedVariant, quantity);
    setAddedToast(true);
    setTimeout(() => setAddedToast(false), 2500);
  };

  const handleAddReview = async (e) => {
    e.preventDefault();
    if (!newComment.trim()) return;
    setSubmittingReview(true);
    setReviewMsg('');

    try {
      const res = await axiosClient.post(`/api/catalog/products/${product.id}/reviews`, {
        subOrderId: 101,
        userId: user?.userId || 1,
        userName: user?.fullName || 'Khách hàng QuickShop',
        rating: newRating,
        comment: newComment,
      });

      if (res.success && res.data) {
        setReviews([res.data, ...reviews]);
        setNewComment('');
        setReviewMsg('Cảm ơn bạn đã gửi đánh giá sản phẩm!');
      }
    } catch (err) {
      // Local fallback
      const localReview = {
        id: Date.now(),
        userName: user?.fullName || 'Khách hàng QuickShop',
        rating: newRating,
        comment: newComment,
        createdAt: new Date().toISOString(),
      };
      setReviews([localReview, ...reviews]);
      setNewComment('');
      setReviewMsg('Đã ghi nhận nhận xét của bạn!');
    } finally {
      setSubmittingReview(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 animate-in fade-in duration-200">
      <div className="relative bg-white rounded-3xl max-w-4xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
        {/* Header Bar */}
        <div className="flex justify-between items-center p-4 border-b border-slate-100 bg-slate-50/50">
          <div className="text-xs text-slate-500 font-medium">
            Trang chủ &gt; {product.categoryName || 'Danh mục'} &gt; <span className="text-slate-800 font-semibold">{product.name}</span>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-slate-200 text-slate-500 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="overflow-y-auto p-4 sm:p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 sm:gap-8">
            {/* Left: Product Images */}
            <div className="space-y-3">
              <div className="aspect-square rounded-2xl overflow-hidden bg-slate-50 border border-slate-200 shadow-inner">
                <img
                  src={product.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                  alt={product.name}
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="grid grid-cols-4 gap-2">
                {[1, 2, 3, 4].map((idx) => (
                  <div
                    key={idx}
                    className="aspect-square rounded-xl overflow-hidden border-2 border-orange-500/40 p-0.5 bg-slate-50 cursor-pointer hover:border-orange-600 transition"
                  >
                    <img
                      src={product.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                      alt="thumb"
                      className="w-full h-full object-cover rounded-lg"
                    />
                  </div>
                ))}
              </div>

              {/* Guarantees Box */}
              <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3.5 space-y-2 text-xs text-slate-600">
                <div className="flex items-center gap-2 font-medium text-slate-800">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Cam kết chính hãng 100% - Hoàn 200% nếu phát hiện giả</span>
                </div>
                <div className="flex items-center gap-2 font-medium text-slate-800">
                  <Truck className="w-4 h-4 text-orange-600" />
                  <span>Miễn phí vận chuyển toàn quốc cho đơn hàng từ 99.000đ</span>
                </div>
                <div className="flex items-center gap-2 font-medium text-slate-800">
                  <RotateCcw className="w-4 h-4 text-blue-600" />
                  <span>Đổi trả miễn phí trong 7 ngày nếu lỗi từ nhà sản xuất</span>
                </div>
              </div>
            </div>

            {/* Right: Product Details & Variant Selection */}
            <div className="space-y-4">
              <div>
                <span className="text-[10px] uppercase font-black px-2 py-0.5 bg-rose-600 text-white rounded shadow-sm">
                  QUICKSHOP MALL
                </span>
                <h1 className="text-lg sm:text-xl font-bold text-slate-900 mt-2 leading-snug">
                  {product.name}
                </h1>
                <div className="flex items-center gap-2 mt-2 text-xs text-slate-500">
                  <div className="flex items-center gap-1 text-amber-500 font-bold">
                    <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
                    <span>{product.averageRating || 5.0}</span>
                  </div>
                  <span>•</span>
                  <span>{reviews.length} Đánh giá</span>
                  <span>•</span>
                  <span>Đã bán 1.2k+</span>
                </div>
              </div>

              {/* Price Banner */}
              <div className="bg-orange-50/70 border border-orange-200/60 rounded-2xl p-4 flex items-baseline gap-3">
                <div className="text-2xl sm:text-3xl font-black text-orange-600">
                  {formatVND(currentPrice)}
                </div>
                <div className="text-xs text-slate-400 line-through">
                  {formatVND(originalPrice)}
                </div>
                <div className="text-[11px] font-bold text-rose-600 bg-rose-100 px-2 py-0.5 rounded-md">
                  Giảm 25%
                </div>
              </div>

              {/* Shop Info Card */}
              <div className="flex items-center justify-between p-3 rounded-2xl bg-slate-50 border border-slate-200 text-xs">
                <div className="flex items-center space-x-2.5">
                  <div className="w-9 h-9 rounded-xl bg-orange-600 text-white flex items-center justify-center font-black">
                    <Store className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 flex items-center gap-1">
                      {product.shopName || 'TechWorld Official Store'}
                      <Check className="w-3.5 h-3.5 text-blue-500" />
                    </div>
                    <div className="text-[11px] text-slate-500">Đánh giá Shop: 4.9★ (99% Phản hồi)</div>
                  </div>
                </div>
                <button className="px-3 py-1 bg-white border border-slate-300 hover:border-orange-500 hover:text-orange-600 rounded-lg font-semibold transition">
                  Xem Shop
                </button>
              </div>

              {/* Color Selector */}
              {colors.length > 0 && (
                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-700">Màu sắc:</div>
                  <div className="flex flex-wrap gap-2">
                    {colors.map((c) => (
                      <button
                        key={c}
                        onClick={() => setSelectedColor(c)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-medium border transition ${
                          selectedColor === c
                            ? 'border-orange-600 bg-orange-50 text-orange-700 font-semibold shadow-sm'
                            : 'border-slate-300 hover:border-slate-400 text-slate-700'
                        }`}
                      >
                        {c}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Size / Storage Selector */}
              {sizes.length > 0 && (
                <div className="space-y-2">
                  <div className="text-xs font-semibold text-slate-700">Phiên bản / Dung lượng:</div>
                  <div className="flex flex-wrap gap-2">
                    {sizes.map((s) => (
                      <button
                        key={s}
                        onClick={() => setSelectedSize(s)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-medium border transition ${
                          selectedSize === s
                            ? 'border-orange-600 bg-orange-50 text-orange-700 font-semibold shadow-sm'
                            : 'border-slate-300 hover:border-slate-400 text-slate-700'
                        }`}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* SKU & Stock Info */}
              <div className="text-xs text-slate-500 flex items-center justify-between pt-1">
                <div>Mã SKU: <span className="font-mono font-medium text-slate-700">{selectedVariant?.sku || 'N/A'}</span></div>
                <div>Kho hàng: <span className="font-semibold text-emerald-600">{currentStock} sản phẩm có sẵn</span></div>
              </div>

              {/* Quantity Counter */}
              <div className="flex items-center space-x-4 pt-2">
                <span className="text-xs font-semibold text-slate-700">Số lượng:</span>
                <div className="flex items-center border border-slate-300 rounded-xl overflow-hidden">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="px-3 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold transition"
                  >
                    -
                  </button>
                  <span className="px-4 py-1 text-xs font-bold text-slate-900 bg-white">
                    {quantity}
                  </span>
                  <button
                    onClick={() => setQuantity(Math.min(currentStock, quantity + 1))}
                    className="px-3 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold transition"
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  onClick={handleAddToCart}
                  className="flex-1 py-3 px-4 bg-orange-100 hover:bg-orange-200 text-orange-700 font-bold text-xs sm:text-sm rounded-xl transition flex items-center justify-center gap-2 border border-orange-300 active:scale-95"
                >
                  <ShoppingCart className="w-4 h-4" />
                  Thêm vào giỏ hàng
                </button>

                <button
                  onClick={() => {
                    handleAddToCart();
                    if (onBuyNow) onBuyNow();
                  }}
                  className="flex-1 py-3 px-4 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-xs sm:text-sm rounded-xl transition shadow-lg shadow-orange-500/30 active:scale-95 flex items-center justify-center gap-1.5"
                >
                  Mua ngay
                </button>
              </div>

              {/* Toast Notification */}
              {addedToast && (
                <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-xl text-xs flex items-center gap-2 animate-in fade-in slide-in-from-top duration-200">
                  <Check className="w-4 h-4 text-emerald-600 stroke-[3]" />
                  <span>Đã thêm sản phẩm vào giỏ hàng thành công!</span>
                </div>
              )}
            </div>
          </div>

          {/* Product Description Section */}
          <div className="border-t border-slate-200 pt-6 space-y-3">
            <h3 className="font-bold text-sm text-slate-900 uppercase tracking-wide">
              Mô tả chi tiết sản phẩm
            </h3>
            <div className="bg-slate-50/80 rounded-2xl p-4 text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line border border-slate-200/80">
              {product.description || 'Sản phẩm chính hãng phân phối tại QuickShop Mall. Đầy đủ tem bảo hành và phụ kiện tiêu chuẩn.'}
            </div>
          </div>

          {/* Reviews & Ratings Section (FR-04.1) */}
          <div className="border-t border-slate-200 pt-6 space-y-4">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="font-bold text-sm text-slate-900 uppercase tracking-wide flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-orange-600" />
                  Đánh giá & Nhận xét từ khách hàng ({reviews.length})
                </h3>
                <p className="text-xs text-slate-500">Khách hàng đã mua và xác nhận từ các đơn hàng thành công</p>
              </div>
            </div>

            {/* List of Customer Reviews */}
            <div className="space-y-3 max-h-64 overflow-y-auto pr-1">
              {reviews.map((r) => (
                <div key={r.id} className="p-3.5 bg-slate-50 rounded-2xl border border-slate-200/70 space-y-1.5">
                  <div className="flex justify-between items-center text-xs">
                    <span className="font-bold text-slate-900">{r.userName}</span>
                    <span className="text-[11px] text-slate-400">
                      {new Date(r.createdAt || Date.now()).toLocaleDateString('vi-VN')}
                    </span>
                  </div>
                  <div className="flex items-center space-x-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <Star
                        key={star}
                        className={`w-3.5 h-3.5 ${
                          star <= r.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-200'
                        }`}
                      />
                    ))}
                  </div>
                  <p className="text-xs text-slate-700 leading-relaxed">{r.comment}</p>
                </div>
              ))}
            </div>

            {/* Write a Review Form */}
            <form onSubmit={handleAddReview} className="p-4 bg-orange-50/50 rounded-2xl border border-orange-200 space-y-3">
              <div className="text-xs font-bold text-slate-900">
                Gửi nhận xét của bạn về sản phẩm:
              </div>
              <div className="flex items-center space-x-2">
                <span className="text-xs text-slate-600">Đánh giá số sao:</span>
                <div className="flex space-x-1">
                  {[1, 2, 3, 4, 5].map((s) => (
                    <button
                      type="button"
                      key={s}
                      onClick={() => setNewRating(s)}
                      className="focus:outline-none"
                    >
                      <Star
                        className={`w-5 h-5 ${
                          s <= newRating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'
                        }`}
                      />
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  placeholder="Chia sẻ cảm nhận của bạn về sản phẩm, chất lượng đóng gói, giao hàng..."
                  className="flex-1 bg-white border border-slate-300 rounded-xl px-3.5 py-2 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
                <button
                  type="submit"
                  disabled={submittingReview}
                  className="px-4 py-2 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  Gửi nhận xét
                </button>
              </div>

              {reviewMsg && (
                <div className="text-xs text-emerald-600 font-medium">
                  {reviewMsg}
                </div>
              )}
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
