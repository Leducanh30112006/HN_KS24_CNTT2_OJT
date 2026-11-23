import React, { useState, useEffect, useRef } from 'react';
import { 
  ShoppingBag, Search, Bell, ShoppingCart, User, Store, ShieldCheck, 
  HelpCircle, Smartphone, ChevronDown, LogOut, CheckCircle, Tag, Truck, Package, Sparkles, MessageSquare
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { useNotificationStore } from '../stores/useNotificationStore';
import { gsap, useGSAP } from '../lib/gsap';
import { AnimatedPresence, AnimatedNumber } from './motion/MotionPrimitives';
import { LucianButton } from './ui/LucianPrimitives';
import { useClickOutside } from '../hooks/useClickOutside';
import ThemeToggle from './ui/ThemeToggle';

export default function Navbar({ 
  currentView, 
  setCurrentView, 
  searchKeyword, 
  setSearchKeyword, 
  onSearchSubmit,
  selectedCategory,
  setSelectedCategory,
  categories = [],
  onOpenCart,
  onOpenAuthModal,
  onNavigateToLogin,
  onGoToStorefront,
  onOpenChatWithShop,
}) {
  const { user, isAuthenticated, logout } = useAuthStore();
  const totalCartCount = useCartStore((state) => state.getTotalCount());
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotificationStore();

  const [showNotifMenu, setShowNotifMenu] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [unreadChatCount, setUnreadChatCount] = useState(0);

  useEffect(() => {
    const updateUnread = () => {
      try {
        const raw = localStorage.getItem('quickshop_buyer_seller_chat_v2');
        const currentUserId = user?.userId ?? user?.id;
        if (raw && currentUserId) {
          const threads = JSON.parse(raw);
          const count = (threads || [])
            .filter((t) => String(t.userId) === String(currentUserId))
            .reduce((acc, t) => acc + (t.unreadByBuyer || 0), 0);
          setUnreadChatCount(count);
        } else {
          setUnreadChatCount(0);
        }
      } catch (e) {
        setUnreadChatCount(0);
      }
    };
    updateUnread();
    window.addEventListener('quickshop_chat_update', updateUnread);
    return () => window.removeEventListener('quickshop_chat_update', updateUnread);
  }, [user?.userId, user?.id]);
  const searchInputRef = useRef(null);
  const cartBadgeRef = useRef(null);
  const cartButtonRef = useRef(null);
  const navContainerRef = useRef(null);
  const prevCountRef = useRef(totalCartCount);

  // Refs for click outside handling on notifications and user menus
  const notifContainerRef = useRef(null);
  const notifButtonRef = useRef(null);
  const userContainerRef = useRef(null);
  const userButtonRef = useRef(null);

  useClickOutside(notifContainerRef, () => setShowNotifMenu(false), {
    enabled: showNotifMenu,
    ignoreRefs: [notifButtonRef],
    listenEscape: true,
  });

  useClickOutside(userContainerRef, () => setShowUserMenu(false), {
    enabled: showUserMenu,
    ignoreRefs: [userButtonRef],
    listenEscape: true,
  });

  // Initial Page-Load Reveal: Animate Navbar down with expo.out
  useGSAP(() => {
    if (navContainerRef.current) {
      gsap.fromTo(
        navContainerRef.current,
        { y: -20, opacity: 0 },
        { y: 0, opacity: 1, duration: 0.6, ease: 'expo.out', clearProps: 'transform' }
      );
    }
  });

  // Subtle GSAP Magnetic Hover Pull on Cart Pill (±4px physics)
  useGSAP(() => {
    const el = cartButtonRef.current;
    if (!el) return;
    const xTo = gsap.quickTo(el, 'x', { duration: 0.35, ease: 'power3.out' });
    const yTo = gsap.quickTo(el, 'y', { duration: 0.35, ease: 'power3.out' });

    const onMouseMove = (e) => {
      const rect = el.getBoundingClientRect();
      const x = (e.clientX - (rect.left + rect.width / 2)) * 0.18;
      const y = (e.clientY - (rect.top + rect.height / 2)) * 0.18;
      xTo(Math.max(-4, Math.min(4, x)));
      yTo(Math.max(-4, Math.min(4, y)));
    };

    const onMouseLeave = () => {
      xTo(0);
      yTo(0);
    };

    el.addEventListener('mousemove', onMouseMove);
    el.addEventListener('mouseleave', onMouseLeave);
    return () => {
      el.removeEventListener('mousemove', onMouseMove);
      el.removeEventListener('mouseleave', onMouseLeave);
    };
  });

  // Cart Badge Tactile Feedback: scale pop on item count increase
  useEffect(() => {
    if (totalCartCount > prevCountRef.current && cartBadgeRef.current) {
      gsap.fromTo(
        cartBadgeRef.current,
        { scale: 1.28 },
        { scale: 1, duration: 0.35, ease: 'back.out(2)' }
      );
    }
    prevCountRef.current = totalCartCount;
  }, [totalCartCount]);

  // Global Keyboard Shortcuts: '/' and 'Ctrl+K' / 'Cmd+K' to focus search
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      const activeEl = document.activeElement;
      const isEditable = activeEl && (
        activeEl.tagName === 'INPUT' ||
        activeEl.tagName === 'TEXTAREA' ||
        activeEl.tagName === 'SELECT' ||
        activeEl.isContentEditable ||
        activeEl.getAttribute('contenteditable') === 'true' ||
        activeEl.getAttribute('role') === 'textbox'
      );

      // Shortcut 1: Ctrl+K or Cmd+K
      const isCtrlK = (e.ctrlKey || e.metaKey) && (e.key === 'k' || e.key === 'K');
      // Shortcut 2: Slash '/' (only when NOT focused inside an editable field)
      const isSlash = e.key === '/' && !isEditable && !e.ctrlKey && !e.metaKey && !e.altKey;

      if (isCtrlK || isSlash) {
        e.preventDefault();
        if (searchInputRef.current) {
          searchInputRef.current.focus();
          searchInputRef.current.select();
        }
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => {
      window.removeEventListener('keydown', handleGlobalKeyDown);
    };
  }, []);

  const hotKeywords = ['iPhone 16 Pro', 'QuickBook Ultra M3', 'Áo Polo Cafe', 'Robot hút bụi', 'Kem chống nắng'];

  const handleGoToLogin = () => {
    if (onNavigateToLogin) {
      onNavigateToLogin();
    } else if (onOpenAuthModal) {
      onOpenAuthModal();
    } else {
      setCurrentView('login');
    }
  };

  const handleTriggerSearch = (e) => {
    if (e && e.preventDefault) e.preventDefault();
    if (onSearchSubmit) {
      onSearchSubmit(searchKeyword);
    } else if (setCurrentView) {
      setCurrentView('search');
    }
  };

  const handleSearchKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleTriggerSearch(e);
    }
  };

  const isSeller = user && (user.role === 'ROLE_SELLER' || user.role === 'SELLER');
  const isSellerOnly = isSeller && !user?.role?.includes('ADMIN');
  const isAdmin = user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN');
  const isShipper = user && (user.role === 'ROLE_SHIPPER' || user.role === 'SHIPPER');

  return (
    <>
      <header
        ref={navContainerRef}
        className="fixed top-2 sm:top-3.5 left-0 right-0 z-40 px-2.5 sm:px-6 w-full pointer-events-none transition-all"
      >
        <div className="max-w-7xl mx-auto flex flex-col gap-1.5 pointer-events-auto">
          {/* Main Navigation Bar - Floating Rounded Capsule Pill */}
          <nav className="rounded-2xl bg-white/85 dark:bg-[#07080D]/90 backdrop-blur-xl border border-slate-200/80 dark:border-white/10 shadow-lg shadow-slate-900/5 dark:shadow-black/50 px-3 sm:px-4 lg:px-5 py-2 sm:py-2.5 flex items-center justify-between gap-2 sm:gap-3 lg:gap-5 transition-all max-w-full overflow-x-clip">
          {/* Left: Brand Logo & Role Fast Switcher */}
          <div className="flex items-center gap-2 sm:gap-3 lg:gap-4 flex-shrink-0 min-w-0">
            <button
              type="button"
              onClick={() => {
                if (onGoToStorefront) {
                  onGoToStorefront();
                } else {
                  if (setSearchKeyword) setSearchKeyword('');
                  if (setSelectedCategory) setSelectedCategory(null);
                  setCurrentView('home');
                }
              }} 
              className="flex items-center gap-2 cursor-pointer group select-none text-left bg-transparent border-0 p-0"
            >
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-2xl bg-gradient-to-tr from-orange-500 via-rose-500 to-amber-500 flex items-center justify-center text-white shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform flex-shrink-0">
                <ShoppingBag className="w-5 h-5 sm:w-6 sm:h-6 stroke-[2.5]" />
              </div>
              <div>
                <div className="text-lg sm:text-xl lg:text-2xl font-black tracking-tight text-slate-900 dark:text-[#F4F0E8] flex items-center">
                  Quick<span className="text-orange-600 dark:text-orange-500">Shop</span>
                  <span className="ml-1 text-[11px] font-black uppercase tracking-wider bg-orange-100 dark:bg-orange-950/60 text-orange-700 dark:text-orange-400 px-1.5 py-0.5 rounded-md">
                    Mall
                  </span>
                </div>
                <div className="hidden lg:block text-[11px] text-slate-400 dark:text-slate-500 font-medium tracking-wide">
                  Flagship Storefront 2026
                </div>
              </div>
            </button>

            {/* Two-Way Tactile Switcher: Giới thiệu 3D <-> Mua sắm */}
            <div className="flex items-center p-0.5 sm:p-1 bg-slate-100/90 dark:bg-[#13141C] rounded-2xl border border-slate-200/80 dark:border-white/10 shadow-xs">
              <button
                type="button"
                onClick={() => setCurrentView('intro')}
                className={`px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
                  currentView === 'intro'
                    ? 'bg-slate-900 dark:bg-white/15 text-white shadow-sm ring-1 ring-slate-800 dark:ring-white/20'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-white/70 dark:hover:bg-white/10'
                }`}
                title="Trang giới thiệu 3D QuickShop"
              >
                <Sparkles className={`w-3.5 h-3.5 ${currentView === 'intro' ? 'text-amber-400' : 'text-slate-400'}`} />
                <span className="hidden xs:inline">Giới thiệu</span>
                <span className="xs:hidden">3D</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  if (onGoToStorefront) onGoToStorefront();
                  else setCurrentView('home');
                }}
                className={`px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
                  currentView === 'home' || currentView === 'storefront'
                    ? 'bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-sm ring-1 ring-orange-500'
                    : 'text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white hover:bg-white/70 dark:hover:bg-white/10'
                }`}
                title="Khám phá sàn mua sắm sản phẩm chính hãng"
              >
                <ShoppingBag className="w-3.5 h-3.5" />
                <span>Mua sắm</span>
              </button>
            </div>

            {/* Quick Role Navigation Pills */}
            <div className="hidden xl:flex items-center gap-1.5 pl-2 border-l border-slate-200/80 dark:border-white/10">
              {isAdmin && (
                <button
                  onClick={() => setCurrentView('admin')}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-[11px] font-bold transition shadow-xs active:scale-95"
                  title="Mở Cổng Quản Trị Admin Riêng Biệt"
                >
                  <ShieldCheck className="w-3 h-3 text-amber-400" />
                  <span>Admin</span>
                </button>
              )}

              {isSellerOnly && (
                <button
                  onClick={() => setCurrentView('seller')}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-[11px] font-bold transition shadow-xs active:scale-95"
                  title="Mở Kênh Người Bán Riêng Biệt"
                >
                  <Store className="w-3 h-3" />
                  <span>Kênh Bán</span>
                </button>
              )}

              {isShipper && (
                <button
                  onClick={() => setCurrentView('shipper')}
                  className="flex items-center gap-1 px-2.5 py-1 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-[11px] font-bold transition shadow-xs active:scale-95"
                  title="Mở Cổng Vận Chuyển Shipper Riêng Biệt"
                >
                  <Truck className="w-3 h-3" />
                  <span>Shipper</span>
                </button>
              )}

              {isAuthenticated && (
                <button
                  onClick={() => setCurrentView('orders')}
                  className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-[11px] font-semibold transition active:scale-95 ${
                    currentView === 'orders' ? 'bg-orange-50 text-orange-600 font-bold' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                  }`}
                  title="Tra cứu Lịch sử Đơn Mua"
                >
                  <Package className="w-3 h-3 text-slate-500" />
                  <span>Đơn Mua</span>
                </button>
              )}
            </div>
          </div>

          {/* Center: Command-Style Search Bar */}
          <div className="flex-1 max-w-xl hidden md:block min-w-0">
            <form onSubmit={handleTriggerSearch} className="relative flex items-center">
              <Search className="absolute left-3.5 w-4 h-4 text-slate-400 pointer-events-none" />
              <input
                ref={searchInputRef}
                type="text"
                aria-label="Tìm kiếm sản phẩm"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={handleSearchKeyPress}
                placeholder="Tìm kiếm hơn 10.000+ sản phẩm chính hãng..."
                className="w-full pl-10 pr-28 py-2 bg-slate-100/90 dark:bg-[#13141C] hover:bg-slate-100 dark:hover:bg-[#1A1B25] focus:bg-white dark:focus:bg-[#1A1B25] text-xs sm:text-sm text-slate-900 dark:text-[#F4F0E8] rounded-xl border border-transparent dark:border-white/10 focus:border-orange-500 dark:focus:border-amber-400 focus:ring-2 focus:ring-orange-500/15 outline-none transition-all placeholder:text-slate-400 dark:placeholder:text-slate-500"
              />
              <div className="absolute right-1 flex items-center gap-1.5">
                <span className="hidden xl:inline-flex items-center gap-0.5 select-none pointer-events-none mr-1">
                  <kbd className="px-1.5 py-0.5 text-[10px] font-mono text-slate-400 dark:text-slate-500 bg-white/90 dark:bg-[#1A1B25] rounded border border-slate-200 dark:border-white/10 shadow-2xs">Ctrl K</kbd>
                </span>
                <button
                  type="submit"
                  aria-label="Tìm kiếm ngay"
                  className="px-3.5 py-1.5 bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white font-bold text-xs rounded-lg shadow-sm shadow-orange-500/25 active:scale-95 transition flex items-center gap-1 cursor-pointer select-none"
                  title="Tìm kiếm"
                >
                  <Search className="w-3.5 h-3.5 stroke-[2.5]" />
                  <span>Tìm</span>
                </button>
              </div>
            </form>
          </div>

          {/* Right: Messenger, Notification, Theme Toggle, Tactile Cart Pill, User Profile */}
          <div className="flex items-center gap-1.5 sm:gap-2 flex-shrink-0">
            {/* Messenger Chat Button (Hiển thị nổi bật, không bị ẩn) */}
            <button
              type="button"
              onClick={() => {
                if (onOpenChatWithShop) onOpenChatWithShop();
                else if (setCurrentView) setCurrentView('chat');
              }}
              aria-label="Mở tin nhắn Messenger với Người bán"
              className={`btn-icon p-2 sm:p-2.5 rounded-xl transition relative cursor-pointer active:scale-90 flex items-center gap-1.5 ${
                currentView === 'chat'
                  ? 'bg-orange-50 dark:bg-orange-950/40 text-orange-600 dark:text-orange-400 font-bold ring-2 ring-orange-500/20'
                  : 'text-slate-600 dark:text-slate-300 hover:text-orange-600 dark:hover:text-amber-400 hover:bg-orange-50 dark:hover:bg-white/10'
              }`}
              title="Tin nhắn Messenger / Chat với Người bán"
            >
              <MessageSquare className="w-5 h-5 text-orange-600 dark:text-orange-400 stroke-[2.2]" />
              {unreadChatCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-4 h-4 px-1 bg-orange-600 text-white text-[10px] font-mono font-bold rounded-full flex items-center justify-center shadow-xs">
                  <AnimatedNumber value={unreadChatCount} />
                </span>
              )}
              <span className="hidden xl:inline text-xs font-bold text-slate-800 dark:text-slate-200">Mess</span>
            </button>
          {/* Notifications Dropdown */}
          <div className="relative" ref={notifContainerRef}>
            <button
              ref={notifButtonRef}
              onClick={() => setShowNotifMenu(!showNotifMenu)}
              aria-label="Mở thông báo"
              className="btn-icon p-2 sm:p-2.5 text-slate-600 dark:text-slate-300 hover:text-orange-600 dark:hover:text-amber-400 hover:bg-orange-50 dark:hover:bg-white/10 rounded-xl transition relative cursor-pointer active:scale-90"
              title="Thông báo"
            >
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center shadow-xs">
                  <AnimatedNumber value={unreadCount} />
                </span>
              )}
            </button>

            <AnimatedPresence show={showNotifMenu} mode="dropdown" className="absolute right-0 mt-2 w-80 z-50">
              <div className="bg-white dark:bg-[#13141C] rounded-2xl shadow-2xl border border-slate-200 dark:border-white/10 overflow-hidden">
                <div className="p-3 bg-gradient-to-r from-orange-50 to-rose-50 dark:from-[#1A1B25] dark:to-[#13141C] border-b border-slate-100 dark:border-white/5 flex justify-between items-center">
                  <div className="font-semibold text-xs text-slate-800 dark:text-[#F4F0E8] flex items-center gap-1.5">
                    <Bell className="w-3.5 h-3.5 text-orange-600 dark:text-amber-400" />
                    Thông báo mới ({notifications.length})
                  </div>
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllAsRead}
                      className="text-[11px] text-orange-600 dark:text-amber-400 hover:underline font-medium cursor-pointer"
                    >
                      Đọc tất cả
                    </button>
                  )}
                </div>

                <div data-lenis-prevent className="max-h-72 overflow-y-auto divide-y divide-slate-100 dark:divide-white/5">
                  {notifications.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-400 dark:text-slate-500">
                      Chưa có thông báo nào
                    </div>
                  ) : (
                    notifications.map((n) => (
                      <button
                        type="button"
                        key={n.id}
                        onClick={() => markAsRead(n.id)}
                        className={`w-full text-left p-3 text-xs cursor-pointer transition hover:bg-slate-50 dark:hover:bg-white/5 border-0 ${
                          !n.isRead ? 'bg-orange-50/40 dark:bg-orange-950/20 font-medium' : 'bg-transparent text-slate-600 dark:text-slate-300'
                        }`}
                      >
                        <div className="font-semibold text-slate-900 dark:text-[#F4F0E8] mb-0.5 flex items-center justify-between">
                          <span>{n.title}</span>
                          {!n.isRead && (
                            <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                          )}
                        </div>
                        <div className="text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                          {n.content}
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>
            </AnimatedPresence>
          </div>

          {/* Theme Toggle (Lucian #219 Circular Reveal) */}
          <ThemeToggle size="sm" />

          {/* Shopping Cart Tactile Pill with Subtle Magnetic Pull & Shimmer */}
          <LucianButton
            ref={cartButtonRef}
            magnetic
            variant="obsidian"
            size="sm"
            onClick={onOpenCart}
            className="flex-shrink-0"
            title="Xem Giỏ hàng"
          >
            <div className="relative">
              <ShoppingCart className="w-4 h-4 text-amber-400 group-hover:scale-110 transition-transform" />
              {totalCartCount > 0 && (
                <span
                  ref={cartBadgeRef}
                  className="absolute -top-2.5 -right-2.5 bg-orange-600 text-white text-[10px] font-mono font-bold px-1 min-w-4 h-4 rounded-full flex items-center justify-center shadow-xs select-none"
                >
                  <AnimatedNumber value={totalCartCount} />
                </span>
              )}
            </div>
            <span className="hidden lg:inline text-xs font-bold tracking-tight">Giỏ hàng</span>
          </LucianButton>

          {/* User Account / Auth Dropdown */}
          <div className="relative" ref={userContainerRef}>
            {isAuthenticated && user ? (
              <button
                ref={userButtonRef}
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center gap-2 p-1.5 pr-2.5 rounded-xl border border-slate-200 dark:border-white/10 hover:border-orange-300 dark:hover:border-amber-400/50 bg-slate-50/50 dark:bg-white/5 transition active:scale-[0.96] cursor-pointer"
              >
                <div className="w-7 h-7 rounded-lg bg-orange-100 dark:bg-orange-950/60 text-orange-600 dark:text-amber-400 font-bold flex items-center justify-center text-xs overflow-hidden border border-orange-200/60 dark:border-white/10 shadow-2xs">
                  {user.avatar || user.avatarUrl ? (
                    <img src={user.avatar || user.avatarUrl} alt="" className="w-full h-full object-cover" />
                  ) : user.fullName ? (
                    user.fullName.charAt(0).toUpperCase()
                  ) : (
                    'U'
                  )}
                </div>
                <div className="hidden lg:block text-left text-xs">
                  <div className="font-semibold text-slate-900 dark:text-[#F4F0E8] truncate max-w-[120px]">
                    {user.fullName || user.username}
                  </div>
                </div>
                <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
              </button>
            ) : (
              <LucianButton
                variant="secondary"
                size="sm"
                onClick={handleGoToLogin}
                icon={User}
              >
                <span>Đăng nhập</span>
              </LucianButton>
            )}

            {/* User Dropdown Menu - Phân quyền và Tách biệt Cổng rõ ràng */}
            <AnimatedPresence show={showUserMenu && isAuthenticated} mode="dropdown" className="absolute right-0 mt-2 w-56 z-50">
              <div className="bg-white dark:bg-[#13141C] rounded-2xl shadow-2xl border border-slate-200 dark:border-white/10 p-2 text-slate-900 dark:text-[#F4F0E8]">
                <div className="p-2.5 border-b border-slate-100 dark:border-white/5 mb-1">
                  <div className="font-bold text-xs text-slate-900 dark:text-[#F4F0E8]">{user?.fullName || user?.username}</div>
                  <div className="text-[11px] text-slate-500 dark:text-slate-400 truncate">{user?.email}</div>
                </div>

                {/* Các chức năng cá nhân của người mua */}
                <div className="space-y-0.5">
                  <button
                    onClick={() => {
                      setCurrentView('profile');
                      setShowUserMenu(false);
                    }}
                    className={`w-full text-left px-3 py-2 text-xs font-medium rounded-xl flex items-center gap-2 transition cursor-pointer active:scale-95 ${
                      currentView === 'profile'
                        ? 'bg-orange-50 dark:bg-orange-950/40 text-orange-600 dark:text-orange-400 font-bold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-white/5 hover:text-orange-600 dark:hover:text-amber-400'
                    }`}
                  >
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    <span>Hồ sơ của tôi</span>
                  </button>

                  <button
                    onClick={() => {
                      setCurrentView('orders');
                      setShowUserMenu(false);
                    }}
                    className={`w-full text-left px-3 py-2 text-xs font-medium rounded-xl flex items-center gap-2 transition cursor-pointer active:scale-95 ${
                      currentView === 'orders'
                        ? 'bg-orange-50 dark:bg-orange-950/40 text-orange-600 dark:text-orange-400 font-bold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-white/5 hover:text-orange-600 dark:hover:text-amber-400'
                    }`}
                  >
                    <Package className="w-3.5 h-3.5 text-slate-400" />
                    <span>Đơn mua / Lịch sử đơn</span>
                  </button>

                  <button
                    onClick={() => {
                      if (onOpenChatWithShop) onOpenChatWithShop();
                      else setCurrentView('chat');
                      setShowUserMenu(false);
                    }}
                    className={`w-full text-left px-3 py-2 text-xs font-medium rounded-xl flex items-center justify-between transition cursor-pointer active:scale-95 ${
                      currentView === 'chat'
                        ? 'bg-orange-50 dark:bg-orange-950/40 text-orange-600 dark:text-orange-400 font-bold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-white/5 hover:text-orange-600 dark:hover:text-amber-400'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <MessageSquare className="w-3.5 h-3.5 text-orange-600" />
                      <span>Tin nhắn Messenger</span>
                    </div>
                    {unreadChatCount > 0 && (
                      <span className="w-4 h-4 rounded-full bg-orange-600 text-white text-[9px] font-black flex items-center justify-center">
                        {unreadChatCount}
                      </span>
                    )}
                  </button>
                </div>

                {/* Cổng chuyên dụng dành riêng cho vai trò công việc (Chỉ hiển thị Cổng tương ứng) */}
                {isShipper && (
                  <div className="pt-1.5 mt-1.5 border-t border-slate-100">
                    <div className="px-2 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      Cổng Làm Việc
                    </div>
                    <button
                      onClick={() => {
                        setCurrentView('shipper');
                        setShowUserMenu(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs font-bold text-blue-700 bg-blue-50 hover:bg-blue-100 rounded-xl flex items-center justify-between transition cursor-pointer active:scale-95"
                    >
                      <div className="flex items-center gap-2">
                        <Truck className="w-4 h-4 text-blue-600" />
                        <span>Kênh Vận Chuyển</span>
                      </div>
                      <ChevronDown className="w-3 h-3 -rotate-90 text-blue-500" />
                    </button>
                  </div>
                )}

                {isSellerOnly && (
                  <div className="pt-1.5 mt-1.5 border-t border-slate-100 dark:border-white/5">
                    <div className="px-2 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      Cổng Làm Việc
                    </div>
                    <button
                      onClick={() => {
                        setCurrentView('seller');
                        setShowUserMenu(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 hover:bg-emerald-100 dark:hover:bg-emerald-950/50 rounded-xl flex items-center justify-between transition cursor-pointer active:scale-95"
                    >
                      <div className="flex items-center gap-2">
                        <Store className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                        <span>Kênh Người Bán</span>
                      </div>
                      <ChevronDown className="w-3 h-3 -rotate-90 text-emerald-500" />
                    </button>
                  </div>
                )}

                {isAdmin && (
                  <div className="pt-1.5 mt-1.5 border-t border-slate-100 dark:border-white/5">
                    <div className="px-2 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      Cổng Quản Trị
                    </div>
                    <button
                      onClick={() => {
                        setCurrentView('admin');
                        setShowUserMenu(false);
                      }}
                      className="w-full text-left px-3 py-2 text-xs font-bold text-slate-800 dark:text-slate-200 bg-slate-100 dark:bg-white/10 hover:bg-slate-200 dark:hover:bg-white/15 rounded-xl flex items-center justify-between transition cursor-pointer active:scale-95"
                    >
                      <div className="flex items-center gap-2">
                        <ShieldCheck className="w-4 h-4 text-slate-700 dark:text-amber-400" />
                        <span>Trang Quản Trị Hệ Thống</span>
                      </div>
                      <ChevronDown className="w-3 h-3 -rotate-90 text-slate-500" />
                    </button>
                  </div>
                )}

                <div className="pt-1.5 mt-1.5 border-t border-slate-100 dark:border-white/5">
                  <button
                    onClick={() => {
                      logout();
                      setCurrentView('storefront');
                      setShowUserMenu(false);
                    }}
                    className="w-full text-left px-3 py-2 text-xs font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 rounded-xl flex items-center gap-2 transition cursor-pointer active:scale-95"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              </div>
            </AnimatedPresence>
          </div>
        </div>
      </nav>

      {/* Mobile Command Search Strip */}
      <div className="md:hidden w-full px-1">
        <form onSubmit={handleTriggerSearch} className="relative flex items-center">
          <Search className="absolute left-3.5 w-4 h-4 text-slate-400 pointer-events-none" />
          <input
            type="text"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onKeyDown={handleSearchKeyPress}
            placeholder="Tìm kiếm hơn 10.000+ sản phẩm chính hãng..."
            className="w-full pl-10 pr-24 py-2 bg-white/95 dark:bg-[#13141C] backdrop-blur-md text-xs text-slate-900 dark:text-[#F4F0E8] rounded-2xl border border-slate-200/80 dark:border-white/10 shadow-md focus:border-orange-500 focus:outline-none"
          />
          <button
            type="submit"
            aria-label="Tìm kiếm sản phẩm"
            className="absolute right-1 px-3.5 py-1.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl text-xs font-bold active:scale-95 transition flex items-center gap-1 shadow-sm cursor-pointer"
          >
            <Search className="w-3.5 h-3.5 stroke-[2.5]" />
            <span>Tìm</span>
          </button>
        </form>
      </div>
    </div>
  </header>

  {/* Spacer giữ chỗ để nội dung trang không bị thanh điều hướng fixed che khuất */}
  <div className="h-[96px] md:h-[76px] shrink-0 w-full pointer-events-none select-none" aria-hidden="true" />
</>
  );
}
