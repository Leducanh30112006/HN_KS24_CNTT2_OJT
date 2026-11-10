import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import HeroBanner from './components/HeroBanner';
import FlashSaleSection from './components/FlashSaleSection';
import ProductCard from './components/ProductCard';
import ProductDetailPage from './components/ProductDetailPage';
import FlashSalePage from './components/FlashSalePage';
import ShopDetailPage from './components/ShopDetailPage';
import CartPage from './components/CartPage';
import CheckoutPage from './components/CheckoutPage';
import CartDrawer from './components/CartDrawer';
import AiChatWidget from './components/AiChatWidget';
import SellerPortal from './components/SellerPortal';
import AdminPortal from './components/AdminPortal';
import LoginPage from './components/LoginPage';
import AuthModal from './components/AuthModal';
import Pagination from './components/Pagination';
import ApiErrorBanner from './components/ApiErrorBanner';
import catalogApi, { mapPriceRange } from './api/catalogApi';
import axiosClient from './api/axiosClient';
import { 
  Filter, RotateCcw, Star, ChevronRight, ChevronDown, ShieldCheck, 
  HelpCircle, Smartphone, Award, Truck, Sparkles, Lock, ShieldAlert, ArrowLeft, CheckCircle2 
} from 'lucide-react';
import { useAuthStore } from './stores/useAuthStore';
import { defaultCategories, defaultProducts } from './data/sampleProducts';

