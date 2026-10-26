import React, { useState } from 'react';
import { 
  ShoppingBag, Search, Bell, ShoppingCart, User, Store, ShieldCheck, 
  HelpCircle, Smartphone, ChevronDown, LogOut, CheckCircle, Tag
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { useNotificationStore } from '../stores/useNotificationStore';

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
}) {
  const { user, isAuthenticated, logout } = useAuthStore();
  const totalCartCount = useCartStore((state) => state.getTotalCount());
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotificationStore();

  const [showNotifMenu, setShowNotifMenu] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);

  const hotKeywords = ['iPhone 16 Pro', 'QuickBook Ultra M3', 'Áo Polo Cafe', 'Robot hút bụi', 'Kem chống nắng'];

  const handleSearchKeyPress = (e) => {
    if (e.key === 'Enter') {
      onSearchSubmit();
    }
  };

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-slate-200 shadow-sm">
      {/* 1. Top Utility Header (Chuẩn phong cách Sàn TMĐT Việt Nam) */}
      <div className="bg-gradient-to-r from-orange-600 via-rose-600 to-amber-600 text-white text-xs py-1.5 px-4">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-2">
          <div className="flex items-center space-x-4">
            <span className="flex items-center gap-1 opacity-90 hover:opacity-100 cursor-pointer">
              <Smartphone className="w-3.5 h-3.5" />
              Tải ứng dụng QuickShop
            </span>
            <span className="hidden sm:inline opacity-40">|</span>
            <span className="flex items-center gap-1 opacity-90 hover:opacity-100 cursor-pointer">
              <HelpCircle className="w-3.5 h-3.5" />
              Hotline hỗ trợ: <strong className="font-semibold text-amber-200">1900-QUICK (7:30 - 22:00)</strong>
            </span>
          </div>

          {/* Quick Portal Switcher */}
          <div className="flex items-center space-x-2 sm:space-x-4 font-medium">
            <button
              onClick={() => setCurrentView('storefront')}
              className={`px-2.5 py-0.5 rounded-full text-[11px] transition flex items-center gap-1 ${
                currentView === 'storefront' ? 'bg-white text-orange-600 shadow-sm font-semibold' : 'hover:bg-white/10'
              }`}
            >
              <ShoppingBag className="w-3 h-3" />
              Storefront (Người mua)
            </button>

            <button
              onClick={() => setCurrentView('seller')}
              className={`px-2.5 py-0.5 rounded-full text-[11px] transition flex items-center gap-1 ${
                currentView === 'seller' ? 'bg-white text-orange-600 shadow-sm font-semibold' : 'hover:bg-white/10'
              }`}
            >
              <Store className="w-3 h-3" />
              Kênh Người Bán
            </button>

            <button
              onClick={() => setCurrentView('admin')}
              className={`px-2.5 py-0.5 rounded-full text-[11px] transition flex items-center gap-1 ${
                currentView === 'admin' ? 'bg-white text-orange-600 shadow-sm font-semibold' : 'hover:bg-white/10'
              }`}
            >
              <ShieldCheck className="w-3 h-3" />
              Quản Trị Admin
            </button>
          </div>
        </div>
      </div>

      {/* 2. Main Navigation Bar */}
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
        {/* Brand Logo */}
        <div 
          onClick={() => setCurrentView('storefront')} 
          className="flex items-center gap-2.5 cursor-pointer group select-none flex-shrink-0"
        >
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-orange-500 via-rose-500 to-amber-500 flex items-center justify-center text-white shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
            <ShoppingBag className="w-6 h-6 stroke-[2.5]" />
          </div>
          <div>
            <div className="text-2xl font-black tracking-tight text-slate-900 flex items-center">
              Quick<span className="text-orange-600">Shop</span>
              <span className="ml-1 text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 bg-orange-100 text-orange-700 rounded-md">
                VN
              </span>
            </div>
            <div className="text-[10px] text-slate-600 font-medium tracking-wide">
              Sàn TMĐT Đa Gian Hàng - Giao Nhanh 2H
            </div>
          </div>
        </div>

        {/* Smart Search Bar */}
        <div className="flex-1 max-w-2xl hidden md:block">
          <div className="relative flex items-center">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyDown={handleSearchKeyPress}
              placeholder="Tìm kiếm sản phẩm, thương hiệu hoặc gian hàng..."
              className="w-full pl-4 pr-24 py-2.5 text-sm bg-slate-50 border border-slate-300 rounded-xl focus:bg-white focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 transition placeholder-slate-400"
            />
            <button
              onClick={onSearchSubmit}
              className="absolute right-1.5 px-4 py-1.5 bg-gradient-to-r from-orange-500 to-rose-500 text-white text-xs font-semibold rounded-lg hover:from-orange-600 hover:to-rose-600 transition shadow-sm flex items-center gap-1.5"
            >
              <Search className="w-3.5 h-3.5" />
              Tìm kiếm
            </button>
          </div>

          {/* Trending Hot Keywords */}
          <div className="flex items-center gap-2 mt-1.5 overflow-x-auto text-[11px] text-slate-500 whitespace-nowrap">
            <span className="flex items-center gap-0.5 text-orange-600 font-medium">
              <Tag className="w-3 h-3" /> Xu hướng:
            </span>
            {hotKeywords.map((kw, i) => (
              <button
                key={i}
                onClick={() => {
                  setSearchKeyword(kw);
                  onSearchSubmit(kw);
                }}
                className="hover:text-orange-600 transition underline-offset-2 hover:underline"
              >
                {kw}
              </button>
            ))}
          </div>
        </div>

        {/* Right Action Icons: Notification, Cart, User Profile */}
        <div className="flex items-center space-x-2 sm:space-x-4">
          {/* Notifications Dropdown */}
          <div className="relative">
            <button
              onClick={() => setShowNotifMenu(!showNotifMenu)}
              className="p-2.5 text-slate-600 hover:text-orange-600 hover:bg-orange-50 rounded-xl transition relative"
              title="Thông báo"
            >
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse">
                  {unreadCount}
                </span>
              )}
            </button>

            {showNotifMenu && (
              <div className="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-2xl border border-slate-200 z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-150">
                <div className="p-3 bg-gradient-to-r from-orange-50 to-rose-50 border-b border-slate-100 flex justify-between items-center">
                  <div className="font-semibold text-xs text-slate-800 flex items-center gap-1.5">
                    <Bell className="w-3.5 h-3.5 text-orange-600" />
                    Thông báo mới ({notifications.length})
                  </div>
                  {unreadCount > 0 && (
                    <button
                      onClick={markAllAsRead}
                      className="text-[11px] text-orange-600 hover:underline font-medium"
                    >
                      Đọc tất cả
                    </button>
                  )}
                </div>

                <div className="max-h-72 overflow-y-auto divide-y divide-slate-100">
                  {notifications.length === 0 ? (
                    <div className="p-6 text-center text-xs text-slate-400">
                      Chưa có thông báo nào
                    </div>
                  ) : (
                    notifications.map((n) => (
                      <div
                        key={n.id}
                        onClick={() => markAsRead(n.id)}
                        className={`p-3 text-xs cursor-pointer transition hover:bg-slate-50 ${
                          !n.isRead ? 'bg-orange-50/40 font-medium' : 'text-slate-600'
                        }`}
                      >
                        <div className="font-semibold text-slate-900 mb-0.5 flex items-center justify-between">
                          <span>{n.title}</span>
                          {!n.isRead && (
                            <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                          )}
                        </div>
                        <div className="text-slate-500 line-clamp-2 leading-relaxed">
                          {n.content}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Shopping Cart Button with Dynamic Badge */}
          <button
            onClick={onOpenCart}
            className="flex items-center gap-2 p-2 sm:px-3.5 sm:py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-xl transition shadow-md shadow-orange-500/20 active:scale-95"
          >
            <div className="relative">
              <ShoppingCart className="w-5 h-5" />
              {totalCartCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-white text-orange-600 text-[10px] font-black w-4 h-4 rounded-full flex items-center justify-center shadow-sm">
                  {totalCartCount}
                </span>
              )}
            </div>
            <span className="hidden sm:inline text-xs font-semibold">Giỏ hàng</span>
          </button>

          {/* User Account / Auth Dropdown */}
          <div className="relative">
            {isAuthenticated && user ? (
              <button
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center gap-2 p-1.5 pr-2.5 rounded-xl border border-slate-200 hover:border-orange-300 transition"
              >
                <div className="w-7 h-7 rounded-lg bg-orange-100 text-orange-600 font-bold flex items-center justify-center text-xs">
                  {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                </div>
                <div className="hidden lg:block text-left text-xs">
                  <div className="font-semibold text-slate-900 truncate max-w-[100px]">
                    {user.fullName || user.username}
                  </div>
                  <div className="text-[10px] text-orange-600 font-medium">
                    {user.role}
                  </div>
                </div>
                <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
              </button>
            ) : (
              <button
                onClick={onOpenAuthModal}
                className="flex items-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 hover:text-orange-600 hover:bg-orange-50 rounded-xl transition"
              >
                <User className="w-4 h-4" />
                <span>Đăng nhập</span>
              </button>
            )}

            {/* User Dropdown Menu */}
            {showUserMenu && isAuthenticated && (
              <div className="absolute right-0 mt-2 w-52 bg-white rounded-2xl shadow-xl border border-slate-200 z-50 p-2 animate-in fade-in zoom-in-95 duration-150">
                <div className="p-2 border-b border-slate-100 mb-1">
                  <div className="font-semibold text-xs text-slate-900">{user?.fullName}</div>
                  <div className="text-[11px] text-slate-500 truncate">{user?.email}</div>
                </div>

                <button
                  onClick={() => {
                    setCurrentView('seller');
                    setShowUserMenu(false);
                  }}
                  className="w-full text-left px-3 py-2 text-xs font-medium text-slate-700 hover:bg-orange-50 hover:text-orange-600 rounded-lg flex items-center gap-2"
                >
                  <Store className="w-3.5 h-3.5" />
                  Kênh Người Bán
                </button>

                <button
                  onClick={() => {
                    setCurrentView('admin');
                    setShowUserMenu(false);
                  }}
                  className="w-full text-left px-3 py-2 text-xs font-medium text-slate-700 hover:bg-orange-50 hover:text-orange-600 rounded-lg flex items-center gap-2"
                >
                  <ShieldCheck className="w-3.5 h-3.5" />
                  Quản Trị Admin
                </button>

                <button
                  onClick={() => {
                    logout();
                    setShowUserMenu(false);
                  }}
                  className="w-full text-left px-3 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 rounded-lg flex items-center gap-2 mt-1 border-t border-slate-100"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Đăng xuất
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 3. Category Horizontal Pills (Dải danh mục trực quan) */}
      <div className="bg-slate-50 border-t border-slate-200/80 px-4 py-2">
        <div className="max-w-7xl mx-auto flex items-center space-x-2 overflow-x-auto text-xs no-scrollbar">
          <button
            onClick={() => setSelectedCategory(null)}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap font-medium transition ${
              selectedCategory === null
                ? 'bg-orange-600 text-white shadow-sm'
                : 'text-slate-600 hover:text-orange-600 hover:bg-white'
            }`}
          >
            Tất cả sản phẩm
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3 py-1.5 rounded-lg whitespace-nowrap font-medium transition flex items-center gap-1.5 ${
                selectedCategory === cat.id
                  ? 'bg-orange-600 text-white shadow-sm'
                  : 'text-slate-600 hover:text-orange-600 hover:bg-white'
              }`}
            >
              <span>{cat.name}</span>
              {cat.productCount > 0 && (
                <span className={`text-[10px] px-1 rounded-full ${
                  selectedCategory === cat.id ? 'bg-orange-700 text-white' : 'bg-slate-200 text-slate-600'
                }`}>
                  {cat.productCount}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}
