import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
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
import UserProfilePage from './components/UserProfilePage';
import OrderHistoryPage from './components/OrderHistoryPage';
import ShipperPortal from './components/ShipperPortal';
import catalogApi, { mapPriceRange } from './api/catalogApi';
import axiosClient from './api/axiosClient';
import Lenis from 'lenis';
import { gsap, ScrollTrigger, useGSAP } from './lib/gsap';
import {
  Filter, RotateCcw, Star, ChevronRight, ChevronDown, ShieldCheck,
  HelpCircle, Smartphone, Award, Truck, Sparkles, Lock, ShieldAlert, ArrowLeft, CheckCircle2,
  Store, SlidersHorizontal, X, Laptop, Shirt, Home, BookOpen, Activity, ShoppingBag, Cpu
} from 'lucide-react';
import { useAuthStore } from './stores/useAuthStore';
import { useCartStore } from './stores/useCartStore';
import { defaultCategories, defaultProducts } from './data/sampleProducts';

// Hàm xáo trộn mảng ngẫu nhiên (Fisher-Yates shuffle) để mỗi lần load trang chính sản phẩm đều mới mẻ và khác biệt
function shuffleArray(arr) {
  if (!Array.isArray(arr)) return [];
  const result = [...arr];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}

// Xây dựng URL đồng bộ trạng thái view và query params vào History API
function buildUrlForState(view, extra = {}) {
  if (typeof window === 'undefined') return '/';
  const params = new URLSearchParams();
  if (view && view !== 'storefront') {
    params.set('view', view);
  }
  if (extra.id) {
    params.set('id', String(extra.id));
  }
  if (extra.shopId) {
    params.set('shopId', String(extra.shopId));
  }
  if (extra.kw) {
    params.set('q', extra.kw);
  }
  if (extra.cat !== undefined && extra.cat !== null) {
    params.set('cat', String(extra.cat));
  }
  const qs = params.toString();
  return qs ? `?${qs}` : window.location.pathname;
}

// Đọc trạng thái ban đầu từ URL để phục hồi ngay khi tải lại trang
function getInitialUrlState() {
  if (typeof window === 'undefined') {
    return { view: 'storefront', q: '', cat: null, id: null, shopId: null };
  }
  try {
    const params = new URLSearchParams(window.location.search);
    const view = params.get('view') || 'storefront';
    const q = params.get('q') || '';
    const cat = params.get('cat') ? Number(params.get('cat')) : null;
    const id = params.get('id') || null;
    const shopId = params.get('shopId') || null;
    return { view, q, cat, id, shopId };
  } catch {
    return { view: 'storefront', q: '', cat: null, id: null, shopId: null };
  }
}

// Ánh xạ icon và bảng màu nhận diện thương hiệu cho từng ngành hàng
function getCategoryIconMeta(category) {
  const iconKey = (category?.icon || '').toLowerCase();
  const slug = (category?.slug || '').toLowerCase();
  const name = (category?.name || '').toLowerCase();

  if (iconKey === 'laptop' || slug.includes('laptop') || name.includes('laptop') || name.includes('máy tính')) {
    return {
      Icon: Laptop,
      bg: 'bg-blue-50 text-blue-600 group-hover:bg-blue-600 group-hover:text-white',
      borderHover: 'hover:border-blue-300',
    };
  }
  if (iconKey === 'shirt' || slug.includes('thoi-trang') || name.includes('thời trang') || name.includes('quần áo')) {
    return {
      Icon: Shirt,
      bg: 'bg-rose-50 text-rose-600 group-hover:bg-rose-600 group-hover:text-white',
      borderHover: 'hover:border-rose-300',
    };
  }
  if (iconKey === 'shoppingbag' || slug.includes('giay-dep') || name.includes('giày') || name.includes('túi')) {
    return {
      Icon: ShoppingBag,
      bg: 'bg-amber-50 text-amber-600 group-hover:bg-amber-600 group-hover:text-white',
      borderHover: 'hover:border-amber-300',
    };
  }
  if (iconKey === 'sparkles' || slug.includes('my-pham') || name.includes('mỹ phẩm') || name.includes('làm đẹp')) {
    return {
      Icon: Sparkles,
      bg: 'bg-pink-50 text-pink-600 group-hover:bg-pink-600 group-hover:text-white',
      borderHover: 'hover:border-pink-300',
    };
  }
  if (iconKey === 'home' || slug.includes('nha-cua') || name.includes('nhà cửa') || name.includes('đời sống') || name.includes('gia đình')) {
    return {
      Icon: Home,
      bg: 'bg-emerald-50 text-emerald-600 group-hover:bg-emerald-600 group-hover:text-white',
      borderHover: 'hover:border-emerald-300',
    };
  }
  if (iconKey === 'bookopen' || slug.includes('sach') || name.includes('sách') || name.includes('văn phòng phẩm')) {
    return {
      Icon: BookOpen,
      bg: 'bg-indigo-50 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white',
      borderHover: 'hover:border-indigo-300',
    };
  }
  if (iconKey === 'activity' || slug.includes('the-thao') || name.includes('thể thao') || name.includes('dã ngoại')) {
    return {
      Icon: Activity,
      bg: 'bg-teal-50 text-teal-600 group-hover:bg-teal-600 group-hover:text-white',
      borderHover: 'hover:border-teal-300',
    };
  }
  if (iconKey === 'cpu' || slug.includes('dien-tu') || name.includes('điện tử')) {
    return {
      Icon: Cpu,
      bg: 'bg-cyan-50 text-cyan-700 group-hover:bg-cyan-600 group-hover:text-white',
      borderHover: 'hover:border-cyan-300',
    };
  }
  return {
    Icon: Smartphone,
    bg: 'bg-orange-50 text-orange-600 group-hover:bg-orange-600 group-hover:text-white',
    borderHover: 'hover:border-orange-300',
  };
}

