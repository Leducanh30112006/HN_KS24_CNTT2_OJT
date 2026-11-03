import React, { useState, useMemo } from 'react';
import { 
  ShoppingBag, ArrowLeft, Trash2, Store, Tag, ShieldCheck, 
  Truck, ChevronRight, Check, Sparkles, MessageSquare, AlertCircle
} from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';

export default function CartPage({ 
  onProceedToCheckout, 
  onBack, 
  onSelectProduct 
}) {
  const { items, updateQuantity, removeFromCart, removeMultipleFromCart, getItemsByShop } = useCartStore();

  // Selected item variantIds for checkout (Mặc định chọn tất cả)
  const [selectedVariantIds, setSelectedVariantIds] = useState(() => {
    return new Set(items.map((i) => i.variantId));
  });

  const shopGroups = getItemsByShop();

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  // Toggle single item
  const handleToggleItem = (variantId) => {
    setSelectedVariantIds((prev) => {
      const next = new Set(prev);
      if (next.has(variantId)) {
        next.delete(variantId);
      } else {
        next.add(variantId);
      }
      return next;
    });
  };

  // Toggle all items of a specific Shop
  const handleToggleShop = (shopItems) => {
    const shopVariantIds = shopItems.map((i) => i.variantId);
    const allShopSelected = shopVariantIds.every((id) => selectedVariantIds.has(id));

    setSelectedVariantIds((prev) => {
      const next = new Set(prev);
      if (allShopSelected) {
        shopVariantIds.forEach((id) => next.delete(id));
      } else {
        shopVariantIds.forEach((id) => next.add(id));
      }
      return next;
    });
  };

  // Toggle ALL items in Cart
  const isAllSelected = items.length > 0 && items.every((i) => selectedVariantIds.has(i.variantId));
  const handleToggleAll = () => {
    if (isAllSelected) {
      setSelectedVariantIds(new Set());
    } else {
      setSelectedVariantIds(new Set(items.map((i) => i.variantId)));
    }
  };

  // Delete all selected items
  const handleDeleteSelected = () => {
    if (selectedVariantIds.size === 0) return;
    if (window.confirm(`Bạn có chắc chắn muốn xóa ${selectedVariantIds.size} sản phẩm đã chọn khỏi giỏ hàng?`)) {
      removeMultipleFromCart(Array.from(selectedVariantIds));
      setSelectedVariantIds(new Set());
    }
  };

  // Filter out the selected items for checkout
  const selectedItems = useMemo(() => {
    return items.filter((i) => selectedVariantIds.has(i.variantId));
  }, [items, selectedVariantIds]);

  const selectedCount = selectedItems.reduce((sum, i) => sum + i.quantity, 0);
  const selectedTotal = selectedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);

  // Handle Checkout click
  const handleCheckoutClick = () => {
    if (selectedItems.length === 0) return;
    onProceedToCheckout(selectedItems);
  };

  // EMPTY CART SCREEN
  if (items.length === 0) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 animate-in fade-in duration-300">
        <div className="bg-white rounded-3xl border border-slate-200/90 p-10 sm:p-16 text-center space-y-4 shadow-sm">
          <div className="w-24 h-24 rounded-3xl bg-orange-50 text-orange-400 mx-auto flex items-center justify-center shadow-inner">
            <ShoppingBag className="w-12 h-12" />
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-800">Giỏ hàng của bạn còn trống</h2>
          <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
            Bạn chưa có sản phẩm nào trong giỏ hàng. Hãy khám phá hàng ngàn deal sốc công nghệ, thời trang và gia dụng ưu đãi hôm nay nhé!
          </p>
          <div className="pt-4">
            <button
              onClick={onBack}
              className="px-8 py-3.5 bg-gradient-to-r from-orange-500 to-rose-600 text-white font-black text-xs sm:text-sm rounded-2xl shadow-lg shadow-orange-500/25 hover:from-orange-600 hover:to-rose-700 transition inline-flex items-center gap-2 active:scale-95 uppercase tracking-wide"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Mua Ngay</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-28 animate-in fade-in duration-300">
      {/* 1. Header Navigation & Breadcrumbs */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <button
          onClick={onBack}
          className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-bold active:scale-95 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Tiếp tục Mua sắm</span>
        </button>

        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <span className="text-slate-400 cursor-pointer hover:text-orange-600" onClick={onBack}>Trang chủ</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-orange-600 font-bold">Giỏ Hàng QuickShop</span>
          <span className="bg-orange-100 text-orange-700 font-bold px-2 py-0.5 rounded-full text-[11px] ml-1">
            {items.reduce((s, i) => s + i.quantity, 0)} món
          </span>
        </div>
      </div>

      {/* 2. Freeship Xtra Notification Banner */}
      <div className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white p-3.5 sm:px-6 rounded-2xl shadow-sm flex items-center gap-3 text-xs">
        <Truck className="w-5 h-5 flex-shrink-0" />
        <div className="flex-1 font-medium">
          <span className="font-bold uppercase tracking-wider bg-white/20 px-2 py-0.5 rounded mr-2">Freeship Xtra</span>
          Đơn hàng từ 99.000₫ được hỗ trợ tối đa 25.000₫ phí vận chuyển toàn quốc!
        </div>
      </div>

      {/* 3. Table Header Bar (Chuẩn Shopee) */}
      <div className="bg-white rounded-2xl border border-slate-200/90 p-4 shadow-sm hidden md:grid grid-cols-12 gap-4 text-xs font-bold text-slate-600 items-center">
        <div className="col-span-5 flex items-center space-x-3">
          <input
            type="checkbox"
            checked={isAllSelected}
            onChange={handleToggleAll}
            className="w-4 h-4 accent-orange-600 rounded cursor-pointer"
          />
          <span className="text-slate-800">Sản Phẩm ({items.length})</span>
        </div>
        <div className="col-span-2 text-center">Đơn Giá</div>
        <div className="col-span-2 text-center">Số Lượng</div>
        <div className="col-span-2 text-center">Số Tiền</div>
        <div className="col-span-1 text-center">Thao Tác</div>
      </div>

      {/* 4. Cart Items Grouped by Shop */}
      <div className="space-y-4">
        {shopGroups.map((group) => {
          const shopVariantIds = group.items.map((i) => i.variantId);
          const isShopSelected = shopVariantIds.every((id) => selectedVariantIds.has(id));

          return (
            <div key={group.shopId} className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
              {/* Shop Header */}
              <div className="bg-slate-50/80 p-4 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <input
                    type="checkbox"
                    checked={isShopSelected}
                    onChange={() => handleToggleShop(group.items)}
                    className="w-4 h-4 accent-orange-600 rounded cursor-pointer"
                  />
                  <Store className="w-4 h-4 text-orange-600" />
                  <span className="font-bold text-xs sm:text-sm text-slate-900">{group.shopName}</span>
                  <span className="bg-rose-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded uppercase">
                    Mall
                  </span>
                </div>

                <div className="flex items-center gap-3 text-xs">
                  <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-100 hidden sm:inline-block">
                    Giao từ Hà Nội
                  </span>
                  <button 
                    onClick={() => alert(`Đang mở trung tâm chat với ${group.shopName}...`)}
                    className="text-orange-600 hover:text-orange-700 font-bold flex items-center gap-1 transition"
                  >
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>Chat ngay</span>
                  </button>
                </div>
              </div>

              {/* Items in this Shop */}
              <div className="divide-y divide-slate-100 p-4">
                {group.items.map((item) => {
                  const isChecked = selectedVariantIds.has(item.variantId);

                  return (
                    <div 
                      key={item.variantId} 
                      className={`py-3.5 grid grid-cols-1 md:grid-cols-12 gap-4 items-center transition-colors rounded-xl px-2 ${
                        isChecked ? 'bg-orange-50/20' : 'hover:bg-slate-50/50'
                      }`}
                    >
                      {/* Product Details & Checkbox (5 cols) */}
                      <div className="col-span-1 md:col-span-5 flex items-center space-x-3">
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => handleToggleItem(item.variantId)}
                          className="w-4 h-4 accent-orange-600 rounded cursor-pointer flex-shrink-0"
                        />
                        <img
                          src={item.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                          alt={item.productName}
                          className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl object-cover bg-slate-50 border border-slate-200/80 flex-shrink-0 cursor-pointer"
                          onClick={() => onSelectProduct && onSelectProduct({ id: item.productId, name: item.productName })}
                        />
                        <div className="space-y-1 min-w-0 flex-1">
                          <h4 
                            onClick={() => onSelectProduct && onSelectProduct({ id: item.productId, name: item.productName })}
                            className="text-xs sm:text-sm font-bold text-slate-800 line-clamp-2 hover:text-orange-600 transition cursor-pointer"
                            title={item.productName}
                          >
                            {item.productName}
                          </h4>
                          <div className="text-[11px] text-slate-500 flex flex-wrap gap-2">
                            {item.color && (
                              <span className="bg-slate-100 px-2 py-0.5 rounded-md font-medium text-slate-700">
                                Phân loại: {item.color}
                              </span>
                            )}
                            {item.size && (
                              <span className="bg-slate-100 px-2 py-0.5 rounded-md font-medium text-slate-700">
                                {item.size}
                              </span>
                            )}
                          </div>
                          {/* Mobile unit price */}
                          <div className="text-xs font-bold text-slate-500 md:hidden">
                            Đơn giá: {formatVND(item.price)}
                          </div>
                        </div>
                      </div>

                      {/* Unit Price (2 cols) */}
                      <div className="col-span-2 text-center text-xs font-semibold text-slate-600 hidden md:block">
                        {formatVND(item.price)}
                      </div>

                      {/* Quantity Controls (2 cols) */}
                      <div className="col-span-2 flex justify-start md:justify-center items-center">
                        <div className="flex items-center border border-slate-200 rounded-xl overflow-hidden shadow-inner bg-slate-50">
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity - 1)}
                            className="px-3 py-1 bg-white hover:bg-slate-100 font-bold text-slate-700 transition active:scale-95"
                            title="Giảm số lượng"
                          >
                            -
                          </button>
                          <span className="px-3.5 py-1 text-xs font-bold text-slate-900 bg-white min-w-[36px] text-center">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity + 1)}
                            className="px-3 py-1 bg-white hover:bg-slate-100 font-bold text-slate-700 transition active:scale-95"
                            title="Tăng số lượng"
                          >
                            +
                          </button>
                        </div>
                      </div>

                      {/* Line Total (2 cols) */}
                      <div className="col-span-2 text-left md:text-center">
                        <div className="text-xs sm:text-sm font-black text-rose-600">
                          {formatVND(item.price * item.quantity)}
                        </div>
                      </div>

                      {/* Delete Action (1 col) */}
                      <div className="col-span-1 text-right md:text-center">
                        <button
                          onClick={() => removeFromCart(item.variantId)}
                          className="p-2 text-slate-400 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition"
                          title="Xóa sản phẩm này"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>

      {/* 5. Sticky Bottom Action Bar (Chuẩn Shopee) */}
      <div className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 shadow-2xl py-3 sm:py-4 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-3 sm:gap-6">
          {/* Left: Select all & Bulk delete */}
          <div className="flex items-center gap-4 text-xs font-bold text-slate-700 w-full sm:w-auto justify-between sm:justify-start">
            <label className="flex items-center space-x-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={isAllSelected}
                onChange={handleToggleAll}
                className="w-4 h-4 accent-orange-600 rounded cursor-pointer"
              />
              <span>Chọn tất cả ({items.length})</span>
            </label>

            {selectedVariantIds.size > 0 && (
              <button
                onClick={handleDeleteSelected}
                className="text-slate-400 hover:text-rose-600 transition flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Xóa các mục đã chọn ({selectedVariantIds.size})</span>
              </button>
            )}
          </div>

          {/* Right: Total price & Proceed button */}
          <div className="flex items-center justify-between sm:justify-end gap-4 sm:gap-6 w-full sm:w-auto">
            <div className="text-right">
              <div className="text-xs text-slate-500 font-medium">
                Tổng thanh toán ({selectedCount} sản phẩm):
              </div>
              <div className="text-lg sm:text-2xl font-black text-rose-600">
                {formatVND(selectedTotal)}
              </div>
            </div>

            <button
              onClick={handleCheckoutClick}
              disabled={selectedCount === 0}
              className={`px-8 sm:px-12 py-3.5 sm:py-4 rounded-2xl font-black text-xs sm:text-sm uppercase tracking-wide transition shadow-xl active:scale-95 flex items-center gap-2 ${
                selectedCount > 0
                  ? 'bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white shadow-orange-500/30 cursor-pointer'
                  : 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
              }`}
            >
              <span>Mua Hàng ({selectedCount})</span>
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

