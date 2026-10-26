import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import HeroBanner from './components/HeroBanner';
import FlashSaleSection from './components/FlashSaleSection';
import ProductCard from './components/ProductCard';
import ProductDetailModal from './components/ProductDetailModal';
import CartDrawer from './components/CartDrawer';
import AiChatWidget from './components/AiChatWidget';
import SellerPortal from './components/SellerPortal';
import AdminPortal from './components/AdminPortal';
import AuthModal from './components/AuthModal';
import axiosClient from './api/axiosClient';
import { 
  Filter, RotateCcw, Star, ChevronRight, ShieldCheck, 
  HelpCircle, Smartphone, Award, Truck, Sparkles 
} from 'lucide-react';

export default function App() {
  const [currentView, setCurrentView] = useState('storefront'); // 'storefront' | 'seller' | 'admin'

  // Data states
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);

  // Filter states
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [sortBy, setSortBy] = useState('newest'); // 'newest' | 'priceAsc' | 'priceDesc'
  const [priceRange, setPriceRange] = useState('all'); // 'all', 'under1m', '1m-5m', '5m-20m', 'above20m'
  const [minRating, setMinRating] = useState(0);

  // Modal states
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  // Default seed data for immediate standalone presentation
  const defaultCategories = [
    { id: 1, name: 'Điện thoại & Phụ kiện', slug: 'dien-thoai-phu-kien', icon: 'Smartphone', productCount: 3 },
    { id: 2, name: 'Laptop & Máy tính', slug: 'laptop-may-tinh', icon: 'Laptop', productCount: 2 },
    { id: 3, name: 'Thời trang nam nữ', slug: 'thoi-trang', icon: 'Shirt', productCount: 2 },
    { id: 4, name: 'Nhà cửa & Đời sống', slug: 'nha-cua-doi-song', icon: 'Home', productCount: 2 },
    { id: 5, name: 'Mỹ phẩm & Làm đẹp', slug: 'my-pham-lam-dep', icon: 'Sparkles', productCount: 1 },
    { id: 6, name: 'Giày dép & Túi xách', slug: 'giay-dep-tui-xach', icon: 'ShoppingBag', productCount: 1 },
  ];

  const defaultProducts = [
    {
      id: 1,
      shopId: 1,
      shopName: 'TechWorld Official Store',
      categoryId: 1,
      categoryName: 'Điện thoại & Phụ kiện',
      name: 'Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế',
      description: 'Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình Super Retina XDR OLED 120Hz siêu mượt mà, cụm 3 camera 48MP zoom quang học 5x và thời lượng pin trâu lên đến 29 giờ liên tục.',
      mainImage: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 29990000,
      maxPrice: 41990000,
      totalStock: 85,
      averageRating: 5.0,
      reviewCount: 48,
      variants: [
        { id: 11, sku: 'QP-PRO-TITAN-256', color: 'Titan Tự Nhiên', size: '256GB', price: 29990000, stock: 45, availableStock: 45 },
        { id: 12, sku: 'QP-PRO-BLACK-512', color: 'Đen Không Gian', size: '512GB', price: 34990000, stock: 25, availableStock: 25 },
        { id: 13, sku: 'QP-PRO-DESERT-1TB', color: 'Titan Sa Mạc', size: '1TB', price: 41990000, stock: 15, availableStock: 15 },
      ],
    },
    {
      id: 2,
      shopId: 1,
      shopName: 'TechWorld Official Store',
      categoryId: 2,
      categoryName: 'Laptop & Máy tính',
      name: 'Laptop QuickBook Ultra 14 inch M3 Pro - 18GB/512GB Bạc Ánh Kim',
      description: 'Chiếc máy trạm di động mỏng nhẹ đẳng cấp nhất: màn hình Liquid Retina XDR 1000 nits, chip 11-core CPU, 14-core GPU xử lý đồ họa mượt mà, pin dùng cả ngày không cần sạc.',
      mainImage: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 45990000,
      maxPrice: 57990000,
      totalStock: 30,
      averageRating: 4.9,
      reviewCount: 32,
      variants: [
        { id: 21, sku: 'QB-ULTRA-SPACEBLACK-18-512', color: 'Đen Không Gian', size: '18GB / 512GB', price: 45990000, stock: 20, availableStock: 20 },
        { id: 22, sku: 'QB-ULTRA-SILVER-36-1TB', color: 'Bạc Ánh Kim', size: '36GB / 1TB', price: 57990000, stock: 10, availableStock: 10 },
      ],
    },
    {
      id: 3,
      shopId: 1,
      shopName: 'TechWorld Official Store',
      categoryId: 1,
      categoryName: 'Điện thoại & Phụ kiện',
      name: 'Tai nghe Bluetooth True Wireless QuickPods Pro 2 Chống Ồn Chủ Động',
      description: 'Âm thanh vòm cá nhân hóa chuẩn phòng thu, công nghệ khử tiếng ồn chủ động ANC thế hệ 2 gấp đôi hiệu quả, kháng nước IP54 và hộp sạc hỗ trợ sạc MagSafe không dây.',
      mainImage: 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 4990000,
      maxPrice: 4990000,
      totalStock: 80,
      averageRating: 5.0,
      reviewCount: 96,
      variants: [
        { id: 31, sku: 'QPODS-PRO-WHITE', color: 'Trắng Tinh Khôi', size: 'Tiêu chuẩn', price: 4990000, stock: 80, availableStock: 80 },
      ],
    },
    {
      id: 4,
      shopId: 2,
      shopName: 'Fashion Hub Official',
      categoryId: 3,
      categoryName: 'Thời trang nam nữ',
      name: 'Áo Polo Nam QuickStyle Premium Vải Cafe Khử Mùi Thoáng Khí',
      description: 'Chất liệu dệt sợi từ bã cafe tự nhiên siêu nhẹ, chống tia UV 50+, co giãn 4 chiều vận động thoải mái và chống nhăn tự nhiên không cần ủi.',
      mainImage: 'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 289000,
      maxPrice: 289000,
      totalStock: 255,
      averageRating: 4.8,
      reviewCount: 140,
      variants: [
        { id: 41, sku: 'POLO-NAVY-M', color: 'Xanh Navy', size: 'M', price: 289000, stock: 100, availableStock: 100 },
        { id: 42, sku: 'POLO-NAVY-L', color: 'Xanh Navy', size: 'L', price: 289000, stock: 85, availableStock: 85 },
        { id: 43, sku: 'POLO-WHITE-M', color: 'Trắng Sữa', size: 'M', price: 289000, stock: 70, availableStock: 70 },
      ],
    },
    {
      id: 5,
      shopId: 2,
      shopName: 'SmartHome Vietnam Store',
      categoryId: 4,
      categoryName: 'Nhà cửa & Đời sống',
      name: 'Robot Hút Bụi Lau Nhà Tự Động QuickBot Omni 3000Pa Cảm Biến LiDAR',
      description: 'Hệ thống định vị laser LDS 360 độ lập bản đồ 3D cực chuẩn, lực hút bão 3000Pa gom sạch lông thú cưng, tự động giặt giẻ lau sấy khí nóng diệt khuẩn 99.9%.',
      mainImage: 'https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 8490000,
      maxPrice: 8490000,
      totalStock: 30,
      averageRating: 4.9,
      reviewCount: 22,
      variants: [
        { id: 51, sku: 'ROBOT-OMNI-WHITE', color: 'Trắng Tuyết', size: 'Bản Quốc Tế', price: 8490000, stock: 30, availableStock: 30 },
      ],
    },
    {
      id: 6,
      shopId: 3,
      shopName: 'BeautyCare Mall',
      categoryId: 5,
      categoryName: 'Mỹ phẩm & Làm đẹp',
      name: 'Tinh Chất Serum Niacinamide 10% + Zinc 1% Dưỡng Sáng Đều Màu Da',
      description: 'Công thức lành tính dịu nhẹ kiểm soát bã nhờn, làm thông thoáng lỗ chân lông, làm mờ vết thâm mụn chỉ sau 14 ngày sử dụng đều đặn.',
      mainImage: 'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 199000,
      maxPrice: 349000,
      totalStock: 240,
      averageRating: 4.7,
      reviewCount: 65,
      variants: [
        { id: 61, sku: 'SERUM-30ML', color: 'Trong suốt', size: '30ml', price: 199000, stock: 150, availableStock: 150 },
        { id: 62, sku: 'SERUM-60ML', color: 'Trong suốt', size: '60ml', price: 349000, stock: 90, availableStock: 90 },
      ],
    },
    {
      id: 7,
      shopId: 3,
      shopName: 'Sneaker World',
      categoryId: 6,
      categoryName: 'Giày dép & Túi xách',
      name: 'Giày Sneaker Thể Thao QuickRunner Pro Đệm Khí Êm Ái Chống Trượt',
      description: 'Đế đệm khí êm nhẹ trợ lực khi chạy bộ, thân giày dệt lưới Flyknit thoáng khí kháng khuẩn không lo hôi chân, thiết kế trẻ trung phong cách Hàn Quốc.',
      mainImage: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80',
      status: 'ACTIVE',
      minPrice: 890000,
      maxPrice: 890000,
      totalStock: 120,
      averageRating: 5.0,
      reviewCount: 54,
      variants: [
        { id: 71, sku: 'SNEAKER-RED-40', color: 'Đỏ Rực Rỡ', size: '40', price: 890000, stock: 40, availableStock: 40 },
        { id: 72, sku: 'SNEAKER-RED-41', color: 'Đỏ Rực Rỡ', size: '41', price: 890000, stock: 45, availableStock: 45 },
        { id: 73, sku: 'SNEAKER-RED-42', color: 'Đỏ Rực Rỡ', size: '42', price: 890000, stock: 35, availableStock: 35 },
      ],
    },
  ];

  // Fetch Categories from /api/catalog/categories
  const fetchCategories = async () => {
    try {
      const res = await axiosClient.get('/api/catalog/categories');
      if (res.success && Array.isArray(res.data) && res.data.length > 0) {
        setCategories(res.data);
      } else {
        setCategories(defaultCategories);
      }
    } catch (err) {
      setCategories(defaultCategories);
    }
  };

  // Fetch Products from /api/catalog/products
  const fetchProducts = async (catId = selectedCategory, kw = searchKeyword, sort = sortBy) => {
    setLoadingProducts(true);
    let url = `/api/catalog/products?status=ACTIVE&sortBy=${sort}&size=50`;
    if (catId) url += `&categoryId=${catId}`;
    if (kw && kw.trim()) url += `&keyword=${encodeURIComponent(kw.trim())}`;

    try {
      const res = await axiosClient.get(url);
      if (res.success && res.data?.content && res.data.content.length > 0) {
        setProducts(res.data.content);
      } else {
        // Filter local default
        let filtered = defaultProducts;
        if (catId) filtered = filtered.filter((p) => p.categoryId === catId);
        if (kw && kw.trim()) {
          const lkw = kw.toLowerCase().trim();
          filtered = filtered.filter((p) => p.name.toLowerCase().includes(lkw) || p.description.toLowerCase().includes(lkw));
        }
        setProducts(filtered);
      }
    } catch (err) {
      let filtered = defaultProducts;
      if (catId) filtered = filtered.filter((p) => p.categoryId === catId);
      if (kw && kw.trim()) {
        const lkw = kw.toLowerCase().trim();
        filtered = filtered.filter((p) => p.name.toLowerCase().includes(lkw) || p.description.toLowerCase().includes(lkw));
      }
      setProducts(filtered);
    } finally {
      setLoadingProducts(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchProducts(selectedCategory, searchKeyword, sortBy);
  }, [selectedCategory, sortBy]);

  const handleSearchSubmit = (keywordOverride) => {
    const kw = keywordOverride !== undefined ? keywordOverride : searchKeyword;
    fetchProducts(selectedCategory, kw, sortBy);
  };

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setSearchKeyword('');
    setSortBy('newest');
    setPriceRange('all');
    setMinRating(0);
    fetchProducts(null, '', 'newest');
  };

  // Filter products by client criteria (Price Range & Min Rating)
  const filteredProducts = products.filter((p) => {
    const price = p.minPrice || 0;
    if (priceRange === 'under1m' && price >= 1000000) return false;
    if (priceRange === '1m-5m' && (price < 1000000 || price > 5000000)) return false;
    if (priceRange === '5m-20m' && (price < 5000000 || price > 20000000)) return false;
    if (priceRange === 'above20m' && price < 20000000) return false;

    if (minRating > 0 && (p.averageRating || 5.0) < minRating) return false;
    return true;
  });

  return (
    <div className="min-h-screen bg-[#F5F5FA] text-slate-800 font-sans flex flex-col">
      {/* 1. Universal Top Navigation */}
      <Navbar
        currentView={currentView}
        setCurrentView={setCurrentView}
        searchKeyword={searchKeyword}
        setSearchKeyword={setSearchKeyword}
        onSearchSubmit={handleSearchSubmit}
        selectedCategory={selectedCategory}
        setSelectedCategory={(catId) => {
          setSelectedCategory(catId);
          setCurrentView('storefront');
        }}
        categories={categories}
        onOpenCart={() => setIsCartOpen(true)}
        onOpenAuthModal={() => setIsAuthModalOpen(true)}
      />

      {/* 2. Main Content Views */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-3 sm:px-6 py-4 space-y-6">
        {/* VIEW 1: STOREFRONT (NGƯỜI MUA) */}
        {currentView === 'storefront' && (
          <div className="space-y-6">
            {/* Hero Carousel & Commitments Banner */}
            <HeroBanner onSelectCategory={(catId) => setSelectedCategory(catId)} />

            {/* Flash Sale Deal Shocker Section */}
            <FlashSaleSection
              products={products}
              onSelectProduct={(p) => setSelectedProduct(p)}
            />

            {/* Category Showcase Grid (Chuẩn Shopee) */}
            <div className="bg-white rounded-3xl p-5 border border-slate-200/90 shadow-sm space-y-3">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-sm text-slate-900 uppercase tracking-wide flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-orange-600" />
                  Danh mục sàn nổi bật
                </h3>
                <span className="text-xs text-orange-600 font-semibold cursor-pointer hover:underline">
                  Xem tất cả ngành hàng
                </span>
              </div>

              <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 pt-1">
                {categories.map((c) => (
                  <button
                    key={c.id}
                    onClick={() => setSelectedCategory(c.id === selectedCategory ? null : c.id)}
                    className={`p-3 rounded-2xl border text-center transition flex flex-col items-center justify-center space-y-1.5 group ${
                      selectedCategory === c.id
                        ? 'border-orange-500 bg-orange-50 text-orange-700 shadow-sm font-bold'
                        : 'border-slate-100 hover:border-orange-300 hover:shadow-md bg-white text-slate-700'
                    }`}
                  >
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-orange-100 to-amber-50 text-orange-600 flex items-center justify-center group-hover:scale-110 transition-transform shadow-inner">
                      <Smartphone className="w-6 h-6 stroke-[1.8]" />
                    </div>
                    <span className="text-xs font-semibold line-clamp-1 group-hover:text-orange-600 transition">
                      {c.name}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Main Product Catalog Layout with Filter Sidebar */}
            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
              {/* Left Column: Filter Sidebar */}
              <aside className="bg-white rounded-3xl border border-slate-200/90 p-5 shadow-sm space-y-5 text-xs hidden lg:block sticky top-24">
                <div className="flex justify-between items-center pb-3 border-b border-slate-100">
                  <div className="font-bold text-sm text-slate-900 flex items-center gap-1.5">
                    <Filter className="w-4 h-4 text-orange-600" />
                    Bộ lọc tìm kiếm
                  </div>
                  <button
                    onClick={handleResetFilters}
                    className="text-slate-400 hover:text-orange-600 flex items-center gap-1 transition text-[11px]"
                    title="Xóa tất cả bộ lọc"
                  >
                    <RotateCcw className="w-3 h-3" />
                    Xóa lọc
                  </button>
                </div>

                {/* Filter 1: By Category */}
                <div className="space-y-2">
                  <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">
                    Theo Danh Mục
                  </div>
                  <div className="space-y-1">
                    <button
                      onClick={() => setSelectedCategory(null)}
                      className={`w-full text-left px-2.5 py-1.5 rounded-lg transition ${
                        selectedCategory === null ? 'bg-orange-50 text-orange-600 font-bold' : 'hover:bg-slate-50 text-slate-600'
                      }`}
                    >
                      Tất cả danh mục
                    </button>
                    {categories.map((c) => (
                      <button
                        key={c.id}
                        onClick={() => setSelectedCategory(c.id)}
                        className={`w-full text-left px-2.5 py-1.5 rounded-lg transition flex justify-between items-center ${
                          selectedCategory === c.id ? 'bg-orange-50 text-orange-600 font-bold' : 'hover:bg-slate-50 text-slate-600'
                        }`}
                      >
                        <span className="truncate">{c.name}</span>
                        {c.productCount > 0 && (
                          <span className="text-[10px] text-slate-400">({c.productCount})</span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Filter 2: By Price Range */}
                <div className="space-y-2 pt-2 border-t border-slate-100">
                  <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">
                    Khoảng Giá (VND)
                  </div>
                  <div className="space-y-1 text-slate-600">
                    {[
                      { id: 'all', label: 'Tất cả mức giá' },
                      { id: 'under1m', label: 'Dưới 1.000.000đ' },
                      { id: '1m-5m', label: '1.000.000đ - 5.000.000đ' },
                      { id: '5m-20m', label: '5.000.000đ - 20.000.000đ' },
                      { id: 'above20m', label: 'Trên 20.000.000đ' },
                    ].map((pr) => (
                      <label key={pr.id} className="flex items-center space-x-2 cursor-pointer p-1 hover:text-orange-600">
                        <input
                          type="radio"
                          name="priceRange"
                          checked={priceRange === pr.id}
                          onChange={() => setPriceRange(pr.id)}
                          className="text-orange-600 focus:ring-orange-500"
                        />
                        <span>{pr.label}</span>
                      </label>
                    ))}
                  </div>
                </div>

                {/* Filter 3: By Rating */}
                <div className="space-y-2 pt-2 border-t border-slate-100">
                  <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px]">
                    Đánh Giá Sao
                  </div>
                  <div className="space-y-1 text-slate-600">
                    {[
                      { stars: 5, label: 'Từ 5 sao (Tuyệt đối)' },
                      { stars: 4, label: 'Từ 4 sao trở lên' },
                      { stars: 3, label: 'Từ 3 sao trở lên' },
                      { stars: 0, label: 'Tất cả đánh giá' },
                    ].map((r) => (
                      <button
                        key={r.stars}
                        onClick={() => setMinRating(r.stars)}
                        className={`w-full text-left px-2 py-1 rounded-lg flex items-center space-x-1.5 transition ${
                          minRating === r.stars ? 'bg-orange-50 text-orange-600 font-bold' : 'hover:bg-slate-50'
                        }`}
                      >
                        <div className="flex text-amber-400">
                          {r.stars > 0 ? (
                            Array.from({ length: r.stars }).map((_, i) => (
                              <Star key={i} className="w-3 h-3 fill-amber-400" />
                            ))
                          ) : (
                            <span className="text-[11px] text-slate-500">Tất cả</span>
                          )}
                        </div>
                        <span className="text-[11px] text-slate-600">{r.label}</span>
                      </button>
                    ))}
                  </div>
                </div>
              </aside>

              {/* Right Column: Sort Tool Bar & Product Grid */}
              <div className="lg:col-span-3 space-y-4">
                {/* Sort Bar */}
                <div className="bg-white rounded-2xl border border-slate-200/90 p-3 sm:px-5 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 shadow-sm text-xs">
                  <div className="flex items-center space-x-2">
                    <span className="text-slate-500 font-medium">Sắp xếp theo:</span>
                    <button
                      onClick={() => setSortBy('newest')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'newest' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      }`}
                    >
                      Mới nhất
                    </button>
                    <button
                      onClick={() => setSortBy('priceAsc')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'priceAsc' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      }`}
                    >
                      Giá thấp đến cao
                    </button>
                    <button
                      onClick={() => setSortBy('priceDesc')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'priceDesc' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      }`}
                    >
                      Giá cao đến thấp
                    </button>
                  </div>

                  <div className="text-slate-400 font-medium text-[11px]">
                    Tìm thấy <strong className="text-slate-800">{filteredProducts.length}</strong> sản phẩm
                  </div>
                </div>

                {/* Product Grid */}
                {loadingProducts ? (
                  <div className="py-20 text-center space-y-2">
                    <div className="w-8 h-8 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                    <div className="text-xs text-slate-400">Đang tải sản phẩm từ Catalog DB & Redis Cache...</div>
                  </div>
                ) : filteredProducts.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-3">
                    <div className="w-16 h-16 rounded-full bg-orange-50 text-orange-400 mx-auto flex items-center justify-center">
                      <Filter className="w-8 h-8" />
                    </div>
                    <h3 className="font-bold text-sm text-slate-800">Không tìm thấy sản phẩm phù hợp</h3>
                    <p className="text-xs text-slate-400 max-w-sm mx-auto">
                      Vui lòng thử điều chỉnh lại bộ lọc giá, từ khóa tìm kiếm hoặc chọn danh mục khác.
                    </p>
                    <button
                      onClick={handleResetFilters}
                      className="px-4 py-2 bg-orange-600 text-white rounded-xl text-xs font-semibold hover:bg-orange-700 transition"
                    >
                      Xem tất cả sản phẩm
                    </button>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4">
                    {filteredProducts.map((p) => (
                      <ProductCard
                        key={p.id}
                        product={p}
                        onSelect={(prod) => setSelectedProduct(prod)}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* VIEW 2: SELLER CENTER (KÊNH NGƯỜI BÁN) */}
        {currentView === 'seller' && (
          <SellerPortal
            categories={categories}
            onProductCreated={(newProd) => {
              setProducts([newProd, ...products]);
              alert('Sản phẩm đã được gửi lên hệ thống và chuyển vào hàng đợi duyệt của Admin (FR-04)!');
            }}
          />
        )}

        {/* VIEW 3: ADMIN PORTAL (CỔNG QUẢN TRỊ) */}
        {currentView === 'admin' && (
          <AdminPortal
            categories={categories}
            onCategoriesUpdated={fetchCategories}
          />
        )}
      </main>

      {/* 3. Footer Chuẩn Sàn TMĐT Việt Nam */}
      <footer className="bg-white border-t border-slate-200 mt-16 text-slate-600 text-xs">
        <div className="max-w-7xl mx-auto px-4 py-10 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="space-y-3">
            <div className="text-lg font-black text-slate-900 flex items-center">
              Quick<span className="text-orange-600">Shop</span>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed">
              Sàn Thương mại Điện tử Đa người bán (Multi-Vendor Marketplace) chuẩn Microservices & Cloud-Native.
            </p>
            <div className="text-[11px] text-slate-400">
              © 2026 QuickShop. Phát triển bởi Nhóm 03 (Lê Đức Anh, Triệu Quốc Bình, Nguyễn Văn Kiên).
            </div>
          </div>

          <div className="space-y-2">
            <h4 className="font-bold text-slate-900 uppercase text-xs tracking-wider">Chăm sóc khách hàng</h4>
            <ul className="space-y-1.5 text-slate-500 text-xs">
              <li className="hover:text-orange-600 cursor-pointer">Trung tâm trợ giúp</li>
              <li className="hover:text-orange-600 cursor-pointer">Hướng dẫn mua hàng</li>
              <li className="hover:text-orange-600 cursor-pointer">Chính sách vận chuyển</li>
              <li className="hover:text-orange-600 cursor-pointer">Chính sách bảo hành & đổi trả</li>
            </ul>
          </div>

          <div className="space-y-2">
            <h4 className="font-bold text-slate-900 uppercase text-xs tracking-wider">Về QuickShop</h4>
            <ul className="space-y-1.5 text-slate-500 text-xs">
              <li className="hover:text-orange-600 cursor-pointer">Giới thiệu sàn TMĐT</li>
              <li className="hover:text-orange-600 cursor-pointer">Tuyển dụng đối tác</li>
              <li className="hover:text-orange-600 cursor-pointer">Điều khoản dịch vụ</li>
              <li className="hover:text-orange-600 cursor-pointer">Chính sách bảo mật</li>
            </ul>
          </div>

          <div className="space-y-2">
            <h4 className="font-bold text-slate-900 uppercase text-xs tracking-wider">Đơn vị Vận chuyển & Thanh toán</h4>
            <p className="text-xs text-slate-500 leading-relaxed">
              Tích hợp đối tác vận chuyển GHN, GHTK, Viettel Post và cổng thanh toán VNPay Sandbox, Thẻ nội địa Napas, Ví điện tử Escrow.
            </p>
            <div className="pt-2 text-[11px] text-emerald-600 font-bold flex items-center gap-1">
              <ShieldCheck className="w-4 h-4" />
              Đã đăng ký chứng nhận với Bộ Công Thương
            </div>
          </div>
        </div>
      </footer>

      {/* 4. Modals & Widgets */}
      {selectedProduct && (
        <ProductDetailModal
          product={selectedProduct}
          onClose={() => setSelectedProduct(null)}
          onBuyNow={() => {
            setSelectedProduct(null);
            setIsCartOpen(true);
          }}
        />
      )}

      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
      />

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
      />

      <AiChatWidget />
    </div>
  );
}