export default function App() {
  const initialUrl = getInitialUrlState();
  const [currentView, setCurrentView] = useState(() => {
    if (initialUrl.view && initialUrl.view !== 'storefront') {
      return initialUrl.view;
    }
    const savedView = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('quickshop_current_view') : null;
    return savedView || 'storefront';
  });
  const { user, isAuthenticated } = useAuthStore();
  const { syncCartOnLogin, handleLogoutClear, ownerUserId } = useCartStore();

  const isSeller = isAuthenticated && user && (user.role === 'ROLE_SELLER' || user.role === 'SELLER' || user.role?.includes('ADMIN'));
  const isAdmin = isAuthenticated && user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
  const isShipper = isAuthenticated && user && (user.role === 'ROLE_SHIPPER' || user.role === 'SHIPPER' || user.role?.includes('ADMIN'));
  const isSellerOnly = isSeller && !isAdmin;
  const lenisRef = useRef(null);
  const storefrontRef = useRef(null);
  const mainViewRef = useRef(null);

  // Khởi tạo Lenis Smooth Scroll đồng bộ cùng GSAP ScrollTrigger và ticker
  useEffect(() => {
    const lenis = new Lenis({
      duration: 1.1,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smoothWheel: true,
      touchMultiplier: 1.5,
    });
    lenisRef.current = lenis;

    lenis.on('scroll', ScrollTrigger.update);

    const updateTicker = (time) => {
      lenis.raf(time * 1000);
    };

    gsap.ticker.add(updateTicker);
    gsap.ticker.lagSmoothing(0);

    return () => {
      gsap.ticker.remove(updateTicker);
      lenis.destroy();
      lenisRef.current = null;
    };
  }, []);

  // Persistent checkout items in sessionStorage (chống mất giỏ hàng chọn thanh toán khi chuyển sang đăng nhập)
  const [checkoutItems, setCheckoutItemsState] = useState(() => {
    try {
      if (typeof sessionStorage !== 'undefined') {
        const saved = sessionStorage.getItem('quickshop_checkout_items');
        return saved ? JSON.parse(saved) : [];
      }
    } catch (e) {
      console.warn('Lỗi đọc quickshop_checkout_items:', e);
    }
    return [];
  });

  const setCheckoutItems = (items) => {
    const arr = Array.isArray(items) ? items : [];
    setCheckoutItemsState(arr);
    try {
      if (typeof sessionStorage !== 'undefined') {
        if (arr.length > 0) {
          sessionStorage.setItem('quickshop_checkout_items', JSON.stringify(arr));
        } else {
          sessionStorage.removeItem('quickshop_checkout_items');
        }
      }
    } catch (e) {}
  };

  // Điều hướng có tích hợp HTML5 History API (pushState/replaceState)
  const handleNavigateView = (view, options = {}) => {
    const { replace = false, extra = {}, skipPush = false, scroll = true } = options;
    if (view === 'storefront') {
      setSelectedProduct(null);
      setSelectedShop(null);
    }
    sessionStorage.setItem('quickshop_current_view', view);
    setCurrentView(view);

    if (!skipPush && typeof window !== 'undefined') {
      const statePayload = {
        view,
        id: extra.id || (view === 'product-detail' ? selectedProduct?.id : undefined),
        shopId: extra.shopId || (view === 'shop-detail' ? selectedShop?.id : undefined),
        kw: extra.kw !== undefined ? extra.kw : (view === 'search' ? searchKeyword : undefined),
        cat: extra.cat !== undefined ? extra.cat : (view === 'search' ? selectedCategory : undefined),
      };
      const url = buildUrlForState(view, statePayload);
      if (replace) {
        window.history.replaceState(statePayload, '', url);
      } else {
        window.history.pushState(statePayload, '', url);
      }
    }
    if (scroll) {
      if (lenisRef.current) {
        lenisRef.current.scrollTo(0, { immediate: false });
      } else {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    }
  };

  // Lưu URL/view đích trước khi chuyển sang màn hình đăng nhập
  const handleRequireAuth = (intendedView = null) => {
    const target = intendedView || currentView || 'storefront';
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem('quickshop_auth_redirect', target);
    }
    handleNavigateView('login');
  };

  const handleAuthSuccess = (targetView) => {
    const savedRedirect = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('quickshop_auth_redirect') : null;
    if (savedRedirect && ['admin', 'seller', 'shipper', 'storefront', 'cart', 'orders', 'profile', 'checkout', 'product-detail'].includes(savedRedirect)) {
      sessionStorage.removeItem('quickshop_auth_redirect');
      handleNavigateView(savedRedirect, { replace: true });
    } else {
      handleNavigateView(targetView || 'storefront', { replace: true });
    }
  };

  // Đồng bộ phiên đăng nhập/đăng xuất, giỏ hàng và view tương ứng
  useEffect(() => {
    if (isAuthenticated && user?.userId) {
      if (!isShipper) {
        syncCartOnLogin(user.userId, { dedupeRecent: true });
      }
      // Ưu tiên chuyển hướng về trang trước đó mà người dùng đang dở dang (ví dụ: checkout)
      const savedRedirect = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('quickshop_auth_redirect') : null;
      if (savedRedirect && ['admin', 'seller', 'shipper', 'storefront', 'cart', 'orders', 'profile', 'checkout', 'product-detail'].includes(savedRedirect)) {
        sessionStorage.removeItem('quickshop_auth_redirect');
        handleNavigateView(savedRedirect, { replace: true });
        return;
      }
      const savedView = sessionStorage.getItem('quickshop_current_view');
      if (savedView && ['admin', 'seller', 'shipper', 'storefront', 'cart', 'orders', 'profile', 'checkout'].includes(savedView)) {
        setCurrentView(savedView);
      } else if (user?.role) {
        const r = String(user.role).toUpperCase();
        if (r.includes('ADMIN')) {
          setCurrentView('admin');
        } else if (r.includes('SELLER')) {
          setCurrentView('seller');
        } else if (r.includes('SHIPPER')) {
          setCurrentView('shipper');
        }
      }
    } else {
      if (ownerUserId !== null) {
        handleLogoutClear(ownerUserId);
      }
    }
  }, [isAuthenticated, user?.userId, user?.role]);

  // Data states
  const [categories, setCategories] = useState(defaultCategories);
  const [products, setProducts] = useState(() => shuffleArray(defaultProducts).slice(0, 12));
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

  // Filter states (khởi tạo từ URL nếu có để hỗ trợ refresh / bookmark / share)
  const [selectedCategory, setSelectedCategory] = useState(() => initialUrl.cat || null);
  const [searchKeyword, setSearchKeyword] = useState(() => initialUrl.q || '');
  const [sortBy, setSortBy] = useState('newest'); // 'newest' | 'priceAsc' | 'priceDesc'
  const [priceRange, setPriceRange] = useState('all'); // 'all', 'under1m', '1m-5m', '5m-20m', 'above20m'
  const [minRating, setMinRating] = useState(0);
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  // Khóa cuộn trang khi Modal Bộ Lọc mở giữa màn hình (kèm Lenis stop)
  useEffect(() => {
    if (isFilterOpen) {
      document.body.style.overflow = 'hidden';
      if (lenisRef.current) {
        lenisRef.current.stop();
      }
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') setIsFilterOpen(false);
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => {
        document.body.style.overflow = '';
        if (lenisRef.current) {
          lenisRef.current.start();
        }
        window.removeEventListener('keydown', handleKeyDown);
      };
    } else {
      document.body.style.overflow = '';
      if (lenisRef.current) {
        lenisRef.current.start();
      }
    }
  }, [isFilterOpen]);

  // Modal / Selected Product & Shop states
  const [selectedProduct, setSelectedProduct] = useState(() => {
    if (initialUrl.id) {
      return defaultProducts.find((p) => String(p.id) === String(initialUrl.id)) || null;
    }
    return null;
  });
  const [selectedShop, setSelectedShop] = useState(null);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [vnpayResult, setVnpayResult] = useState(null);

  // Lắng nghe nút Back / Forward của trình duyệt qua HTML5 PopState
  useEffect(() => {
    const handlePopState = (e) => {
      const urlState = getInitialUrlState();
      const targetView = e.state?.view || urlState.view || 'storefront';
      setCurrentView(targetView);
      sessionStorage.setItem('quickshop_current_view', targetView);

      if (urlState.q !== searchKeyword) setSearchKeyword(urlState.q);
      if (urlState.cat !== selectedCategory) setSelectedCategory(urlState.cat);

      if (targetView === 'product-detail' && (urlState.id || e.state?.id)) {
        const prodId = urlState.id || e.state?.id;
        const found = defaultProducts.find((p) => String(p.id) === String(prodId));
        if (found) setSelectedProduct(found);
      } else if (targetView === 'storefront') {
        setSelectedProduct(null);
        setSelectedShop(null);
      }
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [searchKeyword, selectedCategory]);

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
    // Đảm bảo overrideParams luôn là object an toàn, tránh lỗi khi truyền null hoặc tham số rời rạc
    const safeParams = (overrideParams && typeof overrideParams === 'object') ? overrideParams : {};
    setLoadingProducts(true);

    const catId = safeParams.catId !== undefined ? safeParams.catId : selectedCategory;
    const kw = safeParams.kw !== undefined ? safeParams.kw : searchKeyword;
    const sort = safeParams.sort !== undefined ? safeParams.sort : sortBy;
    const range = safeParams.range !== undefined ? safeParams.range : priceRange;
    const rating = safeParams.rating !== undefined ? safeParams.rating : minRating;
    const page = safeParams.page !== undefined ? safeParams.page : currentPage;
    const size = safeParams.size !== undefined ? safeParams.size : pageSize;
    const shouldShuffle = safeParams.shuffle !== undefined ? safeParams.shuffle : false;

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

        // Bổ sung các trường highlights, specifications, descriptionImages từ defaultProducts vào items nếu thiếu
        const enrichedItems = items.map((serverItem) => {
          const match = defaultProducts.find((p) => String(p.id) === String(serverItem.id));
          return match
            ? {
                ...match,
                ...serverItem,
                highlights: serverItem.highlights || match.highlights || [],
                specifications: serverItem.specifications || match.specifications || [],
                descriptionImages: serverItem.descriptionImages || match.descriptionImages || [],
              }
            : serverItem;
        });

        // Kết hợp server items và defaultProducts thành một kho sản phẩm 50+ mẫu đa dạng
        const serverIds = new Set(enrichedItems.map((p) => String(p.id)));
        const additionalSamples = defaultProducts.filter((p) => !serverIds.has(String(p.id)));
        let fullCatalog = [...enrichedItems, ...additionalSamples];

        // Lọc theo Danh Mục (nếu có)
        if (catId) {
          const childIds = categories.filter((c) => c.parentId === catId).map((c) => c.id);
          const allowedCatIds = [catId, ...childIds];
          fullCatalog = fullCatalog.filter((p) => allowedCatIds.includes(p.categoryId));
        }

        // Lọc theo Từ Khóa Tìm Kiếm (nếu có)
        if (kw && kw.trim()) {
          const lkw = kw.toLowerCase().trim();
          fullCatalog = fullCatalog.filter(
            (p) =>
              p.name.toLowerCase().includes(lkw) ||
              (p.description && p.description.toLowerCase().includes(lkw)) ||
              (p.brand && p.brand.toLowerCase().includes(lkw))
          );
        }

        // Lọc theo Khoảng Giá (nếu có)
        if (mappedMinPrice !== null) {
          fullCatalog = fullCatalog.filter((p) => (p.minPrice || 0) >= mappedMinPrice);
        }
        if (mappedMaxPrice !== null) {
          fullCatalog = fullCatalog.filter((p) => (p.minPrice || 0) <= mappedMaxPrice);
        }

        // Lọc theo Đánh Giá Sao (nếu có)
        if (rating > 0) {
          fullCatalog = fullCatalog.filter((p) => (p.averageRating || 5.0) >= rating);
        }

        // Sắp xếp sản phẩm:
        if (sort === 'priceAsc') {
          fullCatalog.sort((a, b) => (a.minPrice || 0) - (b.minPrice || 0));
        } else if (sort === 'priceDesc') {
          fullCatalog.sort((a, b) => (b.minPrice || 0) - (a.minPrice || 0));
        } else {
          // Khi sort === 'newest' hoặc mặc định:
          // Xáo trộn ngẫu nhiên để mỗi lần duyệt/tải trang chính các sản phẩm xuất hiện mới mẻ và khác nhau
          if (shouldShuffle || (!catId && !kw && range === 'all' && rating === 0)) {
            fullCatalog = shuffleArray(fullCatalog);
          } else {
            fullCatalog.sort((a, b) => (b.id || 0) - (a.id || 0));
          }
        }

        const total = fullCatalog.length;
        const computedTotalPages = Math.max(1, Math.ceil(total / size));
        const validPage = Math.min(page, computedTotalPages - 1);
        const start = Math.max(0, validPage * size);
        const pagedItems = fullCatalog.slice(start, start + size);

        setProducts(pagedItems);
        setTotalElements(total);
        setTotalPages(computedTotalPages);
        setCurrentPage(validPage);
        setIsUsingFallback(false);
        setApiError(null);
      } else {
        throw new Error(res?.message || 'Không nhận được dữ liệu hợp lệ từ Catalog API.');
      }
    } catch (err) {
      console.warn('Catalog API offline hoặc lỗi kết nối, chuyển sang chế độ dữ liệu dự phòng:', err);
      const errMsg = err?.response?.data?.message || err?.message || 'Không thể kết nối đến máy chủ Catalog API.';
      setApiError(errMsg);
      setIsUsingFallback(true);

      // Lọc dữ liệu mẫu dự phòng phía client
      let fallbackCatalog = [...defaultProducts];
      if (catId) {
        const childIds = categories.filter((c) => c.parentId === catId).map((c) => c.id);
        const allowedCatIds = [catId, ...childIds];
        fallbackCatalog = fallbackCatalog.filter((p) => allowedCatIds.includes(p.categoryId));
      }
      if (kw && kw.trim()) {
        const lkw = kw.toLowerCase().trim();
        fallbackCatalog = fallbackCatalog.filter(
          (p) =>
            p.name.toLowerCase().includes(lkw) ||
            (p.description && p.description.toLowerCase().includes(lkw)) ||
            (p.brand && p.brand.toLowerCase().includes(lkw))
        );
      }
      if (mappedMinPrice !== null) {
        fallbackCatalog = fallbackCatalog.filter((p) => (p.minPrice || 0) >= mappedMinPrice);
      }
      if (mappedMaxPrice !== null) {
        fallbackCatalog = fallbackCatalog.filter((p) => (p.minPrice || 0) <= mappedMaxPrice);
      }
      if (rating > 0) {
        fallbackCatalog = fallbackCatalog.filter((p) => (p.averageRating || 5.0) >= rating);
      }
      if (sort === 'priceAsc') {
        fallbackCatalog.sort((a, b) => (a.minPrice || 0) - (b.minPrice || 0));
      } else if (sort === 'priceDesc') {
        fallbackCatalog.sort((a, b) => (b.minPrice || 0) - (a.minPrice || 0));
      } else {
        if (shouldShuffle || (!catId && !kw && range === 'all' && rating === 0)) {
          fallbackCatalog = shuffleArray(fallbackCatalog);
        } else {
          fallbackCatalog.sort((a, b) => (b.id || 0) - (a.id || 0));
        }
      }

      const total = fallbackCatalog.length;
      const computedTotalPages = Math.max(1, Math.ceil(total / size));
      const validPage = Math.min(page, computedTotalPages - 1);
      const start = Math.max(0, validPage * size);
      const pagedItems = fallbackCatalog.slice(start, start + size);

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

  // Bắt sự kiện Return Redirect từ cổng thanh toán PayOS / MB Bank hoặc VNPay
  useEffect(() => {
    try {
      const searchParams = new URLSearchParams(window.location.search);

      // Nếu người dùng quay lại từ PayOS (có tham số payos_status hoặc orderId/orderCode)
      if (
        searchParams.has('payos_status') ||
        searchParams.has('orderCode') ||
        (searchParams.has('orderId') && !searchParams.has('vnp_ResponseCode'))
      ) {
        setCurrentView('checkout');
        return;
      }

      // Xử lý fallback nếu có phản hồi từ VNPay cũ
      if (searchParams.has('vnp_ResponseCode')) {
        const responseCode = searchParams.get('vnp_ResponseCode');
        const isSuccess = responseCode === '00';
        const txnRef = searchParams.get('vnp_TxnRef') || '';
        const rawAmount = searchParams.get('vnp_Amount') || '0';
        const amount = Number(rawAmount) / 100;
        const bankCode = searchParams.get('vnp_BankCode') || '';
        const orderInfo = searchParams.get('vnp_OrderInfo') || '';

        setVnpayResult({
          isOpen: true,
          success: isSuccess,
          responseCode,
          txnRef,
          amount,
          bankCode,
          orderInfo,
          message: isSuccess
            ? 'Giao dịch thanh toán qua VNPay thành công! Đơn hàng của bạn đã được chuyển sang trạng thái xử lý.'
            : 'Thanh toán qua VNPay không thành công hoặc giao dịch đã bị hủy bởi người dùng.'
        });

        // Xóa query param trên thanh địa chỉ URL để tránh lặp lại khi refresh
        window.history.replaceState({}, document.title, window.location.pathname);
      }
    } catch (e) {
      console.warn('Lỗi phân tích URL Return thanh toán:', e);
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
    // Chuyển sang trang kết quả tìm kiếm chuyên biệt với query params trong URL
    handleNavigateView('search', { extra: { kw, cat: selectedCategory } });
  };

  const handleSelectProduct = (prod) => {
    setSelectedProduct(prod);
    handleNavigateView('product-detail', { extra: { id: prod?.id } });
  };

  const handleSelectShop = (shop) => {
    setSelectedShop(shop);
    handleNavigateView('shop-detail', { extra: { shopId: shop?.id || shop?.shopId } });
  };

  const handleBackToStorefront = (options = {}) => {
    const shuffle = options?.shuffle !== undefined ? options.shuffle : true;
    setSelectedProduct(null);
    setSelectedShop(null);
    setSelectedCategory(null);
    setSearchKeyword('');
    setSortBy('newest');
    setPriceRange('all');
    setMinRating(0);
    setCurrentPage(0);
    handleNavigateView('storefront');
    fetchProducts({
      catId: null,
      kw: '',
      sort: 'newest',
      range: 'all',
      rating: 0,
      page: 0,
      shuffle,
    });
  };

  const handleResetFilters = () => {
    setSelectedCategory(null);
    setSearchKeyword('');
    setSortBy('newest');
    setPriceRange('all');
    setMinRating(0);
    setCurrentPage(0);
    handleNavigateView('storefront');
    fetchProducts({
      catId: null,
      kw: '',
      sort: 'newest',
      range: 'all',
      rating: 0,
      page: 0,
      shuffle: true,
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
  const activeFilterCount =
    (selectedCategory ? 1 : 0) +
    (priceRange !== 'all' ? 1 : 0) +
    (minRating > 0 ? 1 : 0);

  // Hiển thị 1 hàng danh mục (6 danh mục) khi chưa bấm "Xem tất cả ngành hàng"
  const displayedCategories = showAllCategories ? categories : categories.slice(0, 6);

  // GSAP ScrollTrigger Stagger Entrance Animations for Storefront
  useGSAP(() => {
    if (currentView !== 'storefront' || !storefrontRef.current) return;

    // Staggered entrance for category dock cards
    const catContainer = storefrontRef.current.querySelector('.category-showcase-container');
    const catCards = storefrontRef.current.querySelectorAll('.category-card-item');
    if (catContainer && catCards.length > 0) {
      gsap.fromTo(
        catCards,
        { opacity: 0, y: 28 },
        {
          opacity: 1,
          y: 0,
          duration: 0.55,
          stagger: 0.05,
          ease: 'power2.out',
          clearProps: 'transform',
          scrollTrigger: {
            trigger: catContainer,
            start: 'top 88%',
            once: true,
          },
        }
      );
    }

    // ScrollTrigger Catalog Cascade with noticeable upward glide & clearProps
    const prodGrid = storefrontRef.current.querySelector('#catalog-product-grid');
    const prodCards = storefrontRef.current.querySelectorAll('.product-card-item');
    if (prodGrid && prodCards.length > 0) {
      gsap.fromTo(
        prodCards,
        { opacity: 0, y: 32 },
        {
          opacity: 1,
          y: 0,
          duration: 0.55,
          stagger: 0.05,
          ease: 'power2.out',
          clearProps: 'transform',
          scrollTrigger: {
            trigger: prodGrid,
            start: 'top 85%',
            once: true,
          },
        }
      );
    }

    // Re-sync ScrollTrigger after async products finish rendering
    ScrollTrigger.refresh();
  }, { scope: storefrontRef, dependencies: [currentView, showAllCategories, products.length, loadingProducts] });

  // GSAP Smooth View Transition across navigation changes (Apple/Linear-grade fluid curve)
  useGSAP(() => {
    if (mainViewRef.current) {
      gsap.fromTo(
        mainViewRef.current,
        { opacity: 0, y: 22 },
        { opacity: 1, y: 0, duration: 0.55, ease: 'power3.out', clearProps: 'transform' }
      );
    }
  }, { scope: mainViewRef, dependencies: [currentView, selectedCategory] });

  // VIEW RIÊNG BIỆT 1: DEDICATED FULL-PAGE LOGIN VIEW
  if (currentView === 'login') {
    return (
      <LoginPage
        onBackToHome={() => handleNavigateView('storefront')}
        onLoginSuccess={(targetView) => {
          const savedRedirect = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('quickshop_auth_redirect') : null;
          if (savedRedirect && ['admin', 'seller', 'shipper', 'storefront', 'cart', 'orders', 'profile', 'checkout', 'product-detail'].includes(savedRedirect)) {
            sessionStorage.removeItem('quickshop_auth_redirect');
            handleNavigateView(savedRedirect, { replace: true });
          } else {
            handleNavigateView(targetView || 'storefront', { replace: true });
          }
        }}
      />
    );
  }

  // VIEW RIÊNG BIỆT 2: CỔNG QUẢN TRỊ ADMIN (FULL-PAGE STANDALONE ADMIN PORTAL)
  if (currentView === 'admin') {
    if (!isAdmin) {
      return (
        <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4 font-sans">
          <div className="max-w-md w-full bg-white rounded-3xl p-8 border border-slate-200 text-center shadow-xl space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-rose-100 text-rose-600 mx-auto flex items-center justify-center">
              <ShieldAlert className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-bold text-slate-900">Từ chối truy cập (403 Forbidden)</h2>
            <p className="text-xs text-slate-500 leading-relaxed">
              Bạn cần đăng nhập bằng tài khoản có vai trò Quản Trị Viên (ROLE_ADMIN) để truy cập Cổng Quản Trị Hệ Thống.
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <button
                onClick={() => handleNavigateView('storefront')}
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Quay lại Trang Mua Sắm
              </button>
              <button
                onClick={() => handleNavigateView('login')}
                className="px-4 py-2.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Đăng nhập Admin
              </button>
            </div>
          </div>
        </div>
      );
    }
    return (
      <AdminPortal
        categories={categories}
        onCategoriesUpdated={fetchCategories}
        onBackToStorefront={() => handleNavigateView('storefront')}
      />
    );
  }

  // VIEW RIÊNG BIỆT 3: KÊNH NGƯỜI BÁN (FULL-PAGE STANDALONE SELLER CENTRE)
  if (currentView === 'seller') {
    if (!isSeller) {
      return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4 font-sans">
          <div className="max-w-md w-full bg-white rounded-3xl p-8 border border-slate-200 text-center shadow-xl space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-amber-100 text-amber-600 mx-auto flex items-center justify-center">
              <Store className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-bold text-slate-900">Yêu cầu quyền Người Bán</h2>
            <p className="text-xs text-slate-500 leading-relaxed">
              Bạn cần đăng nhập bằng tài khoản Chủ Gian Hàng (ROLE_SELLER) để quản lý sản phẩm và gian hàng.
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <button
                onClick={() => handleNavigateView('storefront')}
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Quay lại Trang Mua Sắm
              </button>
              <button
                onClick={() => handleNavigateView('login')}
                className="px-4 py-2.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Đăng nhập Người Bán
              </button>
            </div>
          </div>
        </div>
      );
    }
    return (
      <SellerPortal
        categories={categories}
        onProductCreated={() => fetchProducts({ page: 0 })}
        onBackToStorefront={() => handleNavigateView('storefront')}
      />
    );
  }

  // VIEW RIÊNG BIỆT 4: KÊNH GIAO VẬN SHIPPER (FULL-PAGE STANDALONE SHIPPER DISPATCH)
  if (currentView === 'shipper') {
    if (!isShipper) {
      return (
        <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4 font-sans">
          <div className="max-w-md w-full bg-white rounded-3xl p-8 border border-slate-200 text-center shadow-xl space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-blue-100 text-blue-600 mx-auto flex items-center justify-center">
              <Truck className="w-8 h-8" />
            </div>
            <h2 className="text-xl font-bold text-slate-900">Yêu cầu quyền Tài Xế Shipper</h2>
            <p className="text-xs text-slate-500 leading-relaxed">
              Bạn cần đăng nhập bằng tài khoản Bưu Tá / Giao Vận (ROLE_SHIPPER) để truy cập cổng điều phối giao hàng.
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <button
                onClick={() => handleNavigateView('storefront')}
                className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Quay lại Trang Mua Sắm
              </button>
              <button
                onClick={() => handleNavigateView('login')}
                className="px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold shadow-md transition"
              >
                Đăng nhập Shipper
              </button>
            </div>
          </div>
        </div>
      );
    }
    return (
      <ShipperPortal
        onBackToHome={() => handleNavigateView('storefront')}
      />
    );
  }

  return (
    <div className="min-h-screen bg-slate-50/70 text-slate-800 font-sans flex flex-col relative">
      {/* Sleek Top Brand Progress Bar on Page Mount */}
      <div className="fixed top-0 left-0 right-0 h-1 z-50 bg-gradient-to-r from-orange-500 via-amber-400 to-orange-600 animate-top-progress pointer-events-none" />

      {/* 1. Universal Top Navigation */}
      <Navbar
        currentView={currentView}
        setCurrentView={handleNavigateView}
        searchKeyword={searchKeyword}
        setSearchKeyword={setSearchKeyword}
        onSearchSubmit={handleSearchSubmit}
        selectedCategory={selectedCategory}
        setSelectedCategory={(catId) => {
          setSelectedCategory(catId);
          handleNavigateView('storefront');
        }}
        categories={categories}
        onOpenCart={() => handleNavigateView('cart')}
        onOpenAuthModal={() => handleNavigateView('login')}
        onNavigateToLogin={() => handleNavigateView('login')}
        onGoToStorefront={() => handleBackToStorefront({ shuffle: true })}
      />

      {/* 2. Main Content Views */}
      <main ref={mainViewRef} className="flex-1 max-w-7xl mx-auto w-full px-3 sm:px-6 py-4 space-y-6">

        {/* VIEW 1: PRODUCT DETAIL PAGE (CHUYÊN BIỆT NHƯ SHOPEE VÀ TIKTOK SHOP) */}
        {currentView === 'product-detail' && selectedProduct && (
          <ProductDetailPage
            product={selectedProduct}
            allProducts={products}
            onBack={() => {
              setSelectedProduct(null);
              handleNavigateView('storefront');
            }}
            onSelectRelatedProduct={handleSelectProduct}
            onSelectShop={handleSelectShop}
            onBuyNow={(buyItem) => {
              if (buyItem) {
                setCheckoutItems([buyItem]);
              }
              handleNavigateView('checkout');
            }}
            onRequireAuth={() => handleRequireAuth('product-detail')}
          />
        )}

        {/* VIEW: DEDICATED CART PAGE (TRANG GIỎ HÀNG CHUẨN SHOPEE - CHỈ HIỆN SẢN PHẨM TRONG GIỎ, TÍCH CHỌN MÓN ĐỂ THANH TOÁN) */}
        {currentView === 'cart' && (
          <CartPage
            onProceedToCheckout={(selected) => {
              setCheckoutItems(selected);
              handleNavigateView('checkout');
            }}
            onBack={() => {
              if (selectedProduct) {
                handleNavigateView('product-detail');
              } else {
                handleNavigateView('storefront');
              }
            }}
            onSelectProduct={handleSelectProduct}
            onRequireAuth={() => handleRequireAuth('cart')}
          />
        )}

        {/* VIEW: DEDICATED CHECKOUT PAGE (TRANG THANH TOÁN CÁC MÓN ĐÃ TÍCH CHỌN) */}
        {currentView === 'checkout' && (
          <CheckoutPage
            checkoutItems={checkoutItems}
            onBack={() => {
              handleNavigateView('cart');
            }}
            onSelectProduct={handleSelectProduct}
            onRequireAuth={() => handleRequireAuth('checkout')}
            onViewOrders={() => {
              handleNavigateView('orders');
            }}
          />
        )}

        {/* VIEW: USER PROFILE (HỒ SƠ NGƯỜI DÙNG, ĐỔI MẬT KHẨU, SỔ ĐỊA CHỈ) */}
        {currentView === 'profile' && (
          <UserProfilePage
            onBackToHome={() => {
              handleNavigateView('storefront');
            }}
            onViewOrders={() => {
              handleNavigateView('orders');
            }}
          />
        )}

        {/* VIEW: ORDER HISTORY & REAL-TIME SHIPPER TRACKING (THEO DÕI ĐƠN HÀNG & LỊCH SỬ MUA HÀNG) */}
        {currentView === 'orders' && (
          <OrderHistoryPage
            onBackToHome={() => {
              handleNavigateView('storefront');
            }}
            onBuyAgain={() => {
              handleNavigateView('cart');
            }}
          />
        )}

        {/* VIEW: FLASH SALE DEDICATED PAGE (TRANG DEAL SỐC CHUYÊN BIỆT) */}
        {currentView === 'flash-sale' && (
          <FlashSalePage
            products={products}
            onBack={() => handleNavigateView('storefront')}
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
                handleNavigateView('product-detail');
              } else {
                handleNavigateView('storefront');
              }
            }}
            onSelectProduct={handleSelectProduct}
            onRequireAuth={() => handleRequireAuth('shop-detail')}
          />
        )}

        {/* VIEW 2: DEDICATED SEARCH RESULTS PAGE (CHỈ HIỂN THỊ CÁC SẢN PHẨM CẦN TÌM) */}
        {currentView === 'search' && (
          <div className="space-y-6 animate-in fade-in duration-200">
            {/* Search Header Banner */}
            <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
              <div className="flex items-center gap-3">
                <button
                  onClick={() => handleBackToStorefront({ shuffle: true })}
                  className="p-2 sm:px-3.5 sm:py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-semibold shadow-sm active:scale-95"
                  title="Quay lại Trang chủ"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Trang chủ Mua sắm</span>
                </button>

                <div>
                  <h2 className="text-base sm:text-xl font-black text-slate-900 flex items-center gap-2">
                    <span>Kết quả tìm kiếm cho:</span>
                    <span className="text-orange-600 font-bold italic">
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
                  className={`px-3 py-1.5 rounded-xl font-semibold transition ${sortBy === 'newest' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                >
                  Mới nhất
                </button>
                <button
                  onClick={() => setSortBy('priceAsc')}
                  className={`px-3 py-1.5 rounded-xl font-semibold transition ${sortBy === 'priceAsc' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                >
                  Giá thấp đến cao
                </button>
                <button
                  onClick={() => setSortBy('priceDesc')}
                  className={`px-3 py-1.5 rounded-xl font-semibold transition ${sortBy === 'priceDesc' ? 'bg-orange-600 text-white shadow-sm' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                    }`}
                >
                  Giá cao đến thấp
                </button>
              </div>

              <button
                onClick={() => handleBackToStorefront({ shuffle: true })}
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
                  onClick={() => handleBackToStorefront({ shuffle: true })}
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
                      onRequireAuth={() => handleRequireAuth('search')}
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
          <div ref={storefrontRef} className="space-y-6">
            {/* Lucian Labs-Style Asymmetrical 3-Card Bento Hero Grid */}
            <HeroBanner
              onSelectCategory={(catId) => setSelectedCategory(catId)}
              onSelectProduct={handleSelectProduct}
              onNavigateFlashSale={() => handleNavigateView('flash-sale')}
              products={products}
            />

            {/* Flash Sale Deal Shocker Section */}
            <FlashSaleSection
              products={products}
              onSelectProduct={handleSelectProduct}
              onViewAllDeals={() => {
                handleNavigateView('flash-sale');
              }}
            />

            {/* Architectural Category Dock (Thanh dock ngành hàng bo tròn, hover lift, active glow) */}
            <div className="category-showcase-container bg-white/85 backdrop-blur-xl rounded-3xl p-5 border border-slate-200/80 shadow-sm space-y-3.5">
              <div className="flex justify-between items-center">
                <div className="space-y-0.5">
                  <h3 className="font-extrabold text-sm text-slate-900 uppercase tracking-wide flex items-center gap-2">
                    <Sparkles className="w-4 h-4 text-orange-600" />
                    <span>Danh Mục Sàn Nổi Bật</span>
                  </h3>
                  <p className="text-[11px] text-slate-400">Chọn nhanh ngành hàng yêu thích với bộ lọc tức thì</p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowAllCategories(!showAllCategories)}
                  className="text-xs text-orange-600 font-bold cursor-pointer hover:underline flex items-center gap-1 transition active:scale-95 px-2.5 py-1 rounded-xl hover:bg-orange-50"
                >
                  <span>{showAllCategories ? 'Thu gọn' : 'Xem tất cả ngành hàng'}</span>
                  <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${showAllCategories ? 'rotate-180' : ''}`} />
                </button>
              </div>

              <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 pt-1">
                {displayedCategories.map((c) => {
                  const { Icon, bg, borderHover } = getCategoryIconMeta(c);
                  const isSelected = selectedCategory === c.id;
                  return (
                    <button
                      key={c.id}
                      onClick={() => setSelectedCategory(isSelected ? null : c.id)}
                      className={`category-card-item p-3 sm:p-4 rounded-2xl border text-center transition-all duration-200 flex flex-col items-center justify-center space-y-2.5 group cursor-pointer hover:-translate-y-1 hover:shadow-lg hover:shadow-orange-500/5 ${
                        isSelected
                          ? 'border-orange-500 bg-orange-50/90 text-orange-950 shadow-md shadow-orange-500/10 font-bold ring-4 ring-orange-400/20'
                          : `border-slate-200/70 ${borderHover} hover:shadow-md bg-white text-slate-700`
                      }`}
                    >
                      <div className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-200 shadow-sm ${
                        isSelected ? 'bg-orange-600 text-white shadow-orange-400/30' : bg
                      }`}>
                        <Icon className="w-6 h-6 stroke-[1.8] transition-transform duration-200 group-hover:scale-110" />
                      </div>
                      <span className={`text-xs font-semibold line-clamp-1 transition-colors ${
                        isSelected ? 'text-orange-950 font-bold' : 'group-hover:text-orange-600'
                      }`}>
                        {c.name}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Main Product Catalog Section with Top Filter Bar */}
            <div className="space-y-4" id="catalog-product-grid">
              {/* Thông báo lỗi kết nối nếu có */}
              <ApiErrorBanner
                error={apiError}
                isRetrying={isRetrying}
                fallbackActive={isUsingFallback}
                onRetry={handleRetryApi}
                onDismiss={() => setApiError(null)}
              />

              {/* Sort & Filter Toolbar */}
              <div className="bg-white rounded-2xl border border-slate-200/90 p-3 sm:px-5 shadow-sm space-y-3">
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-3 text-xs">
                  {/* Left: Nút Bộ lọc & Các nút sắp xếp */}
                  <div className="flex flex-wrap items-center gap-2">
                    {/* Nút mở Modal Bộ lọc căn giữa */}
                    <button
                      onClick={() => setIsFilterOpen(true)}
                      className={`px-3.5 py-2 rounded-xl font-bold flex items-center gap-2 transition shadow-sm ${
                        isFilterOpen || activeFilterCount > 0
                          ? 'bg-orange-600 text-white shadow-orange-200 hover:bg-orange-700'
                          : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                      }`}
                      title="Mở bảng bộ lọc sản phẩm căn giữa màn hình"
                    >
                      <SlidersHorizontal className="w-4 h-4" />
                      <span>Bộ lọc tìm kiếm</span>
                      {activeFilterCount > 0 && (
                        <span className="w-5 h-5 rounded-full bg-white text-orange-600 text-[11px] font-black flex items-center justify-center shadow-xs">
                          {activeFilterCount}
                        </span>
                      )}
                    </button>

                    <div className="h-5 w-px bg-slate-200 hidden sm:block mx-1"></div>

                    <span className="text-slate-500 font-medium hidden sm:inline">Sắp xếp:</span>
                    <button
                      onClick={() => setSortBy('newest')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'newest' ? 'bg-orange-50 text-orange-600 border border-orange-200' : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      Mới nhất
                    </button>
                    <button
                      onClick={() => setSortBy('priceAsc')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'priceAsc' ? 'bg-orange-50 text-orange-600 border border-orange-200' : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      Giá thấp đến cao
                    </button>
                    <button
                      onClick={() => setSortBy('priceDesc')}
                      className={`px-3 py-1.5 rounded-xl font-semibold transition ${
                        sortBy === 'priceDesc' ? 'bg-orange-50 text-orange-600 border border-orange-200' : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      Giá cao đến thấp
                    </button>

                    <button
                      onClick={() => {
                        setCurrentPage(0);
                        fetchProducts({ page: 0, shuffle: true });
                      }}
                      className="px-3 py-1.5 rounded-xl font-semibold transition bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200/80 flex items-center gap-1.5 cursor-pointer active:scale-95"
                      title="Làm mới và khám phá các sản phẩm gợi ý ngẫu nhiên khác nhau"
                    >
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>Đổi gợi ý mới</span>
                    </button>
                  </div>

                  {/* Right: Số lượng sản phẩm & Nút xóa lọc nhanh */}
                  <div className="flex items-center gap-3 text-slate-500 font-medium text-[11px] self-end md:self-auto">
                    {activeFilterCount > 0 && (
                      <button
                        onClick={handleResetFilters}
                        className="text-orange-600 hover:text-orange-700 font-semibold flex items-center gap-1 transition"
                        title="Xóa tất cả bộ lọc đang chọn"
                      >
                        <RotateCcw className="w-3 h-3" />
                        Xóa lọc
                      </button>
                    )}
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

                {/* Hàng nhãn tiêu chí lọc đang hoạt động */}
                {activeFilterCount > 0 && (
                  <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-2 text-xs">
                    <span className="text-slate-400 text-[11px]">Đang lọc:</span>
                    {selectedCategory && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-orange-50 text-orange-700 border border-orange-200/80 font-medium text-[11px]">
                        Danh mục: {categories.find((c) => c.id === selectedCategory)?.name || selectedCategory}
                        <button
                          onClick={() => setSelectedCategory(null)}
                          className="hover:text-red-500 transition"
                          title="Bỏ lọc danh mục này"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    )}
                    {priceRange !== 'all' && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-orange-50 text-orange-700 border border-orange-200/80 font-medium text-[11px]">
                        Giá:{' '}
                        {priceRange === 'under1m'
                          ? 'Dưới 1 triệu'
                          : priceRange === '1m-5m'
                          ? '1 - 5 triệu'
                          : priceRange === '5m-20m'
                          ? '5 - 20 triệu'
                          : 'Trên 20 triệu'}
                        <button
                          onClick={() => setPriceRange('all')}
                          className="hover:text-red-500 transition"
                          title="Bỏ lọc khoảng giá này"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    )}
                    {minRating > 0 && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-orange-50 text-orange-700 border border-orange-200/80 font-medium text-[11px]">
                        Đánh giá: Từ {minRating} sao
                        <button
                          onClick={() => setMinRating(0)}
                          className="hover:text-red-500 transition"
                          title="Bỏ lọc số sao này"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* MODAL BỘ LỌC CĂN GIỮA MÀN HÌNH (CENTERED MODAL DIALOG - KHÔNG CUỐN THEO TRANG) */}
              {isFilterOpen && typeof document !== 'undefined' && createPortal(
                <div
                  className="fixed inset-0 z-[9999] flex items-center justify-center p-3 sm:p-4"
                  data-lenis-prevent
                  onWheel={(e) => e.stopPropagation()}
                >
                  {/* Backdrop phủ đen mờ toàn màn hình khóa cuộn trang */}
                  <div
                    className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity"
                    onClick={() => setIsFilterOpen(false)}
                  />

                  {/* Hộp Modal Căn Giữa Màn Hình */}
                  <div
                    className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-slate-200 z-10 overflow-hidden flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-150"
                    onClick={(e) => e.stopPropagation()}
                    data-lenis-prevent
                    onWheel={(e) => e.stopPropagation()}
                  >
                    {/* Modal Header */}
                    <div className="p-4 sm:p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/70">
                      <div className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shadow-xs">
                          <Filter className="w-4 h-4 text-orange-600" />
                        </div>
                        <span>Tùy chọn bộ lọc chi tiết</span>
                        {activeFilterCount > 0 && (
                          <span className="px-2.5 py-0.5 rounded-full bg-orange-100 text-orange-700 text-xs font-bold">
                            Đã chọn {activeFilterCount} tiêu chí
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-3">
                        {activeFilterCount > 0 && (
                          <button
                            onClick={handleResetFilters}
                            className="text-slate-500 hover:text-orange-600 flex items-center gap-1 transition text-xs font-semibold"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                            Xóa tất cả lọc
                          </button>
                        )}
                        <button
                          onClick={() => setIsFilterOpen(false)}
                          className="text-slate-400 hover:text-slate-700 p-1.5 rounded-xl hover:bg-slate-200/60 transition"
                          title="Đóng bảng lọc"
                        >
                          <X className="w-5 h-5" />
                        </button>
                      </div>
                    </div>

                    {/* Modal Body - 3 Cột Lọc Thông Minh */}
                    <div data-lenis-prevent className="p-5 sm:p-6 overflow-y-auto grid grid-cols-1 md:grid-cols-3 gap-6 text-xs">
                      {/* Cột 1: Theo Danh Mục */}
                      <div className="space-y-3">
                        <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5 pb-1 border-b border-slate-100">
                          <Store className="w-3.5 h-3.5 text-orange-500" />
                          Theo Danh Mục
                        </div>
                        <div data-lenis-prevent className="max-h-64 overflow-y-auto space-y-1 pr-1.5 scrollbar-thin">
                          <button
                            onClick={() => setSelectedCategory(null)}
                            className={`w-full text-left px-3 py-2 rounded-xl transition flex justify-between items-center ${
                              selectedCategory === null
                                ? 'bg-orange-600 text-white font-bold shadow-xs'
                                : 'hover:bg-slate-100 text-slate-700 bg-slate-50/70'
                            }`}
                          >
                            <span>Tất cả danh mục</span>
                            {selectedCategory === null && <CheckCircle2 className="w-3.5 h-3.5" />}
                          </button>
                          {categories.map((c) => (
                            <button
                              key={c.id}
                              onClick={() => setSelectedCategory(c.id)}
                              className={`w-full text-left py-2 px-3 rounded-xl transition flex justify-between items-center ${
                                c.parentId ? 'pl-6 text-[11px]' : 'font-semibold'
                              } ${
                                selectedCategory === c.id
                                  ? 'bg-orange-600 text-white font-bold shadow-xs'
                                  : 'hover:bg-slate-100 text-slate-700 bg-slate-50/70'
                              }`}
                            >
                              <span className="truncate">{c.parentId ? `↳ ${c.name}` : c.name}</span>
                              {selectedCategory === c.id ? (
                                <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                              ) : c.productCount > 0 ? (
                                <span className="text-[10px] text-slate-400">({c.productCount})</span>
                              ) : null}
                            </button>
                          ))}
                        </div>
                      </div>

                      {/* Cột 2: Khoảng Giá */}
                      <div className="space-y-3">
                        <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5 pb-1 border-b border-slate-100">
                          <Sparkles className="w-3.5 h-3.5 text-orange-500" />
                          Khoảng Giá (VND)
                        </div>
                        <div className="space-y-1.5">
                          {[
                            { id: 'all', label: 'Tất cả mức giá' },
                            { id: 'under1m', label: 'Dưới 1.000.000đ' },
                            { id: '1m-5m', label: '1.000.000đ - 5.000.000đ' },
                            { id: '5m-20m', label: '5.000.000đ - 20.000.000đ' },
                            { id: 'above20m', label: 'Trên 20.000.000đ' },
                          ].map((pr) => {
                            const isSelected = priceRange === pr.id;
                            return (
                              <button
                                key={pr.id}
                                onClick={() => setPriceRange(pr.id)}
                                className={`w-full text-left px-3 py-2.5 rounded-xl transition flex justify-between items-center ${
                                  isSelected
                                    ? 'bg-orange-600 text-white font-bold shadow-xs'
                                    : 'hover:bg-slate-100 text-slate-700 bg-slate-50/70'
                                }`}
                              >
                                <span>{pr.label}</span>
                                {isSelected && <CheckCircle2 className="w-3.5 h-3.5" />}
                              </button>
                            );
                          })}
                        </div>
                      </div>

                      {/* Cột 3: Đánh Giá Sao */}
                      <div className="space-y-3">
                        <div className="font-bold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5 pb-1 border-b border-slate-100">
                          <Star className="w-3.5 h-3.5 text-amber-500 fill-amber-400" />
                          Đánh Giá Sao
                        </div>
                        <div className="space-y-1.5">
                          {[
                            { stars: 0, label: 'Tất cả đánh giá' },
                            { stars: 5, label: 'Từ 5 sao (Tuyệt đối)' },
                            { stars: 4, label: 'Từ 4 sao trở lên' },
                            { stars: 3, label: 'Từ 3 sao trở lên' },
                          ].map((r) => {
                            const isSelected = minRating === r.stars;
                            return (
                              <button
                                key={r.stars}
                                onClick={() => setMinRating(r.stars)}
                                className={`w-full text-left px-3 py-2.5 rounded-xl transition flex justify-between items-center ${
                                  isSelected
                                    ? 'bg-orange-600 text-white font-bold shadow-xs'
                                    : 'hover:bg-slate-100 text-slate-700 bg-slate-50/70'
                                }`}
                              >
                                <div className="flex items-center gap-2">
                                  {r.stars > 0 && (
                                    <div className="flex">
                                      {Array.from({ length: r.stars }).map((_, i) => (
                                        <Star
                                          key={i}
                                          className={`w-3.5 h-3.5 ${
                                            isSelected ? 'fill-white text-white' : 'fill-amber-400 text-amber-400'
                                          }`}
                                        />
                                      ))}
                                    </div>
                                  )}
                                  <span>{r.label}</span>
                                </div>
                                {isSelected && <CheckCircle2 className="w-3.5 h-3.5" />}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    </div>

                    {/* Modal Footer */}
                    <div className="p-4 sm:p-5 border-t border-slate-100 bg-slate-50/70 flex flex-col sm:flex-row justify-between items-center gap-3">
                      <div className="text-xs text-slate-500">
                        Tìm thấy <strong className="text-slate-800 font-bold">{totalElements}</strong> sản phẩm phù hợp
                      </div>
                      <div className="flex items-center gap-3 w-full sm:w-auto">
                        <button
                          onClick={() => setIsFilterOpen(false)}
                          className="flex-1 sm:flex-initial px-4 py-2.5 rounded-xl text-slate-600 hover:bg-slate-200/60 font-semibold text-xs transition"
                        >
                          Đóng
                        </button>
                        <button
                          onClick={() => setIsFilterOpen(false)}
                          className="flex-1 sm:flex-initial px-6 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl text-xs shadow-md shadow-orange-500/20 transition active:scale-95"
                        >
                          Áp dụng bộ lọc ({totalElements})
                        </button>
                      </div>
                    </div>
                  </div>
                </div>,
                document.body
              )}

              {/* Product Grid - Rộng full width 4 cột trên desktop */}
              {loadingProducts ? (
                <div className="space-y-4">
                  <div className="py-6 text-center space-y-2 bg-white rounded-2xl border border-slate-200/80 shadow-sm">
                    <div className="w-7 h-7 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                    <div className="text-xs text-slate-500 font-medium">Đang tải sản phẩm từ Catalog Service & Redis Cache...</div>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-3 sm:gap-4">
                    {Array.from({ length: 8 }).map((_, i) => (
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
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-3 sm:gap-4">
                    {filteredProducts.map((p) => (
                      <div key={p.id} className="product-card-item h-full">
                        <ProductCard
                          product={p}
                          onSelect={handleSelectProduct}
                          onSelectShop={handleSelectShop}
                          onRequireAuth={() => handleRequireAuth('storefront')}
                        />
                      </div>
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
              © 2026 QuickShop. Phát triển bởi Nhóm 06 (Lê Đức Anh, Triệu Quốc Bình, Nguyễn Văn Kiên).
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
        onRequireAuth={() => handleRequireAuth('cart')}
      />

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onLoginSuccess={(targetView) => handleAuthSuccess(targetView)}
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
                handleNavigateView('storefront');
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
