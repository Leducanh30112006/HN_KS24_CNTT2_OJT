import React, { useState, useEffect } from 'react';
import { 
  Star, ShoppingCart, ShieldCheck, Truck, RotateCcw, Store, Check, 
  MessageSquare, Send, Sparkles, ArrowLeft, Heart, Share2, 
  ChevronRight, BadgeCheck, Zap, Shield, ThumbsUp, CheckCircle2, RefreshCw 
} from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';
import catalogApi from '../api/catalogApi';
import ProductCard from './ProductCard';
import ApiErrorBanner from './ApiErrorBanner';

export default function ProductDetailPage({ 
  product: initialProduct, 
  allProducts = [], 
  onBack, 
  onSelectRelatedProduct, 
  onBuyNow, 
  onRequireAuth,
  onSelectShop
}) {
  const addToCart = useCartStore((state) => state.addToCart);
  const { user, isAuthenticated } = useAuthStore();

  // Dữ liệu sản phẩm thời gian thực lấy từ Catalog Service / Redis Cache
  const [liveProduct, setLiveProduct] = useState(initialProduct);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [detailError, setDetailError] = useState(null);

  const product = liveProduct || initialProduct;

  const [selectedVariant, setSelectedVariant] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [actionMsg, setActionMsg] = useState(null); // { type: 'success' | 'error', text: string }
  const [shareCopied, setShareCopied] = useState(false);
  const [shopNotice, setShopNotice] = useState('');
  const [isFavorited, setIsFavorited] = useState(false);

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
  const [activeImageIndex, setActiveImageIndex] = useState(0);

  // Zoom on hover state
  const [zoomPos, setZoomPos] = useState({ x: 50, y: 50 });
  const [isZoomed, setIsZoomed] = useState(false);

  const handleMouseMove = (e) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    setZoomPos({
      x: Math.max(0, Math.min(100, x)),
      y: Math.max(0, Math.min(100, y)),
    });
  };

  const handleMouseEnter = () => setIsZoomed(true);
  const handleMouseLeave = () => {
    setIsZoomed(false);
    setZoomPos({ x: 50, y: 50 });
  };

  // Đồng bộ lại màu/kích thước mặc định khi chuyển sản phẩm hoặc nạp variants mới từ Catalog
  useEffect(() => {
    if (colors.length > 0 && !colors.includes(selectedColor)) {
      setSelectedColor(colors[0]);
    }
    if (sizes.length > 0 && !sizes.includes(selectedSize)) {
      setSelectedSize(sizes[0]);
    }
  }, [product?.id, variants]);

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

  // Reset quantity and active image when product changes
  useEffect(() => {
    setQuantity(1);
    setActiveImageIndex(0);
    setActionMsg(null);
    setShopNotice('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [initialProduct?.id]);

  // Tự động đồng bộ chi tiết sản phẩm và tồn kho SKU từ API Catalog
  useEffect(() => {
    if (initialProduct) {
      setLiveProduct(initialProduct);
    }
    if (initialProduct?.id) {
      setLoadingDetail(true);
      setDetailError(null);
      catalogApi.getProductById(initialProduct.id)
        .then((res) => {
          if (res && (res.data || res.success)) {
            const data = res.data || res;
            if (data && data.id) {
              setLiveProduct(data);
            }
          }
        })
        .catch((err) => {
          console.warn('Lỗi đồng bộ chi tiết sản phẩm từ Catalog API:', err);
          if (!initialProduct) {
            setDetailError('Không thể tải dữ liệu chi tiết sản phẩm từ Catalog API.');
          }
        })
        .finally(() => {
          setLoadingDetail(false);
        });
    }
  }, [initialProduct?.id]);

  // Fetch reviews từ API Catalog
  useEffect(() => {
    if (product?.id) {
      catalogApi.getProductReviews(product.id)
        .then((res) => {
          if (res && res.success && Array.isArray(res.data) && res.data.length > 0) {
            setReviews(res.data);
          } else {
            setDefaultReviews();
          }
        })
        .catch(() => {
          setDefaultReviews();
        });
    }
  }, [product?.id]);

  const setDefaultReviews = () => {
    setReviews([
      {
        id: 1,
        userName: 'Trần Minh Quân',
        rating: 5,
        comment: 'Sản phẩm nguyên seal chính hãng, cầm đầm tay cực kỳ ưng ý. Đóng gói 3 lớp xốp khí chống va đập, giao hàng chỉ mất 1 ngày!',
        createdAt: '2026-10-28T09:30:00',
        variant: selectedVariant?.sku || 'Bản Tiêu Chuẩn',
      },
      {
        id: 2,
        userName: 'Nguyễn Thu Trang',
        rating: 5,
        comment: 'Màu ở ngoài đẹp hơn cả trong hình, chụp ảnh thiếu sáng sắc nét đỉnh cao. Đã mua lần 2 tại gian hàng này!',
        createdAt: '2026-10-29T14:15:00',
        variant: selectedVariant?.sku || 'Bản Tiêu Chuẩn',
      },
      {
        id: 3,
        userName: 'Hoàng Quốc Việt',
        rating: 5,
        comment: 'Shop tư vấn nhiệt tình, có tem bảo hành điện tử chính hãng kích hoạt ngay khi nhận hàng. 10/10!',
        createdAt: '2026-11-01T16:45:00',
        variant: selectedVariant?.sku || 'Bản Tiêu Chuẩn',
      },
    ]);
  };

  if (!product) return null;

  const currentPrice = selectedVariant?.price || product.minPrice || 0;
  const currentStock = selectedVariant?.availableStock ?? selectedVariant?.stock ?? product.totalStock ?? 10;
  const isOutOfStock = currentStock <= 0;
  const originalPrice = currentPrice ? currentPrice * 1.25 : 0;

  useEffect(() => {
    if (currentStock > 0 && quantity > currentStock) {
      setQuantity(currentStock);
    }
  }, [currentStock]);

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  };

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      if (onRequireAuth) onRequireAuth('Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
      return;
    }
    if (isOutOfStock) {
      setActionMsg({ type: 'error', text: 'Biến thể này hiện đã hết hàng.' });
      return;
    }
    const result = await addToCart(product, selectedVariant, quantity);
    if (result && result.ok === false) {
      setActionMsg({ type: 'error', text: result.error || 'Không thể thêm vào giỏ hàng.' });
      return;
    }
    setActionMsg({ type: 'success', text: `Đã thêm ${quantity} sản phẩm vào giỏ hàng thành công!` });
    setTimeout(() => setActionMsg((prev) => (prev?.type === 'success' ? null : prev)), 3000);
  };

  const handleBuyNow = async () => {
    if (!isAuthenticated) {
      if (onRequireAuth) onRequireAuth('Vui lòng đăng nhập để tiến hành mua hàng ngay!');
      return;
    }
    if (isOutOfStock) {
      setActionMsg({ type: 'error', text: 'Sản phẩm hiện đã hết hàng, không thể đặt mua.' });
      return;
    }
    const resolvedVariantId = selectedVariant?.variantId ?? selectedVariant?.id ?? product.defaultVariantId ?? product.id;
    const result = await addToCart(product, selectedVariant, quantity);
    if (result && result.ok === false) {
      setActionMsg({ type: 'error', text: result.error || 'Không thể tiến hành mua hàng.' });
      return;
    }
    const buyItem = {
      productId: product.id,
      variantId: resolvedVariantId,
      sku: selectedVariant?.sku || `SKU-${product.id}`,
      productName: product.name,
      variantName: selectedVariant?.variantName || ([selectedVariant?.color, selectedVariant?.size].filter(Boolean).join(' - ') || 'Mặc định'),
      color: selectedVariant?.color || '',
      size: selectedVariant?.size || '',
      price: Number(selectedVariant?.price || product.minPrice || 0),
      mainImage: selectedVariant?.imageUrl || product.mainImage,
      quantity,
      availableStock: currentStock,
      shopId: product.shopId || 1,
      shopName: product.shopName || 'TechWorld Official Store',
    };
    if (onBuyNow) onBuyNow(buyItem);
  };

  const handleAddReview = async (e) => {
    if (e) e.preventDefault();
    if (!newComment.trim()) return;
    setSubmittingReview(true);
    setReviewMsg('');

    try {
      const res = await catalogApi.createProductReview(product.id, {
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
      const localReview = {
        id: Date.now(),
        userName: user?.fullName || user?.username || 'Khách hàng QuickShop',
        rating: newRating,
        comment: newComment,
        createdAt: new Date().toISOString(),
        variant: selectedVariant?.sku || 'Bản Tiêu Chuẩn',
      };
      setReviews([localReview, ...reviews]);
      setNewComment('');
      setReviewMsg('Đã ghi nhận nhận xét của bạn!');
    } finally {
      setSubmittingReview(false);
    }
  };

  // Chỉ lấy danh sách ảnh của chính sản phẩm này
  const productImages = React.useMemo(() => {
    if (!product) return [];
    const list = [];
    if (product.mainImage) list.push(product.mainImage);
    if (Array.isArray(product.images)) {
      product.images.forEach((img) => {
        if (img && !list.includes(img)) list.push(img);
      });
    }
    if (Array.isArray(product.variants)) {
      product.variants.forEach((v) => {
        if (v.image && !list.includes(v.image)) list.push(v.image);
      });
    }
    if (list.length === 0) {
      list.push('https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80');
    }
    return list;
  }, [product]);

  // Related products (same category or others)
  const relatedProducts = allProducts
    .filter((p) => p.id !== product.id)
    .slice(0, 5);

  return (
    <div className="space-y-6 pb-12 animate-in fade-in duration-200">
      {/* 1. Breadcrumb & Navigation Bar */}
      <div className="bg-white rounded-2xl border border-slate-200/90 px-4 py-3 flex items-center justify-between shadow-sm text-xs">
        <div className="flex items-center space-x-2 text-slate-500 truncate">
          <button
            onClick={onBack}
            className="flex items-center gap-1 font-bold text-slate-800 hover:text-orange-600 transition pr-2 border-r border-slate-200"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại</span>
          </button>
          <span className="hover:text-orange-600 cursor-pointer" onClick={onBack}>Trang chủ</span>
          <span>&gt;</span>
          <span className="text-slate-700 font-medium">{product.categoryName || 'Danh mục sản phẩm'}</span>
          <span>&gt;</span>
          <span className="text-slate-900 font-semibold truncate max-w-xs">{product.name}</span>
          {loadingDetail && (
            <span className="hidden md:inline-flex items-center gap-1 text-[11px] text-orange-600 bg-orange-50 border border-orange-200/60 px-2 py-0.5 rounded-full font-medium animate-pulse ml-2">
              <RefreshCw className="w-3 h-3 animate-spin text-orange-500" />
              Đang đồng bộ giá & tồn kho API...
            </span>
          )}
        </div>

        <div className="hidden sm:flex items-center space-x-3 text-slate-400">
          <button 
            onClick={() => setIsFavorited(!isFavorited)}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg border transition ${
              isFavorited ? 'border-rose-400 bg-rose-50 text-rose-600 font-bold' : 'border-slate-200 text-slate-600 hover:text-rose-500'
            }`}
          >
            <Heart className={`w-3.5 h-3.5 ${isFavorited ? 'fill-rose-500 text-rose-500' : ''}`} />
            <span>{isFavorited ? 'Đã thích' : 'Yêu thích (1.4k)'}</span>
          </button>
          
          <button 
            onClick={() => {
              if (navigator.clipboard?.writeText && typeof window !== 'undefined') {
                navigator.clipboard.writeText(window.location.href).catch(() => {});
              }
              setShareCopied(true);
              setTimeout(() => setShareCopied(false), 2500);
            }}
            className="flex items-center gap-1 px-2.5 py-1 rounded-lg border border-slate-200 text-slate-600 hover:text-orange-600 transition"
          >
            {shareCopied ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <Share2 className="w-3.5 h-3.5" />}
            <span>{shareCopied ? 'Đã sao chép liên kết' : 'Chia sẻ'}</span>
          </button>
        </div>
      </div>

      {/* 2. Main Product Hero Section (Shopee/TikTok Shop Layout) */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-8 shadow-sm">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Left Column: Product Gallery (5 cols) */}
          <div className="lg:col-span-5 space-y-4">
            {/* Main Showcase Image with Zoom On Hover */}
            <div 
              className="relative aspect-square rounded-2xl overflow-hidden bg-slate-50 border border-slate-200 shadow-inner group cursor-crosshair select-none"
              onMouseMove={handleMouseMove}
              onMouseEnter={handleMouseEnter}
              onMouseLeave={handleMouseLeave}
            >
              <img
                src={productImages[activeImageIndex] || product.mainImage}
                alt={product.name}
                className="w-full h-full object-cover transition-transform duration-150 ease-out pointer-events-none"
                style={{
                  transformOrigin: `${zoomPos.x}% ${zoomPos.y}%`,
                  transform: isZoomed ? 'scale(1.75)' : 'scale(1)',
                }}
                onError={(e) => {
                  e.currentTarget.onerror = null;
                  e.currentTarget.src = product.mainImage;
                }}
              />
              <div className="absolute top-3 left-3 flex flex-col gap-1 items-start pointer-events-none z-10">
                <span className="bg-rose-600 text-white text-[10px] font-black px-2 py-0.5 rounded shadow tracking-wide uppercase">
                  QUICKSHOP MALL
                </span>
                <span className="bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow">
                  Chính Hãng 100%
                </span>
              </div>
              <div className={`absolute bottom-3 right-3 bg-slate-900/60 backdrop-blur-xs text-white text-[10px] font-medium px-2 py-1 rounded-lg pointer-events-none transition-opacity duration-200 z-10 flex items-center gap-1 ${
                isZoomed ? 'opacity-0' : 'opacity-80'
              }`}>
                <span>🔍 Rê chuột để zoom chi tiết</span>
              </div>
            </div>

            {/* Thumbnail Row - CHỈ hiển thị khi sản phẩm có nhiều ảnh thực tế của chính nó */}
            {productImages.length > 1 && (
              <div className="flex gap-2.5 overflow-x-auto pb-1">
                {productImages.map((img, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setActiveImageIndex(idx)}
                    className={`w-16 h-16 flex-shrink-0 rounded-xl overflow-hidden border-2 transition p-0.5 bg-slate-50 cursor-pointer ${
                      activeImageIndex === idx 
                        ? 'border-orange-600 shadow-md ring-2 ring-orange-500/20 scale-95' 
                        : 'border-slate-200 hover:border-orange-300 opacity-80 hover:opacity-100'
                    }`}
                  >
                    <img 
                      src={img} 
                      alt={`Ảnh ${idx + 1}`} 
                      className="w-full h-full object-cover rounded-lg"
                      onError={(e) => {
                        e.currentTarget.onerror = null;
                        e.currentTarget.src = product.mainImage;
                      }}
                    />
                  </button>
                ))}
              </div>
            )}

            {/* Commitments & Buyer Guarantees Box */}
            <div className="bg-slate-50 border border-slate-200/90 rounded-2xl p-4 space-y-2.5 text-xs text-slate-700">
              <div className="font-bold text-slate-900 flex items-center gap-1.5 uppercase text-[11px] tracking-wider pb-1 border-b border-slate-200/80">
                <Shield className="w-4 h-4 text-emerald-600" />
                <span>An tâm mua sắm cùng QuickShop</span>
              </div>
              <div className="flex items-center gap-2">
                <BadgeCheck className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>Hoàn tiền 200% nếu phát hiện hàng giả, hàng nhái</span>
              </div>
              <div className="flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-blue-600 flex-shrink-0" />
                <span>Đổi trả miễn phí tận nhà trong vòng 7 ngày</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-orange-600 flex-shrink-0" />
                <span>Giao hàng hỏa tốc trong 2 giờ tại Hà Nội & TP.HCM</span>
              </div>
            </div>
          </div>

          {/* Right Column: Title, Ratings, Pricing, Variants & Purchase (7 cols) */}
          <div className="lg:col-span-7 space-y-5">
            {/* Title & Badges */}
            <div>
              <div className="flex items-center gap-2">
                <span className="text-[10px] font-black uppercase px-2 py-0.5 bg-rose-600 text-white rounded">
                  Mall
                </span>
                <span className="text-xs font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded-full border border-orange-200">
                  Thương hiệu chính hãng
                </span>
              </div>

              <h1 className="text-xl sm:text-2xl font-black text-slate-900 mt-2 leading-snug">
                {product.name}
              </h1>

              {/* Ratings and Sales Count */}
              <div className="flex flex-wrap items-center gap-3 mt-2.5 text-xs text-slate-500">
                <div className="flex items-center gap-1 text-amber-500 font-bold">
                  <span className="text-slate-900 text-sm font-black border-b border-orange-500">
                    {product.averageRating || 5.0}
                  </span>
                  <div className="flex text-amber-400">
                    {[1, 2, 3, 4, 5].map((i) => (
                      <Star key={i} className="w-3.5 h-3.5 fill-amber-400" />
                    ))}
                  </div>
                </div>
                <span className="opacity-40">|</span>
                <div className="text-slate-700">
                  <strong className="text-slate-900 underline font-bold">{reviews.length}</strong> Đánh giá
                </div>
                <span className="opacity-40">|</span>
                <div className="text-slate-700">
                  <strong className="text-slate-900 font-bold">{product.reviewCount ? `${product.reviewCount * 12}+` : '1.2k+'}</strong> Đã bán
                </div>
              </div>
            </div>

            {/* Shopee Style Flash Price Banner */}
            <div className="bg-gradient-to-r from-orange-50 via-amber-50/60 to-rose-50 border border-orange-200/80 rounded-2xl p-4 sm:p-5 space-y-2">
              <div className="flex items-baseline gap-3 sm:gap-4 flex-wrap">
                <div className="text-2xl sm:text-3xl font-black text-orange-600">
                  {formatVND(currentPrice)}
                </div>
                <div className="text-sm text-slate-400 line-through">
                  {formatVND(originalPrice)}
                </div>
                <div className="text-xs font-black text-rose-600 bg-rose-100 px-2.5 py-0.5 rounded-md uppercase">
                  Giảm 25%
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1 text-[11px] text-orange-700 font-medium">
                <Zap className="w-3.5 h-3.5 fill-orange-500 text-orange-500" />
                <span>Gì cũng rẻ - Cam kết giá tốt nhất thị trường trên QuickShop</span>
              </div>
            </div>

            {/* Shipping & Delivery Box */}
            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-2 text-xs">
              <div className="flex items-start gap-2.5">
                <Truck className="w-4 h-4 text-emerald-600 mt-0.5 flex-shrink-0" />
                <div className="space-y-1">
                  <div className="font-bold text-slate-800 flex items-center gap-2">
                    <span>Vận chuyển toàn quốc</span>
                    <span className="text-[10px] bg-emerald-100 text-emerald-700 px-1.5 py-0.2 rounded font-bold">
                      Miễn phí vận chuyển
                    </span>
                  </div>
                  <div className="text-slate-500">
                    Giao hàng tiêu chuẩn: Nhận hàng dự kiến trong 1 - 2 ngày làm việc.
                  </div>
                </div>
              </div>
            </div>

            {/* Variant Option 1: Colors */}
            {colors.length > 0 && (
              <div className="space-y-2 pt-1">
                <div className="text-xs font-bold text-slate-700 flex items-center justify-between">
                  <span>Màu sắc:</span>
                  <span className="text-orange-600 font-semibold">{selectedColor}</span>
                </div>
                <div className="flex flex-wrap gap-2.5">
                  {colors.map((c) => (
                    <button
                      key={c}
                      onClick={() => setSelectedColor(c)}
                      className={`px-4 py-2 rounded-xl text-xs font-semibold border transition flex items-center gap-1.5 ${
                        selectedColor === c
                          ? 'border-orange-600 bg-orange-50 text-orange-700 shadow-sm ring-2 ring-orange-500/20'
                          : 'border-slate-300 hover:border-slate-400 text-slate-700 bg-white'
                      }`}
                    >
                      {selectedColor === c && <Check className="w-3.5 h-3.5 text-orange-600" />}
                      <span>{c}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Variant Option 2: Sizes / Versions */}
            {sizes.length > 0 && (
              <div className="space-y-2 pt-1">
                <div className="text-xs font-bold text-slate-700 flex items-center justify-between">
                  <span>Phiên bản / Dung lượng:</span>
                  <span className="text-orange-600 font-semibold">{selectedSize}</span>
                </div>
                <div className="flex flex-wrap gap-2.5">
                  {sizes.map((s) => (
                    <button
                      key={s}
                      onClick={() => setSelectedSize(s)}
                      className={`px-4 py-2 rounded-xl text-xs font-semibold border transition flex items-center gap-1.5 ${
                        selectedSize === s
                          ? 'border-orange-600 bg-orange-50 text-orange-700 shadow-sm ring-2 ring-orange-500/20'
                          : 'border-slate-300 hover:border-slate-400 text-slate-700 bg-white'
                      }`}
                    >
                      {selectedSize === s && <Check className="w-3.5 h-3.5 text-orange-600" />}
                      <span>{s}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* SKU and Live Stock Information */}
            <div className="text-xs text-slate-500 flex items-center justify-between py-1 border-y border-slate-100">
              <div>
                Mã biến thể SKU: <span className="font-mono font-bold text-slate-800">{selectedVariant?.sku || 'N/A'}</span>
              </div>
              <div>
                Tình trạng:{' '}
                {isOutOfStock ? (
                  <span className="font-bold text-rose-600">Hết hàng</span>
                ) : (
                  <span className="font-bold text-emerald-600">{currentStock} sản phẩm có sẵn</span>
                )}
              </div>
            </div>

            {/* Quantity Selector */}
            <div className="flex items-center space-x-4 pt-1">
              <span className="text-xs font-bold text-slate-700">Số lượng:</span>
              <div className="flex items-center border border-slate-300 rounded-xl overflow-hidden shadow-inner bg-white">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  disabled={isOutOfStock || quantity <= 1}
                  className="px-3.5 py-1.5 bg-slate-50 hover:bg-slate-200 text-slate-700 font-bold transition active:scale-95 disabled:opacity-40"
                >
                  -
                </button>
                <span className="px-5 py-1.5 text-xs font-bold text-slate-900 text-center min-w-[40px]">
                  {isOutOfStock ? 0 : quantity}
                </span>
                <button
                  onClick={() => setQuantity(Math.min(Math.max(1, currentStock), quantity + 1))}
                  disabled={isOutOfStock || quantity >= currentStock}
                  className="px-3.5 py-1.5 bg-slate-50 hover:bg-slate-200 text-slate-700 font-bold transition active:scale-95 disabled:opacity-40"
                >
                  +
                </button>
              </div>
            </div>

            {/* Action Buttons: Thêm Giỏ Hàng & Mua Ngay */}
            <div className="flex flex-col sm:flex-row gap-3 pt-4">
              <button
                onClick={handleAddToCart}
                disabled={isOutOfStock}
                className="flex-1 py-3.5 px-6 bg-orange-50 hover:bg-orange-100 text-orange-600 font-bold text-sm rounded-2xl transition flex items-center justify-center gap-2 border-2 border-orange-500 shadow-sm active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ShoppingCart className="w-4 h-4 text-orange-600" />
                <span>{isOutOfStock ? 'Tạm Hết Hàng' : 'Thêm Vào Giỏ Hàng'}</span>
              </button>

              <button
                onClick={handleBuyNow}
                disabled={isOutOfStock}
                className="flex-1 py-3.5 px-6 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-extrabold text-sm rounded-2xl transition shadow-lg shadow-orange-500/30 active:scale-95 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span>{isOutOfStock ? 'Hết Hàng' : 'Mua Ngay Với Giá Ưu Đãi'}</span>
              </button>
            </div>

            {/* Action feedback banner */}
            {actionMsg && (
              <div
                className={`p-3 rounded-xl text-xs flex items-center gap-2 animate-in fade-in slide-in-from-top duration-200 border ${
                  actionMsg.type === 'error'
                    ? 'bg-rose-50 border-rose-300 text-rose-800'
                    : 'bg-emerald-50 border-emerald-300 text-emerald-800'
                }`}
              >
                <CheckCircle2
                  className={`w-4 h-4 flex-shrink-0 ${
                    actionMsg.type === 'error' ? 'text-rose-600' : 'text-emerald-600'
                  }`}
                />
                <span className="font-semibold">{actionMsg.text}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 3. Shop Marketplace Info Section (Chuẩn Shopee) */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          {/* Shop Profile Left */}
          <div className="flex items-center space-x-4">
            <div 
              onClick={() => onSelectShop && onSelectShop({ id: product.shopId || 1, name: product.shopName || 'TechWorld Official Store' })}
              className="relative cursor-pointer group"
              title="Xem thông tin gian hàng"
            >
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-orange-500 to-rose-600 text-white font-black flex items-center justify-center text-xl shadow-md group-hover:scale-105 transition-transform">
                <Store className="w-8 h-8" />
              </div>
              <span className="absolute -bottom-1 -right-1 bg-rose-600 text-white text-[9px] font-black px-1 rounded shadow uppercase">
                Mall
              </span>
            </div>

            <div className="space-y-1">
              <div 
                onClick={() => onSelectShop && onSelectShop({ id: product.shopId || 1, name: product.shopName || 'TechWorld Official Store' })}
                className="text-base font-bold text-slate-900 flex items-center gap-1.5 cursor-pointer hover:text-orange-600 transition"
                title="Bấm để xem trang Shop"
              >
                <span>{product.shopName || 'TechWorld Official Store'}</span>
                <Check className="w-4 h-4 text-white bg-blue-500 rounded-full p-0.5" />
              </div>
              <div className="text-xs text-slate-400">
                Đang hoạt động • Phản hồi trong vài phút
              </div>
              <div className="flex gap-2 pt-1 items-center flex-wrap">
                <button 
                  onClick={() => {
                    setShopNotice(`Đã gửi yêu cầu trò chuyện tới ${product.shopName || 'TechWorld Official Store'}!`);
                    setTimeout(() => setShopNotice(''), 3000);
                  }}
                  className="px-3 py-1 bg-orange-50 border border-orange-200 text-orange-600 hover:bg-orange-100 rounded-lg text-xs font-semibold flex items-center gap-1 transition active:scale-95"
                >
                  <MessageSquare className="w-3 h-3" />
                  <span>Chat Ngay</span>
                </button>
                <button 
                  onClick={() => onSelectShop && onSelectShop({ id: product.shopId || 1, name: product.shopName || 'TechWorld Official Store' })}
                  className="px-3 py-1 bg-slate-100 hover:bg-orange-600 hover:text-white text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1 transition active:scale-95 shadow-sm"
                  title="Xem toàn bộ sản phẩm của gian hàng này"
                >
                  <Store className="w-3 h-3" />
                  <span>Xem Shop</span>
                </button>
              </div>
              {shopNotice && (
                <div className="text-[11px] font-medium text-orange-700 bg-orange-50 border border-orange-200 px-2.5 py-1 rounded-lg">
                  {shopNotice}
                </div>
              )}
            </div>
          </div>

          {/* Shop Metrics Right */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs border-t md:border-t-0 md:border-l border-slate-200 pt-4 md:pt-0 md:pl-8 w-full md:w-auto">
            <div>
              <div className="text-slate-400">Đánh giá</div>
              <div className="font-bold text-orange-600 text-sm mt-0.5">4.9 / 5.0 ★</div>
            </div>
            <div>
              <div className="text-slate-400">Sản phẩm</div>
              <div className="font-bold text-slate-900 text-sm mt-0.5">142 sản phẩm</div>
            </div>
            <div>
              <div className="text-slate-400">Tỉ lệ phản hồi</div>
              <div className="font-bold text-emerald-600 text-sm mt-0.5">99%</div>
            </div>
            <div>
              <div className="text-slate-400">Tham gia sàn</div>
              <div className="font-bold text-slate-900 text-sm mt-0.5">2 năm trước</div>
            </div>
          </div>
        </div>
      </div>

      {/* 4. Product Specifications & Detailed Description */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-8 shadow-sm space-y-6">
        <div>
          <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4 pb-2 border-b border-slate-100 flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-orange-600" />
            Chi tiết thông số sản phẩm
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Danh mục:</span>
              <span className="text-slate-800 font-semibold">{product.categoryName || 'Điện tử & Phụ kiện'}</span>
            </div>
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Thương hiệu:</span>
              <span className="text-slate-800 font-semibold">Chính Hãng Phân Phối</span>
            </div>
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Chính sách bảo hành:</span>
              <span className="text-slate-800 font-semibold">12 tháng toàn quốc</span>
            </div>
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Địa chỉ gửi hàng:</span>
              <span className="text-slate-800 font-semibold">Kho Tổng Hoài Đức, Hà Nội</span>
            </div>
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Mã quản lý kho:</span>
              <span className="text-slate-800 font-mono font-semibold">{selectedVariant?.sku || 'SKU-MASTER'}</span>
            </div>
            <div className="flex p-2.5 bg-slate-50 rounded-xl">
              <span className="w-36 text-slate-400 font-medium">Số lượng tồn kho:</span>
              <span className="text-emerald-600 font-semibold">{currentStock} cái sẵn sàng giao</span>
            </div>
          </div>
        </div>

        {/* Detailed Long Description */}
        <div className="pt-2">
          <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-2">
            Mô tả chi tiết sản phẩm
          </h3>
          <div className="bg-slate-50/70 border border-slate-200/80 rounded-2xl p-5 text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line space-y-3">
            <p>{product.description || 'Sản phẩm chính hãng với tiêu chuẩn chất lượng cao nhất tại QuickShop Mall. Đầy đủ hóa đơn, tem chứng nhận và bảo hành chính hãng từ nhà sản xuất.'}</p>
            <p className="font-semibold text-slate-800">Cam kết chất lượng từ QuickShop:</p>
            <ul className="list-disc pl-5 space-y-1 text-slate-600">
              <li>100% sản phẩm có nguồn gốc xuất xứ rõ ràng.</li>
              <li>Kiểm định đóng gói nghiêm ngặt trước khi bàn giao cho đơn vị vận chuyển.</li>
              <li>Hỗ trợ đổi trả trong 7 ngày nếu sản phẩm lỗi kỹ thuật hoặc hư hại do vận chuyển.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* 5. Customer Reviews & Ratings Section (FR-04.1) */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-8 shadow-sm space-y-6">
        <div className="flex justify-between items-center pb-2 border-b border-slate-100">
          <div>
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <MessageSquare className="w-4 h-4 text-orange-600" />
              Đánh giá sản phẩm ({reviews.length})
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Phản hồi thực tế từ những khách hàng đã mua và trải nghiệm sản phẩm
            </p>
          </div>
        </div>

        {/* Ratings Overview Card */}
        <div className="bg-orange-50/60 border border-orange-200/80 rounded-2xl p-5 flex flex-col sm:flex-row items-center gap-6">
          <div className="text-center sm:text-left sm:pr-8 sm:border-r border-orange-200">
            <div className="text-4xl font-black text-orange-600">
              {product.averageRating || 5.0} <span className="text-lg font-bold text-slate-600">/ 5</span>
            </div>
            <div className="flex text-amber-400 my-1 justify-center sm:justify-start">
              {[1, 2, 3, 4, 5].map((s) => (
                <Star key={s} className="w-4 h-4 fill-amber-400" />
              ))}
            </div>
            <div className="text-xs text-slate-500 font-medium">{reviews.length} đánh giá đã xác thực</div>
          </div>

          <div className="flex flex-wrap gap-2 text-xs">
            <button className="px-3 py-1.5 rounded-xl border border-orange-500 bg-white text-orange-600 font-bold shadow-sm">
              Tất cả ({reviews.length})
            </button>
            <button className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:border-orange-300">
              5 Sao ({reviews.filter(r => r.rating === 5).length || reviews.length})
            </button>
            <button className="px-3 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:border-orange-300">
              Có Bình luận ({reviews.length})
            </button>
          </div>
        </div>

        {/* Review Items List */}
        <div className="space-y-4 divide-y divide-slate-100">
          {reviews.map((r) => (
            <div key={r.id} className="pt-4 first:pt-0 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center space-x-2.5">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-orange-400 to-rose-400 text-white font-bold flex items-center justify-center text-xs shadow-inner">
                    {r.userName?.charAt(0)?.toUpperCase() || 'U'}
                  </div>
                  <div>
                    <div className="font-bold text-slate-900 flex items-center gap-1.5">
                      <span>{r.userName}</span>
                      <span className="text-[10px] text-emerald-600 bg-emerald-50 px-1.5 rounded font-normal">
                        ✓ Đã mua hàng
                      </span>
                    </div>
                    <div className="text-[11px] text-slate-400">
                      {new Date(r.createdAt || Date.now()).toLocaleDateString('vi-VN')} • Biến thể: {r.variant || selectedVariant?.sku || 'Tiêu chuẩn'}
                    </div>
                  </div>
                </div>

                <div className="flex text-amber-400">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-3.5 h-3.5 ${
                        star <= r.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-200'
                      }`}
                    />
                  ))}
                </div>
              </div>

              <p className="text-xs sm:text-sm text-slate-700 leading-relaxed pl-10">
                {r.comment}
              </p>
            </div>
          ))}
        </div>

        {/* Add Review Form */}
        <form onSubmit={handleAddReview} className="p-4 sm:p-5 bg-slate-50 rounded-2xl border border-slate-200 space-y-3">
          <div className="text-xs font-bold text-slate-800">
            Viết nhận xét đánh giá cho sản phẩm này:
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-600 font-medium">Chấm điểm sao:</span>
            <div className="flex space-x-1">
              {[1, 2, 3, 4, 5].map((s) => (
                <button
                  type="button"
                  key={s}
                  onClick={() => setNewRating(s)}
                  className="focus:outline-none transition hover:scale-110"
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

          <div className="flex flex-col sm:flex-row gap-2">
            <input
              type="text"
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Nhập cảm nhận của bạn về chất lượng đóng gói, độ hoàn thiện của sản phẩm..."
              className="flex-1 bg-white border border-slate-300 rounded-xl px-4 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
            />
            <button
              type="submit"
              disabled={submittingReview}
              className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-md shadow-orange-500/20 transition disabled:opacity-50"
            >
              <Send className="w-3.5 h-3.5" />
              <span>Gửi đánh giá</span>
            </button>
          </div>

          {reviewMsg && (
            <div className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{reviewMsg}</span>
            </div>
          )}
        </form>
      </div>

      {/* 6. Related Products Carousel / Grid (Gợi ý thêm sản phẩm) */}
      {relatedProducts.length > 0 && (
        <div className="space-y-4 pt-4">
          <div className="flex justify-between items-center">
            <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wide flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-orange-600" />
              Có thể bạn cũng thích (Sản phẩm tương tự)
            </h3>
            <span className="text-xs text-orange-600 font-semibold cursor-pointer hover:underline">
              Xem thêm gợi ý
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-4">
            {relatedProducts.map((p) => (
              <ProductCard
                key={p.id}
                product={p}
                onSelect={(prod) => {
                  if (onSelectRelatedProduct) onSelectRelatedProduct(prod);
                }}
                onRequireAuth={onRequireAuth}
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

