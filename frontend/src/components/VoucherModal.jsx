import React, { useState } from 'react';
import { 
  X, Ticket, Truck, Store, Check, AlertCircle, Sparkles, 
  ChevronRight, Tag, Info, ShieldCheck, Clock 
} from 'lucide-react';

export default function VoucherModal({ 
  isOpen, 
  onClose, 
  subTotal = 0,
  shopsInOrder = [], // [{ shopId, shopName }]
  appliedVouchers = {
    platformVoucher: null,
    shippingVoucher: null,
    shopVouchers: {}, // { [shopId]: voucherObject }
  },
  onApplyVouchers,
}) {
  const [activeTab, setActiveTab] = useState('all'); // 'all' | 'shipping' | 'platform' | 'shop'
  const [inputCode, setInputCode] = useState('');
  const [inputFeedback, setInputFeedback] = useState('');

  // Local selection states inside modal before clicking "Đồng ý"
  const [selectedShipping, setSelectedShipping] = useState(appliedVouchers.shippingVoucher);
  const [selectedPlatform, setSelectedPlatform] = useState(appliedVouchers.platformVoucher);
  const [selectedShopVouchers, setSelectedShopVouchers] = useState(appliedVouchers.shopVouchers || {});

  if (!isOpen) return null;

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  // 1. Platform Shipping Vouchers
  const shippingVouchersList = [
    {
      code: 'FREESHIPMAX',
      type: 'shipping',
      title: 'Miễn Phí Vận Chuyển Toàn Quốc',
      discount: 25000,
      minOrder: 99000,
      minOrderText: 'Đơn Tối Thiểu 99.000₫',
      exp: '30.11.2026',
      usedPercent: 85,
      badge: 'Freeship Xtra',
      description: 'Áp dụng cho mọi đơn vị vận chuyển tiêu chuẩn',
    },
    {
      code: 'FREESHIP50K',
      type: 'shipping',
      title: 'Giảm 50k Phí Vận Chuyển Hỏa Tốc',
      discount: 50000,
      minOrder: 300000,
      minOrderText: 'Đơn Tối Thiểu 300.000₫',
      exp: '30.11.2026',
      usedPercent: 62,
      badge: 'Hỏa Tốc 2H',
      description: 'Áp dụng cho đơn hàng giao hỏa tốc hoặc đơn lớn',
    },
  ];

  // 2. Platform Discount / Cashback Vouchers
  const platformVouchersList = [
    {
      code: 'QUICKSHOP50K',
      type: 'platform',
      title: 'Giảm 50.000₫ Đơn Hàng Sàn',
      discount: 50000,
      minOrder: 200000,
      minOrderText: 'Đơn Tối Thiểu 200.000₫',
      exp: '30.11.2026',
      usedPercent: 92,
      badge: 'Toàn Sàn',
      description: 'Áp dụng cho mọi ngành hàng trên QuickShop',
    },
    {
      code: 'MALL100K',
      type: 'platform',
      title: 'Giảm 100.000₫ Đơn Hàng Chính Hãng Mall',
      discount: 100000,
      minOrder: 1000000,
      minOrderText: 'Đơn Tối Thiểu 1.000.000₫',
      exp: '30.11.2026',
      usedPercent: 48,
      badge: 'QuickShop Mall',
      description: 'Áp dụng cho các sản phẩm gắn nhãn Mall chính hãng',
    },
    {
      code: 'SUPERDEAL15',
      type: 'platform',
      title: 'Giảm 15% Tối Đa 150.000₫',
      discount: Math.min(150000, Math.round(subTotal * 0.15)),
      isPercentage: true,
      minOrder: 500000,
      minOrderText: 'Đơn Tối Thiểu 500.000₫',
      exp: '30.11.2026',
      usedPercent: 70,
      badge: 'Deal Độc Quyền',
      description: 'Ưu đãi thanh toán trực tuyến và thẻ',
    },
  ];

  // 3. Shop Vouchers (Dynamically mapped based on shops present in the order)
  const defaultShopVouchersByShop = {
    1: [ // TechWorld Official Store
      { code: 'TECHWORLD30K', shopId: 1, shopName: 'TechWorld Official Store', title: 'Giảm 30.000₫', discount: 30000, minOrder: 300000, minOrderText: 'Đơn tối thiểu 300k', exp: '30.11.2026' },
      { code: 'TECHWORLD50K', shopId: 1, shopName: 'TechWorld Official Store', title: 'Giảm 50.000₫', discount: 50000, minOrder: 1000000, minOrderText: 'Đơn tối thiểu 1tr', exp: '30.11.2026' },
    ],
    2: [ // SmartHome & Fashion
      { code: 'SMARTHOME50K', shopId: 2, shopName: 'SmartHome Vietnam Store', title: 'Giảm 50.000₫', discount: 50000, minOrder: 500000, minOrderText: 'Đơn tối thiểu 500k', exp: '30.11.2026' },
      { code: 'FASHION20K', shopId: 2, shopName: 'Fashion Hub Official', title: 'Giảm 20.000₫', discount: 20000, minOrder: 200000, minOrderText: 'Đơn tối thiểu 200k', exp: '30.11.2026' },
    ],
    3: [ // BeautyCare & Sneaker
      { code: 'BEAUTY15K', shopId: 3, shopName: 'BeautyCare Mall', title: 'Giảm 15.000₫', discount: 15000, minOrder: 150000, minOrderText: 'Đơn tối thiểu 150k', exp: '30.11.2026' },
    ],
  };

  // Compile shop vouchers for current shops in order
  const activeShopVouchers = [];
  const currentShopIds = shopsInOrder.length > 0 ? shopsInOrder.map(s => s.shopId) : [1, 2, 3];
  currentShopIds.forEach((sId) => {
    const vouchers = defaultShopVouchersByShop[sId] || [
      { code: `SHOP${sId}_20K`, shopId: sId, shopName: `Gian hàng #${sId}`, title: 'Giảm 20.000₫', discount: 20000, minOrder: 200000, minOrderText: 'Đơn tối thiểu 200k', exp: '30.11.2026' }
    ];
    activeShopVouchers.push(...vouchers);
  });

  // Handle Manual Code Input
  const handleApplyInputCode = (e) => {
    e.preventDefault();
    const code = inputCode.trim().toUpperCase();
    if (!code) return;

    // Check shipping
    const shipMatch = shippingVouchersList.find(v => v.code === code);
    if (shipMatch) {
      if (subTotal >= shipMatch.minOrder) {
        setSelectedShipping(shipMatch);
        setInputFeedback(`✓ Đã áp dụng mã vận chuyển ${code}!`);
        setInputCode('');
      } else {
        setInputFeedback(`Đơn hàng cần đạt tối thiểu ${formatVND(shipMatch.minOrder)}.`);
      }
      return;
    }

    // Check platform
    const platMatch = platformVouchersList.find(v => v.code === code);
    if (platMatch) {
      if (subTotal >= platMatch.minOrder) {
        setSelectedPlatform(platMatch);
        setInputFeedback(`✓ Đã áp dụng mã sàn ${code}!`);
        setInputCode('');
      } else {
        setInputFeedback(`Đơn hàng cần đạt tối thiểu ${formatVND(platMatch.minOrder)}.`);
      }
      return;
    }

    // Check shop
    const shopMatch = activeShopVouchers.find(v => v.code === code);
    if (shopMatch) {
      setSelectedShopVouchers(prev => ({
        ...prev,
        [shopMatch.shopId]: shopMatch,
      }));
      setInputFeedback(`✓ Đã áp dụng voucher shop ${code}!`);
      setInputCode('');
      return;
    }

    setInputFeedback('Mã giảm giá không hợp lệ hoặc đã hết lượt.');
  };

  // Toggle selection
  const handleToggleShipping = (voucher) => {
    if (selectedShipping?.code === voucher.code) {
      setSelectedShipping(null);
    } else {
      setSelectedShipping(voucher);
    }
  };

  const handleTogglePlatform = (voucher) => {
    if (selectedPlatform?.code === voucher.code) {
      setSelectedPlatform(null);
    } else {
      setSelectedPlatform(voucher);
    }
  };

  const handleToggleShopVoucher = (voucher) => {
    setSelectedShopVouchers(prev => {
      const next = { ...prev };
      if (next[voucher.shopId]?.code === voucher.code) {
        delete next[voucher.shopId];
      } else {
        next[voucher.shopId] = voucher;
      }
      return next;
    });
  };

  // Confirm and apply
  const handleConfirm = () => {
    onApplyVouchers({
      shippingVoucher: selectedShipping,
      platformVoucher: selectedPlatform,
      shopVouchers: selectedShopVouchers,
    });
    onClose();
  };

  const handleResetAll = () => {
    setSelectedShipping(null);
    setSelectedPlatform(null);
    setSelectedShopVouchers({});
  };

  // Calculate total saved amount
  const totalShopDiscount = Object.values(selectedShopVouchers).reduce((sum, v) => sum + (v.discount || 0), 0);
  const totalPotentialSavings = (selectedShipping?.discount || 0) + (selectedPlatform?.discount || 0) + totalShopDiscount;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-black/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-slate-200 flex flex-col max-h-[90vh] overflow-hidden animate-in zoom-in-95 duration-200">
        
        {/* 1. Modal Header */}
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center shadow-inner">
              <Ticket className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-black text-slate-900 flex items-center gap-2">
                <span>Chọn QuickShop Voucher</span>
                <span className="text-[10px] bg-orange-100 text-orange-700 font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                  Chuẩn Shopee
                </span>
              </h2>
              <p className="text-[11px] text-slate-500">
                Áp dụng đồng thời mã Miễn Phí Vận Chuyển, Voucher Sàn & Voucher của từng Shop
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-200/60 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* 2. Voucher Code Input Bar */}
        <div className="p-4 bg-slate-50 border-b border-slate-100">
          <form onSubmit={handleApplyInputCode} className="flex gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                value={inputCode}
                onChange={(e) => setInputCode(e.target.value)}
                placeholder="Nhập mã voucher QuickShop hoặc mã Shop..."
                className="w-full pl-9 pr-3 py-2.5 bg-white border border-slate-300 rounded-xl text-xs uppercase font-mono font-bold focus:outline-none focus:border-orange-500 shadow-inner"
              />
              <Tag className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
            </div>
            <button
              type="submit"
              disabled={!inputCode.trim()}
              className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-40 text-white font-bold text-xs rounded-xl transition active:scale-95 shadow-sm"
            >
              Áp Dụng
            </button>
          </form>
          {inputFeedback && (
            <div className={`text-[11px] font-semibold mt-1.5 ${inputFeedback.startsWith('✓') ? 'text-emerald-600' : 'text-rose-500'}`}>
              {inputFeedback}
            </div>
          )}
        </div>

        {/* 3. Category Filter Tabs */}
        <div className="flex border-b border-slate-200 bg-white px-4 pt-2 gap-2 overflow-x-auto text-xs font-bold scrollbar-none">
          <button
            onClick={() => setActiveTab('all')}
            className={`pb-2.5 px-3 border-b-2 whitespace-nowrap transition ${
              activeTab === 'all'
                ? 'border-orange-600 text-orange-600 font-black'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            Tất cả Voucher
          </button>
          <button
            onClick={() => setActiveTab('shipping')}
            className={`pb-2.5 px-3 border-b-2 whitespace-nowrap transition flex items-center gap-1.5 ${
              activeTab === 'shipping'
                ? 'border-orange-600 text-orange-600 font-black'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Truck className="w-3.5 h-3.5" />
            <span>Miễn Phí Vận Chuyển</span>
            {selectedShipping && <span className="w-2 h-2 rounded-full bg-emerald-500"></span>}
          </button>
          <button
            onClick={() => setActiveTab('platform')}
            className={`pb-2.5 px-3 border-b-2 whitespace-nowrap transition flex items-center gap-1.5 ${
              activeTab === 'platform'
                ? 'border-orange-600 text-orange-600 font-black'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Voucher Sàn</span>
            {selectedPlatform && <span className="w-2 h-2 rounded-full bg-orange-500"></span>}
          </button>
          <button
            onClick={() => setActiveTab('shop')}
            className={`pb-2.5 px-3 border-b-2 whitespace-nowrap transition flex items-center gap-1.5 ${
              activeTab === 'shop'
                ? 'border-orange-600 text-orange-600 font-black'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <Store className="w-3.5 h-3.5" />
            <span>Voucher Của Shop</span>
            {Object.keys(selectedShopVouchers).length > 0 && (
              <span className="w-2 h-2 rounded-full bg-purple-500"></span>
            )}
          </button>
        </div>

        {/* 4. Voucher List Content */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-6 bg-slate-50/60">
          
          {/* Section 1: Shipping Vouchers */}
          {(activeTab === 'all' || activeTab === 'shipping') && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-black text-slate-900 uppercase tracking-wide">
                  <Truck className="w-4 h-4 text-emerald-600" />
                  <span>Mã Miễn Phí Vận Chuyển</span>
                  <span className="text-[10px] text-slate-400 font-normal lowercase">(Chọn tối đa 1 mã)</span>
                </div>
              </div>

              <div className="space-y-2.5">
                {shippingVouchersList.map((v) => {
                  const isEligible = subTotal >= v.minOrder;
                  const isSelected = selectedShipping?.code === v.code;

                  return (
                    <div
                      key={v.code}
                      onClick={() => isEligible && handleToggleShipping(v)}
                      className={`relative flex items-center rounded-2xl border transition overflow-hidden shadow-sm ${
                        !isEligible
                          ? 'opacity-50 bg-slate-100 border-slate-200 cursor-not-allowed'
                          : isSelected
                            ? 'border-emerald-500 bg-emerald-50/40 ring-2 ring-emerald-500/20 cursor-pointer'
                            : 'border-slate-200 hover:border-emerald-300 bg-white cursor-pointer'
                      }`}
                    >
                      {/* Left Ticket Stub */}
                      <div className="w-24 sm:w-28 p-3 bg-gradient-to-br from-emerald-600 to-teal-700 text-white flex flex-col items-center justify-center text-center self-stretch border-r border-dashed border-emerald-400 relative">
                        <Truck className="w-7 h-7 mb-1" />
                        <span className="text-[10px] font-black uppercase tracking-wider">FREESHIP</span>
                        <span className="text-[9px] text-emerald-200 font-bold">{v.badge}</span>
                      </div>

                      {/* Middle Details */}
                      <div className="flex-1 p-3.5 space-y-1 min-w-0">
                        <div className="text-xs sm:text-sm font-black text-slate-900">{v.title}</div>
                        <div className="text-[11px] text-slate-600 font-medium">{v.minOrderText}</div>
                        <div className="text-[10px] text-slate-400 flex items-center gap-2">
                          <span>HSD: {v.exp}</span>
                          <span>• Đã dùng {v.usedPercent}%</span>
                        </div>
                        {!isEligible && (
                          <div className="text-[10px] font-bold text-rose-500 flex items-center gap-1">
                            <AlertCircle className="w-3 h-3" />
                            <span>Mua thêm {formatVND(v.minOrder - subTotal)} để dùng mã này</span>
                          </div>
                        )}
                      </div>

                      {/* Right Radio Selector */}
                      <div className="p-4">
                        <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition ${
                          isSelected
                            ? 'border-emerald-600 bg-emerald-600 text-white'
                            : 'border-slate-300 bg-white'
                        }`}>
                          {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Section 2: Platform Vouchers */}
          {(activeTab === 'all' || activeTab === 'platform') && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-black text-slate-900 uppercase tracking-wide">
                  <Sparkles className="w-4 h-4 text-orange-600" />
                  <span>Voucher Giảm Giá Của Sàn</span>
                  <span className="text-[10px] text-slate-400 font-normal lowercase">(Chọn tối đa 1 mã)</span>
                </div>
              </div>

              <div className="space-y-2.5">
                {platformVouchersList.map((v) => {
                  const isEligible = subTotal >= v.minOrder;
                  const isSelected = selectedPlatform?.code === v.code;

                  return (
                    <div
                      key={v.code}
                      onClick={() => isEligible && handleTogglePlatform(v)}
                      className={`relative flex items-center rounded-2xl border transition overflow-hidden shadow-sm ${
                        !isEligible
                          ? 'opacity-50 bg-slate-100 border-slate-200 cursor-not-allowed'
                          : isSelected
                            ? 'border-orange-500 bg-orange-50/40 ring-2 ring-orange-500/20 cursor-pointer'
                            : 'border-slate-200 hover:border-orange-300 bg-white cursor-pointer'
                      }`}
                    >
                      {/* Left Ticket Stub */}
                      <div className="w-24 sm:w-28 p-3 bg-gradient-to-br from-orange-500 via-rose-500 to-amber-500 text-white flex flex-col items-center justify-center text-center self-stretch border-r border-dashed border-orange-300">
                        <Ticket className="w-7 h-7 mb-1" />
                        <span className="text-[10px] font-black uppercase tracking-wider">QUICKSHOP</span>
                        <span className="text-[9px] text-amber-200 font-bold">{v.badge}</span>
                      </div>

                      {/* Middle Details */}
                      <div className="flex-1 p-3.5 space-y-1 min-w-0">
                        <div className="text-xs sm:text-sm font-black text-slate-900">{v.title}</div>
                        <div className="text-[11px] text-slate-600 font-medium">{v.minOrderText}</div>
                        <div className="text-[10px] text-slate-400 flex items-center gap-2">
                          <span>HSD: {v.exp}</span>
                          <span>• Đã dùng {v.usedPercent}%</span>
                        </div>
                        {!isEligible && (
                          <div className="text-[10px] font-bold text-rose-500 flex items-center gap-1">
                            <AlertCircle className="w-3 h-3" />
                            <span>Mua thêm {formatVND(v.minOrder - subTotal)} để dùng mã này</span>
                          </div>
                        )}
                      </div>

                      {/* Right Radio Selector */}
                      <div className="p-4">
                        <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition ${
                          isSelected
                            ? 'border-orange-600 bg-orange-600 text-white'
                            : 'border-slate-300 bg-white'
                        }`}>
                          {isSelected && <Check className="w-3 h-3 stroke-[3]" />}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Section 3: Shop Vouchers */}
          {(activeTab === 'all' || activeTab === 'shop') && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-black text-slate-900 uppercase tracking-wide">
                  <Store className="w-4 h-4 text-purple-600" />
                  <span>Voucher Của Nhà Bán Hàng (Shop)</span>
                  <span className="text-[10px] text-slate-400 font-normal lowercase">(Áp dụng riêng cho từng Shop)</span>
                </div>
              </div>

              <div className="space-y-2.5">
                {activeShopVouchers.map((v) => {
                  const isEligible = subTotal >= v.minOrder;
                  const isSelected = selectedShopVouchers[v.shopId]?.code === v.code;

                  return (
                    <div
                      key={v.code}
                      onClick={() => isEligible && handleToggleShopVoucher(v)}
                      className={`relative flex items-center rounded-2xl border transition overflow-hidden shadow-sm ${
                        !isEligible
                          ? 'opacity-50 bg-slate-100 border-slate-200 cursor-not-allowed'
                          : isSelected
                            ? 'border-purple-500 bg-purple-50/40 ring-2 ring-purple-500/20 cursor-pointer'
                            : 'border-slate-200 hover:border-purple-300 bg-white cursor-pointer'
                      }`}
                    >
                      {/* Left Ticket Stub */}
                      <div className="w-24 sm:w-28 p-3 bg-gradient-to-br from-purple-600 to-indigo-700 text-white flex flex-col items-center justify-center text-center self-stretch border-r border-dashed border-purple-300">
                        <Store className="w-7 h-7 mb-1" />
                        <span className="text-[10px] font-black uppercase tracking-wider">SHOP DEAL</span>
                        <span className="text-[9px] text-purple-200 font-bold truncate max-w-[80px]">{v.shopName}</span>
                      </div>

                      {/* Middle Details */}
                      <div className="flex-1 p-3.5 space-y-1 min-w-0">
                        <div className="text-xs font-bold text-purple-700 flex items-center gap-1">
                          <span>{v.shopName}</span>
                        </div>
                        <div className="text-xs sm:text-sm font-black text-slate-900">{v.title}</div>
                        <div className="text-[11px] text-slate-600 font-medium">{v.minOrderText}</div>
                        <div className="text-[10px] text-slate-400">
                          HSD: {v.exp}
                        </div>
                      </div>

                      {/* Right Checkbox Selector */}
                      <div className="p-4">
                        <div className={`w-5 h-5 rounded-lg border-2 flex items-center justify-center transition ${
                          isSelected
                            ? 'border-purple-600 bg-purple-600 text-white'
                            : 'border-slate-300 bg-white'
                        }`}>
                          {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

        </div>

        {/* 5. Modal Sticky Bottom Action Bar */}
        <div className="p-4 sm:p-5 bg-white border-t border-slate-200 flex flex-col sm:flex-row items-center justify-between gap-3 shadow-lg">
          <div className="space-y-0.5 text-center sm:text-left">
            <div className="text-xs text-slate-500 font-medium">
              Ước tính tiết kiệm được: <strong className="text-sm font-black text-rose-600">{formatVND(totalPotentialSavings)}</strong>
            </div>
            <div className="flex flex-wrap gap-1 text-[11px] text-slate-600">
              {selectedShipping && <span className="bg-emerald-100 text-emerald-800 px-1.5 py-0.2 rounded font-bold">1 Freeship</span>}
              {selectedPlatform && <span className="bg-orange-100 text-orange-800 px-1.5 py-0.2 rounded font-bold">1 Sàn</span>}
              {Object.keys(selectedShopVouchers).length > 0 && (
                <span className="bg-purple-100 text-purple-800 px-1.5 py-0.2 rounded font-bold">
                  {Object.keys(selectedShopVouchers).length} Shop
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={handleResetAll}
              className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-100 text-xs font-bold transition"
            >
              Bỏ chọn tất cả
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              className="flex-1 sm:flex-none px-8 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-black text-xs sm:text-sm rounded-xl transition shadow-lg shadow-orange-500/25 active:scale-95 uppercase tracking-wide"
            >
              Đồng Ý Áp Dụng
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}