export default function App() {
  const [currentView, setCurrentView] = useState('storefront'); // 'storefront' | 'search' | 'product-detail' | 'login' | 'seller' | 'admin'
  const { user, isAuthenticated } = useAuthStore();

  const isSeller = isAuthenticated && user && (user.role === 'ROLE_SELLER' || user.role === 'SELLER');
  const isAdmin = isAuthenticated && user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
  const isSellerOnly = isSeller && !isAdmin;

  // Data states
  const [categories, setCategories] = useState(defaultCategories);
  const [products, setProducts] = useState(defaultProducts.slice(0, 12));
  const [loadingProducts, setLoadingProducts] = useState(false);

  // Pagination states
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(12);
  const [totalPages, setTotalPages] = useState(Math.max(1, Math.ceil(defaultProducts.length / 12)));
  const [totalElements, setTotalElements] = useState(defaultProducts.length);

  // API Error & Fallback states
  const [apiError, setApiError] = useState(null);
  const [isUsingFallback, setIsUsingFallback] = useState(false);
  const [isRetrying, setIsRetrying] = useState(false);

  // Category showcase display toggle (1 row vs all)
  const [showAllCategories, setShowAllCategories] = useState(false);

  // Filter states
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [sortBy, setSortBy] = useState('newest'); // 'newest' | 'priceAsc' | 'priceDesc'
  const [priceRange, setPriceRange] = useState('all'); // 'all', 'under1m', '1m-5m', '5m-20m', 'above20m'
  const [minRating, setMinRating] = useState(0);

  // Modal / Selected Product & Shop states
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [selectedShop, setSelectedShop] = useState(null);
  const [checkoutItems, setCheckoutItems] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [vnpayResult, setVnpayResult] = useState(null);

  // Fetch Categories from /api/catalog/categories
  const fetchCategories = async () => {
    try {
      const res = await catalogApi.getCategories();
      if (res && res.success && Array.isArray(res.data) && res.data.length > 0) {
        setCategories(res.data);
      } else {
        setCategories(defaultCategories);
      }
    } catch (err) {
      console.warn('Không thể nạp danh mục từ Catalog API:', err);
      setCategories(defaultCategories);
    }
  };

  // Fetch Products from /api/catalog/products with full filtering & pagination
  const fetchProducts = async (overrideParams = {}) => {
    setLoadingProducts(true);

    const catId = overrideParams.catId !== undefined ? overrideParams.catId : selectedCategory;
    const kw = overrideParams.kw !== undefined ? overrideParams.kw : searchKeyword;
    const sort = overrideParams.sort !== undefined ? overrideParams.sort : sortBy;
    const range = overrideParams.range !== undefined ? overrideParams.range : priceRange;
    const rating = overrideParams.rating !== undefined ? overrideParams.rating : minRating;
    const page = overrideParams.page !== undefined ? overrideParams.page : currentPage;
    const size = overrideParams.size !== undefined ? overrideParams.size : pageSize;

    const { minPrice: mappedMinPrice, maxPrice: mappedMaxPrice } = mapPriceRange(range);

    try {
      const res = await catalogApi.getProducts({
        categoryId: catId,
        keyword: kw,
        sortBy: sort,
        minPrice: mappedMinPrice,
        maxPrice: mappedMaxPrice,
        minRating: rating > 0 ? rating : undefined,
        page,
        size,
      });

      if (res && res.success && res.data) {
        const pageData = res.data;
        const items = Array.isArray(pageData.content) ? pageData.content : [];

        // Nếu database chưa có dữ liệu và không áp dụng bộ lọc nào ở trang 0
        if (items.length === 0 && !catId && !kw && range === 'all' && rating === 0 && page === 0) {
          setProducts(defaultProducts.slice(0, size));
          setTotalElements(defaultProducts.length);
          setTotalPages(Math.max(1, Math.ceil(defaultProducts.length / size)));
          setCurrentPage(0);
          setIsUsingFallback(true);
        } else {
          setProducts(items);
          setTotalElements(pageData.totalElements ?? items.length);
          setTotalPages(pageData.totalPages ?? 1);
          setCurrentPage(pageData.page ?? page);
          setIsUsingFallback(false);
          setApiError(null);
        }
      } else {
        throw new Error(res?.message || 'Không nhận được dữ liệu hợp lệ từ Catalog API.');
      }
    } catch (err) {
      console.warn('Catalog API offline hoặc lỗi kết nối, chuyển sang chế độ dữ liệu dự phòng:', err);
      const errMsg = err?.response?.data?.message || err?.message || 'Không thể kết nối đến máy chủ Catalog API.';
      setApiError(errMsg);
      setIsUsingFallback(true);

      // Lọc dữ liệu mẫu dự phòng phía client
      let filtered = [...defaultProducts];
      if (catId) {
        const childIds = categories.filter((c) => c.parentId === catId).map((c) => c.id);
        const allowedCatIds = [catId, ...childIds];
        filtered = filtered.filter((p) => allowedCatIds.includes(p.categoryId));
      }
      if (kw && kw.trim()) {
        const lkw = kw.toLowerCase().trim();
        filtered = filtered.filter(
          (p) =>
            p.name.toLowerCase().includes(lkw) ||
            (p.description && p.description.toLowerCase().includes(lkw))
        );
      }
      if (mappedMinPrice !== null) {
        filtered = filtered.filter((p) => (p.minPrice || 0) >= mappedMinPrice);
      }
      if (mappedMaxPrice !== null) {
        filtered = filtered.filter((p) => (p.minPrice || 0) <= mappedMaxPrice);
      }
      if (rating > 0) {
        filtered = filtered.filter((p) => (p.averageRating || 5.0) >= rating);
      }
      if (sort === 'priceAsc') {
        filtered.sort((a, b) => (a.minPrice || 0) - (b.minPrice || 0));
      } else if (sort === 'priceDesc') {
        filtered.sort((a, b) => (b.minPrice || 0) - (a.minPrice || 0));
      } else if (sort === 'newest') {
        filtered.sort((a, b) => (b.id || 0) - (a.id || 0));
      }

      const total = filtered.length;
      const computedTotalPages = Math.max(1, Math.ceil(total / size));
      const validPage = Math.min(page, computedTotalPages - 1);
      const start = Math.max(0, validPage * size);
      const pagedItems = filtered.slice(start, start + size);

      setProducts(pagedItems);
      setTotalElements(total);
      setTotalPages(computedTotalPages);
      setCurrentPage(validPage);
    } finally {
      setLoadingProducts(false);
      setIsRetrying(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  // Bắt sự kiện Return Redirect từ cổng thanh toán VNPay Sandbox
  useEffect(() => {
    try {
      const searchParams = new URLSearchParams(window.location.search);
      if (searchParams.has('vnp_ResponseCode')) {
        const responseCode = searchParams.get('vnp_ResponseCode');
        const isSuccess = responseCode === '00';
        const txnRef = searchParams.get('vnp_TxnRef') || '';
        const rawAmount = searchParams.get('vnp_Amount') || '0';
        const amount = Number(rawAmount) / 100;
        const bankCode = searchParams.get('vnp_BankCode') || '';
        const orderInfo = searchParams.get('vnp_OrderInfo') || '';

        // Đồng bộ kết quả thanh toán với Backend Payment Service
        axiosClient.get(`/api/payments/vnpay/return?${searchParams.toString()}`)
          .then((res) => {
            console.log('Xác thực giao dịch VNPay thành công:', res);
          })
          .catch((err) => {
            console.warn('Lỗi xác thực VNPay Return:', err);
          });

        setVnpayResult({
          isOpen: true,
          success: isSuccess,
          responseCode,
          txnRef,
          amount,
          bankCode,
          orderInfo,
          message: isSuccess
            ? 'Giao dịch thanh toán qua VNPay Sandbox thành công! Đơn hàng của bạn đã được chuyển sang trạng thái xử lý.'
            : 'Thanh toán qua VNPay không thành công hoặc giao dịch đã bị hủy bởi người dùng.'
        });

        // Xóa query param trên thanh địa chỉ URL để tránh lặp lại khi refresh
        window.history.replaceState({}, document.title, window.location.pathname);
      }
    } catch (e) {
      console.warn('Lỗi phân tích URL Return VNPay:', e);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [selectedCategory, sortBy, priceRange, minRating]);

  const handleSearchSubmit = (keywordOverride) => {
    const kw = keywordOverride !== undefined ? keywordOverride : searchKeyword;
    setSearchKeyword(kw);
    setCurrentPage(0);
    fetchProducts({ kw, page: 0 });
    // Chuyển sang trang kết quả tìm kiếm chuyên biệt
    setCurrentView('search');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSelectProduct = (prod) => {
    setSelectedProduct(prod);
    setCurrentView('product-detail');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSelectShop = (shop) => {
    setSelectedShop(shop);
    setCurrentView('shop-detail');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setSearchKeyword('');
    setSortBy('newest');
    setPriceRange('all');
    setMinRating(0);
    setCurrentPage(0);
    fetchProducts({
      catId: null,
      kw: '',
      sort: 'newest',
      range: 'all',
      rating: 0,
      page: 0,
    });
  };

  const handlePageChange = (newPage) => {
    if (newPage < 0 || newPage >= totalPages || newPage === currentPage) return;
    setCurrentPage(newPage);
    fetchProducts({ page: newPage });
    const gridElem = document.getElementById('catalog-product-grid');
    if (gridElem) {
      gridElem.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } else {
      window.scrollTo({ top: 350, behavior: 'smooth' });
    }
  };

  const handlePageSizeChange = (newSize) => {
    setPageSize(newSize);
    setCurrentPage(0);
    fetchProducts({ size: newSize, page: 0 });
  };

  const handleRetryApi = () => {
    setIsRetrying(true);
    fetchCategories();
    fetchProducts();
  };

  // filteredProducts luôn tương đương products vì server hoặc fallback đã phân trang & lọc
  const filteredProducts = products;

  // Hiển thị 1 hàng danh mục (6 danh mục) khi chưa bấm "Xem tất cả ngành hàng"
  const displayedCategories = showAllCategories ? categories : categories.slice(0, 6);

  // Dedicated Full-Page Login View
  if (currentView === 'login') {
    return (
      <LoginPage
        onBackToHome={() => setCurrentView('storefront')}
        onLoginSuccess={(targetView) => setCurrentView(targetView || 'storefront')}
      />
    );
  }

  return (
    <div className="min-h-screen bg-[#F5F5FA] text-slate-800 font-sans flex flex-col">
      {/* 1. Universal Top Navigation */}
      <Navbar
        currentView={currentView}
        setCurrentView={(view) => {
          if (view === 'storefront') {
            setSelectedProduct(null);
            setSelectedShop(null);
          }
          setCurrentView(view);
        }}
        searchKeyword={searchKeyword}
        setSearchKeyword={setSearchKeyword}
        onSearchSubmit={handleSearchSubmit}
        selectedCategory={selectedCategory}
        setSelectedCategory={(catId) => {
          setSelectedCategory(catId);
          setSelectedProduct(null);
          setSelectedShop(null);
          setCurrentView('storefront');
        }}
        categories={categories}
        onOpenCart={() => {
          setCurrentView('cart');
          window.scrollTo({ top: 0, behavior: 'smooth' });
        }}
        onOpenAuthModal={() => setCurrentView('login')}
        onNavigateToLogin={() => setCurrentView('login')}
      />

      {/* 2. Main Content Views */}
      <main className="flex-1 max-w-7xl mx-auto w-full px-3 sm:px-6 py-4 space-y-6">
        
        {/* VIEW 1: PRODUCT DETAIL PAGE (CHUYÊN BIỆT NHƯ SHOPEE VÀ TIKTOK SHOP) */}
        {currentView === 'product-detail' && selectedProduct && (
          <ProductDetailPage
            product={selectedProduct}
            allProducts={products}
            onBack={() => {
              setSelectedProduct(null);
              setCurrentView('storefront');
            }}
            onSelectRelatedProduct={handleSelectProduct}
            onSelectShop={handleSelectShop}
            onBuyNow={(buyItem) => {
              if (buyItem) {
                setCheckoutItems([buyItem]);
              }
              setCurrentView('checkout');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onRequireAuth={() => setCurrentView('login')}
          />
        )}

        {/* VIEW: DEDICATED CART PAGE (TRANG GIỎ HÀNG CHUẨN SHOPEE - CHỈ HIỆN SẢN PHẨM TRONG GIỎ, TÍCH CHỌN MÓN ĐỂ THANH TOÁN) */}
        {currentView === 'cart' && (
          <CartPage
            onProceedToCheckout={(selected) => {
              setCheckoutItems(selected);
              setCurrentView('checkout');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onBack={() => {
              if (selectedProduct) {
                setCurrentView('product-detail');
              } else {
                setCurrentView('storefront');
              }
            }}
            onSelectProduct={handleSelectProduct}
          />
        )}

        {/* VIEW: DEDICATED CHECKOUT PAGE (TRANG THANH TOÁN CÁC MÓN ĐÃ TÍCH CHỌN) */}
        {currentView === 'checkout' && (
          <CheckoutPage
            checkoutItems={checkoutItems}
            onBack={() => {
              setCurrentView('cart');
              window.scrollTo({ top: 0, behavior: 'smooth' });
            }}
            onSelectProduct={handleSelectProduct}
            onRequireAuth={() => setCurrentView('login')}
          />
        )}

        {/* VIEW: FLASH SALE DEDICATED PAGE (TRANG DEAL SỐC CHUYÊN BIỆT) */}
        {currentView === 'flash-sale' && (
          <FlashSalePage
            products={products}
            onBack={() => setCurrentView('storefront')}
            onSelectProduct={handleSelectProduct}
          />
        )}

        {/* VIEW: SHOP / SELLER DETAIL PAGE (TRANG THÔNG TIN SHOP VÀ SẢN PHẨM SHOP BÁN) */}
        {currentView === 'shop-detail' && (
          <ShopDetailPage
            shop={selectedShop}
            allProducts={products}
            onBack={() => {
              if (selectedProduct) {
                setCurrentView('product-detail');
              } else {
                setCurrentView('storefront');
              }
            }}
            onSelectProduct={handleSelectProduct}
            onRequireAuth={() => setCurrentView('login')}
          />
        )}

        {/* VIEW 2: DEDICATED SEARCH RESULTS PAGE (CHỈ HIỂN THỊ CÁC SẢN PHẨM CẦN TÌM) */}
        {currentView === 'search' && (
          <div className="space-y-6 animate-in fade-in duration-200">
            {/* Search Header Banner */}
            <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => {
                    setSearchKeyword('');
                    setSelectedCategory(null);
                    setCurrentView('storefront');
                    fetchProducts(null, '', 'newest');
                  }}
                  className="p-2 sm:px-3.5 sm:py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-semibold shadow-sm active:scale-95"
                  title="Quay lại Trang chủ"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Trang chủ Mua sắm</span>
                </button>

                <div>
                  <h2 className="text-base sm:text-xl font-black text-slate-900 flex items-center gap-2">
                    <span>Kết quả tìm kiếm cho:</span>
                    <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-600 to-rose-600 italic">
                      "{searchKeyword || 'Tất cả sản phẩm'}"
                    </span>
                  </h2>
                  <p className="text-xs text-slate-400 mt-0.5">
                    Trang kết quả tìm kiếm chuyên biệt theo tên sản phẩm, thương hiệu và mô tả
                  </p>
                </div>
              </div>

              <div className="text-xs font-semibold px-4 py-2 bg-orange-50 border border-orange-200 text-orange-700 rounded-full shadow-inner flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-orange-500" />
                <span>
                  Tìm thấy <strong className="font-black text-orange-600">{totalElements}</strong> sản phẩm
                  {totalPages > 1 && (
                    <span className="ml-1 text-slate-500 font-normal">
                      (Trang {currentPage + 1}/{totalPages})
                    </span>
                  )}
                </span>
              </div>
            </div>

            {/* Thông báo lỗi kết nối nếu có */}
            <ApiErrorBanner
              error={apiError}
              isRetrying={isRetrying}
              fallbackActive={isUsingFallback}
              onRetry={handleRetryApi}
              onDismiss={() => setApiError(null)}
            />

            {/* Search Sort Toolbar */}
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

              <button
                onClick={() => {
                  setSearchKeyword('');
                  setSelectedCategory(null);
                  setCurrentView('storefront');
                  fetchProducts(null, '', 'newest');
                }}
                className="text-xs text-orange-600 hover:underline flex items-center gap-1 font-semibold"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Xóa tìm kiếm & về trang chủ
              </button>
            </div>

            {/* Product Grid: Only Searched Products */}
            {loadingProducts ? (
              <div className="py-24 text-center space-y-3 bg-white rounded-3xl border border-slate-200/80 shadow-sm">
                <div className="w-9 h-9 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                <div className="text-xs text-slate-500 font-medium">Đang tìm kiếm sản phẩm phù hợp...</div>
              </div>
            ) : filteredProducts.length === 0 ? (
              <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 shadow-sm space-y-4">
                <div className="w-16 h-16 rounded-full bg-orange-50 text-orange-400 mx-auto flex items-center justify-center">
                  <Filter className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-800">Không tìm thấy sản phẩm nào</h3>
                  <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                    Không có sản phẩm nào khớp với từ khóa <strong className="text-slate-700">"{searchKeyword}"</strong>. Vui lòng thử từ khóa khác hoặc quay lại trang chủ.
                  </p>
                </div>
                <button
                  onClick={() => {
                    setSearchKeyword('');
                    setSelectedCategory(null);
                    setCurrentView('storefront');
                    fetchProducts(null, '', 'newest');
                  }}
                  className="px-5 py-2.5 bg-orange-600 text-white rounded-xl text-xs font-semibold hover:bg-orange-700 shadow-md shadow-orange-500/20 transition inline-flex items-center gap-1.5"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Xem tất cả sản phẩm tại Trang chủ
                </button>
              </div>
            ) : (
              <div className="space-y-6">
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-4">
                  {filteredProducts.map((p) => (
                    <ProductCard
                      key={p.id}
                      product={p}
                      onSelect={handleSelectProduct}
                      onSelectShop={handleSelectShop}
                      onRequireAuth={() => setCurrentView('login')}
                    />
                  ))}
                </div>

                {/* Phân trang kết quả tìm kiếm */}
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  pageSize={pageSize}
                  onPageChange={handlePageChange}
                  onPageSizeChange={handlePageSizeChange}
                />
              </div>
            )}
          </div>
        )}

        {/* VIEW 3: STOREFRONT (NGƯỜI MUA - TRANG CHỦ) */}
        {currentView === 'storefront' && (
          <div className="space-y-6">
            {/* Hero Carousel & Commitments Banner */}
            <HeroBanner onSelectCategory={(catId) => setSelectedCategory(catId)} />

            {/* Flash Sale Deal Shocker Section */}
            <FlashSaleSection
              products={products}
              onSelectProduct={handleSelectProduct}
              onViewAllDeals={() => {
                setCurrentView('flash-sale');
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
            />

            {/* Category Showcase Grid (Hiển thị 1 hàng, bấm xem tất cả mới mở rộng) */}
            <div className="bg-white rounded-3xl p-5 border border-slate-200/90 shadow-sm space-y-3">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-sm text-slate-900 uppercase tracking-wide flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-orange-600" />
                  Danh mục sàn nổi bật
                </h3>
                <button
                  type="button"
                  onClick={() => setShowAllCategories(!showAllCategories)}
                  className="text-xs text-orange-600 font-semibold cursor-pointer hover:underline flex items-center gap-1 transition"
                >
                  <span>{showAllCategories ? 'Thu gọn' : 'Xem tất cả ngành hàng'}</span>
                  <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${showAllCategories ? 'rotate-180' : ''}`} />
                </button>
              </div>

              <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 pt-1">
                {displayedCategories.map((c) => (
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
                        className={`w-full text-left py-1.5 rounded-lg transition flex justify-between items-center ${
                          c.parentId ? 'pl-6 pr-2.5 text-[11px]' : 'px-2.5 font-bold text-slate-800'
                        } ${
                          selectedCategory === c.id ? 'bg-orange-50 text-orange-600 font-bold' : 'hover:bg-slate-50 text-slate-600'
                        }`}
                      >
                        <span className="truncate">{c.parentId ? `↳ ${c.name}` : c.name}</span>
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
              <div className="lg:col-span-3 space-y-4" id="catalog-product-grid">
                {/* Thông báo lỗi kết nối nếu có */}
                <ApiErrorBanner
                  error={apiError}
                  isRetrying={isRetrying}
                  fallbackActive={isUsingFallback}
                  onRetry={handleRetryApi}
                  onDismiss={() => setApiError(null)}
                />

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

                  <div className="text-slate-500 font-medium text-[11px] flex items-center gap-2">
                    <span>
                      Tìm thấy <strong className="text-slate-800 font-bold">{totalElements}</strong> sản phẩm
                    </span>
                    {totalPages > 1 && (
                      <span className="text-slate-400">
                        (Trang <strong className="text-orange-600 font-semibold">{currentPage + 1}</strong> / {totalPages})
                      </span>
                    )}
                  </div>
                </div>

                {/* Product Grid */}
                {loadingProducts ? (
                  <div className="space-y-4">
                    <div className="py-6 text-center space-y-2 bg-white rounded-2xl border border-slate-200/80 shadow-sm">
                      <div className="w-7 h-7 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                      <div className="text-xs text-slate-500 font-medium">Đang tải sản phẩm từ Catalog Service & Redis Cache...</div>
                    </div>
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4">
                      {Array.from({ length: 6 }).map((_, i) => (
                        <div key={i} className="bg-white rounded-2xl border border-slate-100 p-3 space-y-2.5 animate-pulse">
                          <div className="aspect-square bg-slate-100 rounded-xl" />
                          <div className="h-3 bg-slate-100 rounded w-1/3" />
                          <div className="h-4 bg-slate-100 rounded w-full" />
                          <div className="h-4 bg-slate-100 rounded w-1/2" />
                        </div>
                      ))}
                    </div>
                  </div>
                ) : filteredProducts.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-3 shadow-sm">
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
                  <div className="space-y-6">
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4">
                      {filteredProducts.map((p) => (
                        <ProductCard
                          key={p.id}
                          product={p}
                          onSelect={handleSelectProduct}
                          onSelectShop={handleSelectShop}
                          onRequireAuth={() => setCurrentView('login')}
                        />
                      ))}
                    </div>

                    {/* Phân trang cho Storefront Catalog */}
                    <Pagination
                      currentPage={currentPage}
                      totalPages={totalPages}
                      totalElements={totalElements}
                      pageSize={pageSize}
                      onPageChange={handlePageChange}
                      onPageSizeChange={handlePageSizeChange}
                    />
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* VIEW 4: SELLER CENTER (KÊNH NGƯỜI BÁN - PHÂN QUYỀN RBAC) */}
        {currentView === 'seller' && (
          isSellerOnly ? (
            <SellerPortal
              categories={categories}
              onBackToStorefront={() => {
                setSelectedProduct(null);
                setCurrentView('storefront');
              }}
              onProductCreated={(newProd) => {
                setProducts([newProd, ...products]);
              }}
            />
          ) : (
            <div className="bg-white rounded-3xl p-10 text-center border border-slate-200/90 shadow-sm max-w-lg mx-auto my-12 space-y-4">
              <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-600 mx-auto flex items-center justify-center">
                <ShieldAlert className="w-8 h-8" />
              </div>
              <h2 className="text-lg font-bold text-slate-900">Kênh Người Bán - Yêu cầu xác thực</h2>
              <p className="text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
                Khu vực quản lý dành riêng cho Người bán (ROLE_SELLER) và Quản trị viên (ROLE_ADMIN). Vui lòng đăng nhập với tài khoản người bán để tiếp tục.
              </p>
              <div className="flex justify-center items-center gap-3 pt-2">
                <button
                  onClick={() => setCurrentView('login')}
                  className="px-5 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-semibold text-xs rounded-xl shadow-md transition"
                >
                  Đăng nhập Người Bán
                </button>
                <button
                  onClick={() => {
                    setSelectedProduct(null);
                    setCurrentView('storefront');
                  }}
                  className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition flex items-center gap-1.5"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Quay lại Mua sắm
                </button>
              </div>
            </div>
          )
        )}

        {/* VIEW 5: ADMIN PORTAL (CỔNG QUẢN TRỊ - PHÂN QUYỀN RBAC) */}
        {currentView === 'admin' && (
          isAdmin ? (
            <AdminPortal
              categories={categories}
              onBackToStorefront={() => {
                setSelectedProduct(null);
                setCurrentView('storefront');
              }}
              onCategoriesUpdated={fetchCategories}
            />
          ) : (
            <div className="bg-white rounded-3xl p-10 text-center border border-slate-200/90 shadow-sm max-w-lg mx-auto my-12 space-y-4">
              <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 mx-auto flex items-center justify-center">
                <Lock className="w-8 h-8" />
              </div>
              <h2 className="text-lg font-bold text-slate-900">Cổng Quản Trị - Giới hạn quyền truy cập</h2>
              <p className="text-xs text-slate-500 leading-relaxed max-w-sm mx-auto">
                Trang này yêu cầu thẩm quyền Quản trị viên hệ thống (ROLE_ADMIN). Vui lòng đăng nhập tài khoản có thẩm quyền Quản trị để truy cập.
              </p>
              <div className="flex justify-center items-center gap-3 pt-2">
                <button
                  onClick={() => setCurrentView('login')}
                  className="px-5 py-2.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs rounded-xl shadow-md transition"
                >
                  Đăng nhập Quản trị viên
                </button>
                <button
                  onClick={() => {
                    setSelectedProduct(null);
                    setCurrentView('storefront');
                  }}
                  className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl transition flex items-center gap-1.5"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Quay lại Mua sắm
                </button>
              </div>
            </div>
          )
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
      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
        onRequireAuth={() => setCurrentView('login')}
      />

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
      />

      {/* 5. Modal Kết quả Thanh toán VNPay Sandbox */}
      {vnpayResult?.isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 text-center space-y-5 shadow-2xl border border-slate-100">
            <div className={`w-16 h-16 rounded-full mx-auto flex items-center justify-center ${vnpayResult.success ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'}`}>
              {vnpayResult.success ? (
                <CheckCircle2 className="w-10 h-10 stroke-[2.5]" />
              ) : (
                <ShieldAlert className="w-10 h-10 stroke-[2.5]" />
              )}
            </div>

            <div className="space-y-1">
              <span className={`inline-block px-3 py-1 text-[11px] font-bold rounded-full uppercase tracking-wider ${vnpayResult.success ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
                {vnpayResult.success ? 'Thanh toán VNPay thành công' : 'Thanh toán không thành công'}
              </span>
              <h3 className="text-xl font-black text-slate-900 pt-2">
                {vnpayResult.success ? 'Giao dịch hoàn tất!' : 'Giao dịch thất bại / Bị hủy'}
              </h3>
              <p className="text-xs text-slate-500 leading-relaxed max-w-xs mx-auto">
                {vnpayResult.message}
              </p>
            </div>

            <div className="bg-slate-50 rounded-2xl p-4 text-xs space-y-2 border border-slate-200/80 text-left">
              {vnpayResult.txnRef && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Mã giao dịch:</span>
                  <span className="font-mono font-bold text-slate-800">{vnpayResult.txnRef}</span>
                </div>
              )}
              {vnpayResult.amount > 0 && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Số tiền thanh toán:</span>
                  <span className="font-bold text-orange-600">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(vnpayResult.amount)}
                  </span>
                </div>
              )}
              {vnpayResult.bankCode && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Ngân hàng thanh toán:</span>
                  <span className="font-medium text-slate-800">{vnpayResult.bankCode}</span>
                </div>
              )}
            </div>

            <button
              onClick={() => {
                setVnpayResult(null);
                setCurrentView('storefront');
              }}
              className="w-full py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-orange-500/20 transition active:scale-95"
            >
              Đóng và tiếp tục mua sắm
            </button>
          </div>
        </div>
      )}

      <AiChatWidget />
    </div>
  );
}
