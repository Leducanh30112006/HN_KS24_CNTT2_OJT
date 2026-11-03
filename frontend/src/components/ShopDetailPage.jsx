import React, { useState, useMemo } from 'react';
import { 
  Store, CheckCircle2, Star, MessageSquare, Plus, Check, ShieldCheck, 
  MapPin, Clock, Award, Tag, Sparkles, Filter, ArrowLeft, Search, Ticket, 
  ShoppingBag, ChevronRight, ThumbsUp, Heart
} from 'lucide-react';
import ProductCard from './ProductCard';

export default function ShopDetailPage({ 
  shop, 
  allProducts = [], 
  onBack, 
  onSelectProduct,
  onRequireAuth
}) {
  const [isFollowing, setIsFollowing] = useState(false);
  const [followerCount, setFollowerCount] = useState(48520);
  const [savedVouchers, setSavedVouchers] = useState({});
  const [activeTab, setActiveTab] = useState('all'); // 'all' | 'best' | 'newest' | 'priceAsc' | 'priceDesc'
  const [shopSearchKeyword, setShopSearchKeyword] = useState('');

  // Profiles for shops
  const shopProfiles = {
    1: {
      name: 'TechWorld Official Store',
      avatar: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200&auto=format&fit=crop&q=80',
      coverImage: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200&auto=format&fit=crop&q=80',
      rating: 4.9,
      reviewCount: '28.6k',
      responseRate: '99%',
      responseTime: 'trong vài phút',
      joinedTime: '3 năm trước',
      location: 'Hà Nội, Việt Nam',
      isMall: true,
    },
    2: {
      name: 'Fashion Hub Official',
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80',
      coverImage: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=1200&auto=format&fit=crop&q=80',
      rating: 4.8,
      reviewCount: '15.4k',
      responseRate: '98%',
      responseTime: 'trong 1 giờ',
      joinedTime: '2 năm trước',
      location: 'TP. Hồ Chí Minh',
      isMall: true,
    },
    3: {
      name: 'SmartHome Vietnam Store',
      avatar: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=200&auto=format&fit=crop&q=80',
      coverImage: 'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=1200&auto=format&fit=crop&q=80',
      rating: 4.9,
      reviewCount: '9.8k',
      responseRate: '99%',
      responseTime: 'trong vài phút',
      joinedTime: '1 năm trước',
      location: 'Hà Nội, Việt Nam',
      isMall: true,
    },
    4: {
      name: 'BeautyCare Mall',
      avatar: 'https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?w=200&auto=format&fit=crop&q=80',
      coverImage: 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=1200&auto=format&fit=crop&q=80',
      rating: 4.9,
      reviewCount: '34.2k',
      responseRate: '100%',
      responseTime: 'trong vài phút',
      joinedTime: '4 năm trước',
      location: 'TP. Hồ Chí Minh',
      isMall: true,
    },
    5: {
      name: 'Sneaker World',
      avatar: 'https://images.unsplash.com/photo-1552346154-21d32810aba3?w=200&auto=format&fit=crop&q=80',
      coverImage: 'https://images.unsplash.com/photo-1556906781-9a412961c28c?w=1200&auto=format&fit=crop&q=80',
      rating: 4.9,
      reviewCount: '12.3k',
      responseRate: '97%',
      responseTime: 'trong 1 giờ',
      joinedTime: '2 năm trước',
      location: 'Đà Nẵng, Việt Nam',
      isMall: true,
    },
  };

  const matchedProfile = (shop?.id && shopProfiles[shop.id]) ||
    Object.values(shopProfiles).find(p => p.name === (shop?.name || shop?.shopName)) ||
    shopProfiles[1];

  const shopInfo = {
    id: shop?.id || matchedProfile.id || 1,
    name: shop?.name || shop?.shopName || matchedProfile.name,
    avatar: shop?.avatar || matchedProfile.avatar,
    coverImage: shop?.coverImage || matchedProfile.coverImage,
    rating: shop?.rating || matchedProfile.rating,
    reviewCount: shop?.reviewCount || matchedProfile.reviewCount,
    responseRate: matchedProfile.responseRate,
    responseTime: matchedProfile.responseTime,
    joinedTime: matchedProfile.joinedTime,
    location: shop?.location || matchedProfile.location,
    isMall: true,
  };

  const handleToggleFollow = () => {
    if (isFollowing) {
      setIsFollowing(false);
      setFollowerCount((prev) => prev - 1);
    } else {
      setIsFollowing(true);
      setFollowerCount((prev) => prev + 1);
    }
  };

  const handleSaveVoucher = (code) => {
    setSavedVouchers((prev) => ({
      ...prev,
      [code]: true,
    }));
  };

  // Find products belonging to this shop
  const shopProducts = useMemo(() => {
    const rawMatches = allProducts.filter((p) => {
      if (shop?.id && p.shopId === shop.id) return true;
      if (shop?.name && p.shopName === shop.name) return true;
      return false;
    });

    // If there are few products in sample DB for this shop, supplement with relevant shop items
    if (rawMatches.length < 1) {
      const extraItems = allProducts.slice(0, 6).map((p, index) => ({
        ...p,
        id: p.id * 100 + index,
        shopId: shopInfo.id,
        shopName: shopInfo.name,
      }));
      return rawMatches.length > 0 ? rawMatches : extraItems;
    }

    return rawMatches;
  }, [allProducts, shop, shopInfo]);

  // Filter & sort products
  const displayProducts = useMemo(() => {
    let list = [...shopProducts];

    if (shopSearchKeyword.trim()) {
      const kw = shopSearchKeyword.toLowerCase().trim();
      list = list.filter((p) => p.name.toLowerCase().includes(kw));
    }

    if (activeTab === 'newest') {
      list.sort((a, b) => b.id - a.id);
    } else if (activeTab === 'priceAsc') {
      list.sort((a, b) => (a.minPrice || 0) - (b.minPrice || 0));
    } else if (activeTab === 'priceDesc') {
      list.sort((a, b) => (b.minPrice || 0) - (a.minPrice || 0));
    } else if (activeTab === 'best') {
      list.sort((a, b) => (b.reviewCount || 0) - (a.reviewCount || 0));
    }

    return list;
  }, [shopProducts, shopSearchKeyword, activeTab]);

  const shopVouchers = [
    { code: 'SHOP20K', discount: 'Giảm 20.000₫', minOrder: 'Đơn tối thiểu 200k', exp: '30/11/2026' },
    { code: 'SHOP50K', discount: 'Giảm 50.000₫', minOrder: 'Đơn tối thiểu 500k', exp: '30/11/2026' },
    { code: 'FREESHIP', discount: 'Miễn phí vận chuyển', minOrder: 'Đơn tối thiểu 150k', exp: '30/11/2026' },
  ];

  return (
    <div className="space-y-6 pb-12 animate-in fade-in duration-300">
      {/* 1. Header Back Navigation */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <button
          onClick={onBack}
          className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-bold active:scale-95 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại</span>
        </button>

        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <span className="text-slate-400">Trang chủ</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-slate-400">Nhà bán hàng</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-orange-600 font-bold">{shopInfo.name}</span>
        </div>
      </div>

      {/* 2. Shop Hero Profile Banner (Phong cách Shopee & TikTok Shop Mall) */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
        {/* Cover Image */}
        <div className="h-36 sm:h-48 w-full relative bg-gradient-to-r from-orange-600 via-rose-600 to-amber-600 overflow-hidden">
          <div className="absolute inset-0 bg-black/30 backdrop-blur-[2px]"></div>
          <div className="absolute top-4 right-4 flex items-center gap-2 text-white/90 text-xs font-semibold bg-black/40 px-3 py-1.5 rounded-full backdrop-blur-md">
            <MapPin className="w-3.5 h-3.5 text-amber-300" />
            <span>{shopInfo.location}</span>
          </div>
        </div>

        {/* Shop Info & Action Row */}
        <div className="p-5 sm:p-6 relative -mt-16 sm:-mt-20 flex flex-col lg:flex-row justify-between items-start lg:items-end gap-6">
          {/* Avatar and Name */}
          <div className="flex flex-col sm:flex-row items-start sm:items-end gap-4">
            <div className="relative">
              <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-3xl bg-white p-1.5 shadow-xl border-2 border-white overflow-hidden">
                <div className="w-full h-full rounded-2xl bg-gradient-to-tr from-orange-500 to-rose-600 text-white flex items-center justify-center font-black text-3xl shadow-inner">
                  <Store className="w-12 h-12" />
                </div>
              </div>
              {shopInfo.isMall && (
                <span className="absolute bottom-1 right-1 bg-rose-600 text-white text-[10px] font-black px-2 py-0.5 rounded-md shadow-md uppercase tracking-wider">
                  MALL
                </span>
              )}
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-xl sm:text-2xl font-black text-slate-900">
                  {shopInfo.name}
                </h1>
                <div className="w-5 h-5 rounded-full bg-blue-500 text-white flex items-center justify-center shadow-sm" title="Gian hàng chính hãng đã xác minh">
                  <Check className="w-3.5 h-3.5 stroke-[3]" />
                </div>
              </div>

              <div className="flex items-center gap-3 text-xs text-slate-500">
                <span className="flex items-center gap-1 text-emerald-600 font-semibold">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  Online 5 phút trước
                </span>
                <span>•</span>
                <span>Tham gia: {shopInfo.joinedTime}</span>
              </div>
            </div>
          </div>

          {/* Action Buttons: Theo Dõi & Chat Ngay */}
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <button
              onClick={handleToggleFollow}
              className={`flex-1 sm:flex-none px-6 py-2.5 rounded-2xl text-xs font-bold transition flex items-center justify-center gap-2 shadow-sm active:scale-95 ${
                isFollowing
                  ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300'
                  : 'bg-orange-600 hover:bg-orange-700 text-white shadow-orange-500/20'
              }`}
            >
              {isFollowing ? (
                <>
                  <Check className="w-4 h-4 text-emerald-600" />
                  <span>Đang theo dõi</span>
                </>
              ) : (
                <>
                  <Plus className="w-4 h-4" />
                  <span>Theo dõi Shop ({followerCount.toLocaleString('vi-VN')})</span>
                </>
              )}
            </button>

            <button
              onClick={() => alert(`Đang kết nối trung tâm tin nhắn trực tiếp với ${shopInfo.name}...`)}
              className="flex-1 sm:flex-none px-6 py-2.5 rounded-2xl bg-orange-50 hover:bg-orange-100 text-orange-600 border border-orange-200 text-xs font-bold transition flex items-center justify-center gap-2 active:scale-95"
            >
              <MessageSquare className="w-4 h-4" />
              <span>Chat Ngay</span>
            </button>
          </div>
        </div>

        {/* Shop Metrics Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-5 sm:p-6 bg-slate-50/70 border-t border-slate-100 text-xs">
          <div className="space-y-0.5">
            <div className="text-slate-400 font-medium">Đánh giá Shop</div>
            <div className="text-sm sm:text-base font-black text-orange-600 flex items-center gap-1">
              <Star className="w-4 h-4 fill-amber-400 text-amber-400" />
              <span>{shopInfo.rating} / 5.0</span>
              <span className="text-xs text-slate-400 font-normal">({shopInfo.reviewCount})</span>
            </div>
          </div>

          <div className="space-y-0.5">
            <div className="text-slate-400 font-medium">Sản phẩm gian hàng</div>
            <div className="text-sm sm:text-base font-black text-slate-800">
              {shopProducts.length} sản phẩm
            </div>
          </div>

          <div className="space-y-0.5">
            <div className="text-slate-400 font-medium">Tỉ lệ phản hồi Chat</div>
            <div className="text-sm sm:text-base font-black text-emerald-600">
              {shopInfo.responseRate} <span className="text-xs text-slate-400 font-normal">({shopInfo.responseTime})</span>
            </div>
          </div>

          <div className="space-y-0.5">
            <div className="text-slate-400 font-medium">Người theo dõi</div>
            <div className="text-sm sm:text-base font-black text-slate-800">
              {followerCount.toLocaleString('vi-VN')} người
            </div>
          </div>
        </div>
      </div>

      {/* 3. Shop Vouchers / Coupons Bar */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-5 shadow-sm space-y-3">
        <div className="flex items-center gap-2 text-sm font-black text-slate-900 uppercase tracking-wide">
          <Ticket className="w-4 h-4 text-orange-600" />
          <span>Mã Giảm Giá Của Shop</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {shopVouchers.map((v) => {
            const isSaved = savedVouchers[v.code];
            return (
              <div
                key={v.code}
                className="p-3.5 rounded-2xl border border-dashed border-orange-300 bg-orange-50/60 flex items-center justify-between gap-3 relative overflow-hidden"
              >
                <div className="space-y-1">
                  <div className="text-xs font-black text-orange-700">{v.discount}</div>
                  <div className="text-[11px] text-slate-600 font-medium">{v.minOrder}</div>
                  <div className="text-[10px] text-slate-400">HSD: {v.exp}</div>
                </div>

                <button
                  onClick={() => handleSaveVoucher(v.code)}
                  disabled={isSaved}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition shadow-sm ${
                    isSaved
                      ? 'bg-slate-200 text-slate-500 cursor-default'
                      : 'bg-orange-600 hover:bg-orange-700 text-white active:scale-95'
                  }`}
                >
                  {isSaved ? 'Đã Lưu' : 'Lưu Mã'}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* 4. Products Filter & Search Inside Shop */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-4 sm:p-5 shadow-sm space-y-4">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
          {/* Filter Tabs */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1 w-full sm:w-auto scrollbar-none text-xs">
            <button
              onClick={() => setActiveTab('all')}
              className={`px-4 py-2 rounded-xl font-bold transition whitespace-nowrap ${
                activeTab === 'all'
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              Tất cả sản phẩm
            </button>
            <button
              onClick={() => setActiveTab('best')}
              className={`px-4 py-2 rounded-xl font-bold transition whitespace-nowrap ${
                activeTab === 'best'
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              Bán chạy nhất
            </button>
            <button
              onClick={() => setActiveTab('newest')}
              className={`px-4 py-2 rounded-xl font-bold transition whitespace-nowrap ${
                activeTab === 'newest'
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              Hàng mới về
            </button>
            <button
              onClick={() => setActiveTab('priceAsc')}
              className={`px-4 py-2 rounded-xl font-bold transition whitespace-nowrap ${
                activeTab === 'priceAsc'
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              Giá thấp đến cao
            </button>
            <button
              onClick={() => setActiveTab('priceDesc')}
              className={`px-4 py-2 rounded-xl font-bold transition whitespace-nowrap ${
                activeTab === 'priceDesc'
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
              }`}
            >
              Giá cao đến thấp
            </button>
          </div>

          {/* Search inside shop */}
          <div className="relative w-full sm:w-64">
            <input
              type="text"
              value={shopSearchKeyword}
              onChange={(e) => setShopSearchKeyword(e.target.value)}
              placeholder="Tìm kiếm tại Shop..."
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-orange-500 focus:bg-white transition"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
          </div>
        </div>

        <div className="text-xs text-slate-400 font-medium">
          Hiển thị <strong className="text-slate-800">{displayProducts.length}</strong> sản phẩm thuộc gian hàng <strong className="text-orange-600">{shopInfo.name}</strong>
        </div>
      </div>

      {/* 5. Shop Products Catalog Grid */}
      {displayProducts.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-slate-200/90 space-y-3">
          <div className="w-16 h-16 rounded-full bg-orange-50 text-orange-400 mx-auto flex items-center justify-center">
            <ShoppingBag className="w-8 h-8" />
          </div>
          <h3 className="font-bold text-sm text-slate-800">Không tìm thấy sản phẩm phù hợp</h3>
          <p className="text-xs text-slate-400">
            Không có sản phẩm nào khớp với tìm kiếm trong shop này.
          </p>
          <button
            onClick={() => {
              setShopSearchKeyword('');
              setActiveTab('all');
            }}
            className="px-4 py-2 bg-orange-600 text-white text-xs font-semibold rounded-xl hover:bg-orange-700 transition"
          >
            Xem tất cả sản phẩm của Shop
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4">
          {displayProducts.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              onSelect={onSelectProduct}
              onRequireAuth={onRequireAuth}
            />
          ))}
        </div>
      )}
    </div>
  );
}

