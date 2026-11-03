import React, { useState } from 'react';
import { X, Trash2, ShoppingBag, Store, ArrowRight, ShieldCheck, Tag, CheckCircle2 } from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';

export default function CartDrawer({ isOpen, onClose }) {
  const { items, updateQuantity, removeFromCart, clearCart, getTotalAmount, getItemsByShop } = useCartStore();
  const { user } = useAuthStore();

  const [voucherCode, setVoucherCode] = useState('');
  const [discountAmount, setDiscountAmount] = useState(0);
  const [voucherApplied, setVoucherApplied] = useState('');
  const [checkoutSuccess, setCheckoutSuccess] = useState(false);

  if (!isOpen) return null;

  const shopGroups = getItemsByShop();
  const subTotal = getTotalAmount();
  const shippingFee = subTotal > 99000 ? 0 : 25000;
  const finalTotal = Math.max(0, subTotal + shippingFee - discountAmount);

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const handleApplyVoucher = (e) => {
    e.preventDefault();
    const code = voucherCode.trim().toUpperCase();
    if (code === 'QUICKSHOP50K') {
      setDiscountAmount(50000);
      setVoucherApplied('Mã QUICKSHOP50K: Giảm 50.000đ đã được áp dụng!');
    } else if (code === 'FREESHIPMAX') {
      setDiscountAmount(25000);
      setVoucherApplied('Mã FREESHIPMAX: Miễn phí vận chuyển toàn quốc!');
    } else {
      setVoucherApplied('Mã giảm giá không hợp lệ hoặc đã hết lượt.');
    }
  };

  const handleCheckout = () => {
    setCheckoutSuccess(true);
    setTimeout(() => {
      clearCart();
      setCheckoutSuccess(false);
      onClose();
    }, 3000);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-sm flex justify-end animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white h-full shadow-2xl flex flex-col justify-between animate-in slide-in-from-right duration-300">
        {/* Drawer Header */}
        <div className="p-4 border-b border-slate-200 flex justify-between items-center bg-slate-50/70">
          <div className="flex items-center space-x-2">
            <ShoppingBag className="w-5 h-5 text-orange-600" />
            <h2 className="font-bold text-sm sm:text-base text-slate-900">
              Giỏ hàng của bạn ({items.reduce((s, i) => s + i.quantity, 0)})
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-slate-200 text-slate-500 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Body: Cart Items by Shop */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {items.length === 0 ? (
            <div className="py-20 text-center space-y-3">
              <div className="w-16 h-16 rounded-full bg-orange-50 text-orange-400 mx-auto flex items-center justify-center">
                <ShoppingBag className="w-8 h-8" />
              </div>
              <div className="text-sm font-semibold text-slate-700">Giỏ hàng của bạn đang trống</div>
              <p className="text-xs text-slate-400 max-w-xs mx-auto">
                Hãy khám phá hàng ngàn sản phẩm công nghệ, thời trang và gia dụng ưu đãi hôm nay nhé!
              </p>
            </div>
          ) : (
            shopGroups.map((group) => (
              <div key={group.shopId} className="border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
                {/* Shop Group Header */}
                <div className="bg-slate-50 p-3 border-b border-slate-200 flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Store className="w-4 h-4 text-orange-600" />
                    <span className="font-bold text-xs text-slate-900">{group.shopName}</span>
                  </div>
                  <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                    Giao từ Hà Nội
                  </span>
                </div>

                {/* Products in this Shop */}
                <div className="divide-y divide-slate-100 p-2">
                  {group.items.map((item) => (
                    <div key={item.variantId} className="py-2.5 flex gap-3 items-center">
                      <img
                        src={item.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                        alt={item.productName}
                        className="w-14 h-14 rounded-xl object-cover bg-slate-50 border border-slate-100 flex-shrink-0"
                      />
                      <div className="flex-1 min-w-0">
                        <h4 className="text-xs font-semibold text-slate-800 line-clamp-1">{item.productName}</h4>
                        <div className="text-[11px] text-slate-500 mt-0.5">
                          {item.color && <span>Màu: {item.color} </span>}
                          {item.size && <span>• Size: {item.size}</span>}
                        </div>
                        <div className="text-xs font-bold text-orange-600 mt-1">
                          {formatVND(item.price)}
                        </div>
                      </div>

                      {/* Quantity Controls & Delete */}
                      <div className="flex flex-col items-end gap-1.5">
                        <button
                          onClick={() => removeFromCart(item.variantId)}
                          className="text-slate-400 hover:text-rose-600 p-1 transition"
                          title="Xóa sản phẩm"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                        <div className="flex items-center border border-slate-200 rounded-lg overflow-hidden text-xs">
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity - 1)}
                            className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 font-bold"
                          >
                            -
                          </button>
                          <span className="px-2.5 py-0.5 font-bold text-slate-800 bg-white">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity + 1)}
                            className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 font-bold"
                          >
                            +
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))
          )}

          {/* Voucher Input */}
          {items.length > 0 && (
            <form onSubmit={handleApplyVoucher} className="space-y-1.5 pt-2">
              <div className="text-xs font-semibold text-slate-700 flex items-center gap-1">
                <Tag className="w-3.5 h-3.5 text-orange-600" /> Mã giảm giá QuickShop:
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={voucherCode}
                  onChange={(e) => setVoucherCode(e.target.value)}
                  placeholder="Nhập QUICKSHOP50K hoặc FREESHIPMAX"
                  className="flex-1 bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs uppercase focus:outline-none focus:border-orange-500"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs rounded-xl transition"
                >
                  Áp dụng
                </button>
              </div>
              {voucherApplied && (
                <div className={`text-[11px] ${discountAmount > 0 ? 'text-emerald-600 font-medium' : 'text-rose-500'}`}>
                  {voucherApplied}
                </div>
              )}
            </form>
          )}
        </div>

        {/* Drawer Footer & Checkout summary */}
        {items.length > 0 && (
          <div className="p-4 border-t border-slate-200 bg-slate-50/80 space-y-3">
            <div className="space-y-1.5 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Tạm tính hàng hóa:</span>
                <span className="font-semibold text-slate-900">{formatVND(subTotal)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Phí vận chuyển toàn quốc:</span>
                <span className="font-semibold text-slate-900">
                  {shippingFee === 0 ? <span className="text-emerald-600">Miễn phí (Freeship)</span> : formatVND(shippingFee)}
                </span>
              </div>
              {discountAmount > 0 && (
                <div className="flex justify-between text-emerald-600 font-semibold">
                  <span>Giảm giá Voucher:</span>
                  <span>-{formatVND(discountAmount)}</span>
                </div>
              )}
              <div className="flex justify-between text-sm font-bold text-slate-900 pt-2 border-t border-slate-200">
                <span>Tổng cộng thanh toán:</span>
                <span className="text-base text-orange-600 font-black">{formatVND(finalTotal)}</span>
              </div>
            </div>

            {checkoutSuccess ? (
              <div className="p-3 bg-emerald-500 text-white rounded-xl text-center text-xs font-bold flex items-center justify-center gap-2">
                <CheckCircle2 className="w-4 h-4" />
                Đặt hàng thành công! Đang xử lý giữ kho Saga...
              </div>
            ) : (
              <button
                onClick={handleCheckout}
                className="w-full py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-sm rounded-xl transition shadow-lg shadow-orange-500/30 active:scale-95 flex items-center justify-center gap-2"
              >
                Tiến hành Đặt hàng
                <ArrowRight className="w-4 h-4" />
              </button>
            )}

            <div className="flex items-center justify-center gap-1.5 text-[10px] text-slate-400">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              Bảo mật thanh toán chuẩn PCI-DSS & Escrow Đảm Bảo
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
