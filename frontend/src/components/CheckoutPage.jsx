import React, { useState, useMemo, useEffect } from 'react';
import { 
  ShoppingBag, ArrowLeft, Trash2, Store, Tag, CheckCircle2, ShieldCheck, 
  MapPin, Phone, User, Truck, CreditCard, ChevronRight, AlertCircle, 
  Clock, Sparkles, Check, Edit3, Plus, RefreshCw, DollarSign, QrCode, Ticket, ExternalLink, Copy,
  X, ArrowRight, Volume2
} from 'lucide-react';
import { useCartStore } from '../stores/useCartStore';
import { useAuthStore } from '../stores/useAuthStore';
import VoucherModal from './VoucherModal';
import axiosClient from '../api/axiosClient';

export default function CheckoutPage({ 
  checkoutItems = [],
  onBack, 
  onSelectProduct,
  onRequireAuth 
}) {
  const { items, removeMultipleFromCart } = useCartStore();
  const { user, isAuthenticated } = useAuthStore();

  const displayItems = (checkoutItems && checkoutItems.length > 0) ? checkoutItems : items;

  // Shipping & Payment Options
  const [shippingMethod, setShippingMethod] = useState('standard'); // 'standard' | 'express' | 'economy'
  const [paymentMethod, setPaymentMethod] = useState('cod'); // 'cod' | 'vnpay' | 'credit' | 'momo'
  const [orderNote, setOrderNote] = useState('');

  // Shipping Address State
  const [isEditingAddress, setIsEditingAddress] = useState(false);
  const [address, setAddress] = useState({
    name: user?.fullName || user?.username || 'Lê Đức Anh',
    phone: user?.phone || '0988 123 456',
    addressLine: 'Số 18, Phố Nguyễn Trãi, Phường Mộ Lao, Quận Hà Đông',
    city: 'Hà Nội',
    label: 'Nhà riêng (Mặc định)',
  });

  // Voucher states (Phân chia rõ ràng Voucher Sàn & Voucher Shop chuẩn Shopee)
  const [isVoucherModalOpen, setIsVoucherModalOpen] = useState(false);
  const [appliedShippingVoucher, setAppliedShippingVoucher] = useState({
    code: 'FREESHIPMAX',
    discount: 25000,
    title: 'Miễn Phí Vận Chuyển Toàn Quốc',
  });
  const [appliedPlatformVoucher, setAppliedPlatformVoucher] = useState({
    code: 'QUICKSHOP50K',
    discount: 50000,
    title: 'Giảm 50.000₫ Đơn Hàng Sàn',
  });
  const [appliedShopVouchers, setAppliedShopVouchers] = useState({});
  const [voucherCode, setVoucherCode] = useState('');
  const [voucherApplied, setVoucherApplied] = useState('');

  // Order Placement status
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);
  const [orderId, setOrderId] = useState('');
  const [payOsData, setPayOsData] = useState(null);
  const [isGeneratingPayOs, setIsGeneratingPayOs] = useState(false);

  const formatVND = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const [copiedField, setCopiedField] = useState(null);
  const handleCopy = (text, fieldName) => {
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(text);
      setCopiedField(fieldName);
      setTimeout(() => setCopiedField(null), 2000);
    }
  };

  // Payment confirmation states & real-time notification
  const [paymentConfirmed, setPaymentConfirmed] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [isCheckingPayment, setIsCheckingPayment] = useState(false);
  const [confirmedPaymentDetails, setConfirmedPaymentDetails] = useState(null);

  // Âm thanh chuông báo ngân hàng chúc mừng thanh toán thành công (Web Audio API)
  const playSuccessChime = () => {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const now = ctx.currentTime;

      // Nốt 1: D5 (587.33 Hz)
      const osc1 = ctx.createOscillator();
      const gain1 = ctx.createGain();
      osc1.type = 'triangle';
      osc1.frequency.setValueAtTime(587.33, now);
      gain1.gain.setValueAtTime(0.25, now);
      gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
      osc1.connect(gain1);
      gain1.connect(ctx.destination);
      osc1.start(now);
      osc1.stop(now + 0.35);

      // Nốt 2: A5 (880 Hz) - vang cao chúc mừng
      const osc2 = ctx.createOscillator();
      const gain2 = ctx.createGain();
      osc2.type = 'sine';
      osc2.frequency.setValueAtTime(880, now + 0.18);
      gain2.gain.setValueAtTime(0.35, now + 0.18);
      gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.85);
      osc2.connect(gain2);
      gain2.connect(ctx.destination);
      osc2.start(now + 0.18);
      osc2.stop(now + 0.85);
    } catch (e) {
      console.warn('Audio chime unsupported:', e);
    }
  };

  // Kích hoạt thông báo thành công
  const triggerSuccessState = (paymentData) => {
    setPaymentConfirmed(true);
    setConfirmedPaymentDetails(paymentData);
    setShowSuccessModal(true);
    playSuccessChime();
  };

  // 1. Lắng nghe Return URL từ PayOS khi người dùng quay lại sau thanh toán
  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const returnOrderId = searchParams.get('orderId') || searchParams.get('orderCode');

    if (returnOrderId) {
      setOrderId(returnOrderId);
      setOrderSuccess(true);
      // Xoá param trên URL thanh địa chỉ để trải nghiệm mượt mà
      window.history.replaceState({}, document.title, window.location.pathname);

      // BẢO MẬT: Tuyệt đối không tự ý hiển thị thành công bằng tham số URL
      // Bắt buộc gọi API backend để xác nhận trạng thái đơn hàng thực tế
      setIsCheckingPayment(true);
      axiosClient.get(`/api/payments/status/${encodeURIComponent(returnOrderId)}`)
        .then((res) => {
          const data = res?.data?.data || res?.data;
          if (data && (data.paid === true || data.status === 'SUCCESS')) {
            triggerSuccessState(data);
          }
        })
        .catch((err) => {
          console.warn('Lỗi kiểm tra trạng thái thanh toán từ backend:', err);
        })
        .finally(() => {
          setIsCheckingPayment(false);
        });
    }
  }, []);

  // 2. Lắng nghe trạng thái thanh toán thời gian thực qua Polling API mỗi 2.5s
  useEffect(() => {
    if (!orderId || paymentConfirmed) return;

    let isMounted = true;
    const interval = setInterval(async () => {
      try {
        const res = await axiosClient.get(`/api/payments/status/${encodeURIComponent(orderId)}`);
        const data = res?.data?.data || res?.data;
        if (data && (data.paid === true || data.status === 'SUCCESS')) {
          if (isMounted && !paymentConfirmed) {
            triggerSuccessState(data);
          }
        }
      } catch (e) {
        // Tiếp tục lắng nghe chu kỳ kế tiếp
      }
    }, 2500);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [orderId, paymentConfirmed]);

  // 3. Nút kiểm tra thủ công trạng thái thanh toán (kết nối trực tiếp backend)
  const handleManualCheckPayment = async () => {
    if (!orderId) return;
    setIsCheckingPayment(true);
    try {
      const res = await axiosClient.get(`/api/payments/status/${encodeURIComponent(orderId)}`);
      const data = res?.data?.data || res?.data;
      if (data && (data.paid === true || data.status === 'SUCCESS')) {
        triggerSuccessState(data);
      } else {
        alert('Hệ thống đang tiếp tục đợi tiền về tài khoản MB Bank. Vui lòng bấm kiểm tra lại sau vài giây!');
      }
    } catch (e) {
      alert('Chưa nhận được giao dịch. Vui lòng kiểm tra lại nội dung chuyển khoản!');
    } finally {
      setIsCheckingPayment(false);
    }
  };

  // 4. Tạo yêu cầu thanh toán chuyển khoản PayOS / MB Bank thực tế từ backend
  // Backend PayOsService tự động lấy số tiền đơn hàng, tự sinh request và response đầy đủ chỉ với orderId
  const handleCreatePayOsPayment = async (orderIdToPay, amountToPay) => {
    setIsGeneratingPayOs(true);
    try {
      const targetId = String(orderIdToPay || orderId);
      const res = await axiosClient.post('/api/payments/create', {
        orderId: targetId,
        amount: Number(amountToPay || finalTotal)
      });
      const data = res?.data?.data || res?.data;
      if (data) {
        setPayOsData(data);
        return data;
      }
    } catch (e) {
      console.warn('Lỗi tạo yêu cầu thanh toán PayOS:', e);
    } finally {
      setIsGeneratingPayOs(false);
    }
    return null;
  };

  // Gom nhóm sản phẩm thanh toán theo Shop
  const shopGroups = useMemo(() => {
    const groups = {};
    for (const item of displayItems) {
      const sId = item.shopId || 1;
      if (!groups[sId]) {
        groups[sId] = {
          shopId: sId,
          shopName: item.shopName || `Gian hàng #${sId}`,
          items: [],
        };
      }
      groups[sId].items.push(item);
    }
    return Object.values(groups);
  }, [displayItems]);

  const subTotal = useMemo(() => {
    return displayItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }, [displayItems]);

  // Base shipping fee calculation
  let baseShipping = 0;
  if (shippingMethod === 'standard') {
    baseShipping = subTotal > 99000 ? 0 : 25000;
  } else if (shippingMethod === 'express') {
    baseShipping = 35000;
  } else if (shippingMethod === 'economy') {
    baseShipping = 0;
  }

  // Calculate distinct discount types
  const shippingDiscount = appliedShippingVoucher?.discount || 0;
  const platformDiscount = appliedPlatformVoucher?.discount || 0;
  const shopDiscount = Object.values(appliedShopVouchers).reduce((sum, v) => sum + (v.discount || 0), 0);
  const totalDiscount = platformDiscount + shopDiscount;

  const actualShippingFee = Math.max(0, baseShipping - shippingDiscount);
  const finalTotal = Math.max(0, subTotal + actualShippingFee - totalDiscount);

  // Handle vouchers from modal
  const handleApplyVouchers = ({ shippingVoucher, platformVoucher, shopVouchers }) => {
    setAppliedShippingVoucher(shippingVoucher);
    setAppliedPlatformVoucher(platformVoucher);
    setAppliedShopVouchers(shopVouchers || {});
    setVoucherApplied('✓ Đã cập nhật áp dụng voucher thành công!');
  };

  const handleQuickApplyCode = (codeToApply) => {
    const code = (codeToApply || voucherCode).trim().toUpperCase();
    if (code === 'QUICKSHOP50K') {
      if (subTotal >= 200000) {
        setAppliedPlatformVoucher({ code: 'QUICKSHOP50K', discount: 50000, title: 'Giảm 50.000₫ Đơn Hàng Sàn' });
        setVoucherApplied('✓ Đã áp dụng mã Sàn QUICKSHOP50K (-50.000₫)!');
      } else {
        setVoucherApplied('Mã QUICKSHOP50K yêu cầu đơn hàng từ 200.000₫.');
      }
    } else if (code === 'FREESHIPMAX') {
      setAppliedShippingVoucher({ code: 'FREESHIPMAX', discount: 25000, title: 'Miễn Phí Vận Chuyển' });
      setVoucherApplied('✓ Đã áp dụng mã Freeship FREESHIPMAX (-25.000₫)!');
    } else if (code === 'MALL100K') {
      if (subTotal >= 1000000) {
        setAppliedPlatformVoucher({ code: 'MALL100K', discount: 100000, title: 'Giảm 100.000₫ Đơn Mall' });
        setVoucherApplied('✓ Đã áp dụng mã Sàn MALL100K (-100.000₫)!');
      } else {
        setVoucherApplied('Mã MALL100K yêu cầu đơn hàng từ 1.000.000₫.');
      }
    } else if (code.includes('SHOP') || code.includes('TECH') || code.includes('SMART')) {
      setAppliedShopVouchers((prev) => ({
        ...prev,
        1: { code, title: 'Voucher Shop Đối Tác', discount: 30000, shopId: 1, shopName: 'TechWorld' }
      }));
      setVoucherApplied(`✓ Đã áp dụng mã Shop ${code} (-30.000₫)!`);
    } else {
      setVoucherApplied('Mã giảm giá không hợp lệ hoặc đã hết lượt áp dụng.');
    }
  };

  const handlePlaceOrder = async () => {
    if (!isAuthenticated) {
      if (onRequireAuth) {
        onRequireAuth('Vui lòng đăng nhập để tiến hành đặt mua hàng!');
      }
      return;
    }

    if (displayItems.length === 0) return;

    setIsSubmitting(true);
    const userId = user?.userId || user?.id || 1001;

    try {
      const isBankTransfer = paymentMethod === 'payos' || paymentMethod === 'vnpay';
      const payload = {
        paymentMethod: isBankTransfer ? 'PAYOS' : paymentMethod.toUpperCase(),
        recipientName: address.name || 'Người nhận',
        recipientPhone: address.phone || '0987654321',
        shippingAddress: `${address.addressLine || ''}, ${address.city || 'TP. Hồ Chí Minh'}`.trim(),
        note: orderNote || '',
        voucherCode: appliedPlatformVoucher?.code || '',
        items: displayItems.map((item) => ({
          variantId: Number(item.variantId || item.id),
          quantity: Number(item.quantity) || 1,
        })),
      };

      const res = await axiosClient.post('/api/orders/checkout', payload, {
        headers: {
          'X-User-Id': userId,
        },
      });

      let createdOrderId = null;
      let finalAmount = finalTotal;

      if (res && (res.success || res.code === 200 || res.status === 200 || res.status === 201) && res.data) {
        const orderData = res.data?.data || res.data;
        const realOrderId = orderData.masterOrderId ? String(orderData.masterOrderId) : (orderData.orderCode || '1001');
        finalAmount = orderData.totalAmount || finalTotal;
        const displayOrderId = orderData.orderCode || `QS-${orderData.masterOrderId || realOrderId}`;
        createdOrderId = displayOrderId;
        setOrderId(displayOrderId);
        setOrderSuccess(true);
        removeMultipleFromCart(displayItems.map((i) => i.variantId));

        // NẾU CHỌN THANH TOÁN CHUYỂN KHOẢN: Khởi tạo URL PayOS thực tế từ Backend
        if (isBankTransfer) {
          handleCreatePayOsPayment(displayOrderId, finalAmount);
        }
      } else {
        alert(res?.message || 'Không thể tạo đơn hàng. Vui lòng kiểm tra lại thông tin giỏ hàng!');
      }
    } catch (err) {
      console.error('Lỗi khi đặt hàng:', err);
      alert(err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi gửi yêu cầu đặt hàng. Vui lòng thử lại!');
    } finally {
      setIsSubmitting(false);
    }
  };

  // =========================================================================
  // GIAO DIỆN TOÀN MÀN HÌNH: ĐÃ THANH TOÁN THÀNH CÔNG (PAYMENT SUCCESS SCREEN)
  // =========================================================================
  if (paymentConfirmed) {
    return (
      <div className="max-w-3xl mx-auto py-8 sm:py-12 px-4 animate-in fade-in zoom-in-95 duration-400">
        <div className="bg-white rounded-3xl border border-emerald-200/90 p-6 sm:p-12 shadow-xl text-center space-y-6 relative overflow-hidden">
          {/* Dải gradient vinh danh ở đỉnh thẻ */}
          <div className="absolute top-0 left-0 right-0 h-3 bg-gradient-to-r from-emerald-500 via-teal-500 to-emerald-600"></div>

          {/* Biểu tượng tích xanh chuyển động phát sáng */}
          <div className="relative mx-auto w-24 h-24 pt-2">
            <div className="absolute inset-0 rounded-full bg-emerald-400/25 animate-ping"></div>
            <div className="relative w-24 h-24 rounded-full bg-gradient-to-br from-emerald-500 to-teal-600 text-white flex items-center justify-center shadow-xl shadow-emerald-500/30">
              <CheckCircle2 className="w-14 h-14 stroke-[2.5]" />
            </div>
          </div>

          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-4 py-1.5 bg-emerald-100 text-emerald-800 text-xs font-black rounded-full uppercase tracking-wider border border-emerald-300">
              <Sparkles className="w-3.5 h-3.5 text-amber-500" />
              <span>Giao dịch thanh toán hoàn tất</span>
            </div>
            <h1 className="text-2xl sm:text-4xl font-black text-slate-900 tracking-tight">
              ĐÃ THANH TOÁN THÀNH CÔNG!
            </h1>
            <p className="text-xs sm:text-sm text-slate-600 max-w-lg mx-auto leading-relaxed">
              Hệ thống QuickShop đã ghi nhận khoản thanh toán chuyển khoản qua MB Bank cho đơn hàng{' '}
              <strong className="text-emerald-700 font-mono font-bold text-sm">#{confirmedPaymentDetails?.orderId || orderId}</strong>.
              Đơn hàng của bạn đã chuyển sang trạng thái <strong>ĐÃ THANH TOÁN (PAID)</strong> và đang được người bán chuẩn bị để giao đến bạn sớm nhất.
            </p>
          </div>

          {/* Thẻ Biên lai Thanh toán Chi tiết */}
          <div className="bg-gradient-to-br from-emerald-50/90 via-white to-emerald-50/50 rounded-2xl p-5 sm:p-6 border-2 border-emerald-400/70 shadow-sm text-left space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-emerald-100">
              <div>
                <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Tổng tiền đã thanh toán</span>
                <div className="text-2xl sm:text-3xl font-black text-emerald-600 pt-0.5">
                  {formatVND(confirmedPaymentDetails?.amount || finalTotal)}
                </div>
              </div>
              <div className="inline-flex items-center gap-1.5 px-3.5 py-1.5 bg-emerald-600 text-white font-black text-xs rounded-xl shadow-sm self-start sm:self-auto">
                <ShieldCheck className="w-4 h-4" />
                <span>ĐÃ THANH TOÁN (PAID)</span>
              </div>
            </div>

            {/* Grid Thông tin Giao dịch Ngân Hàng */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div className="bg-white p-3.5 rounded-xl border border-emerald-100 space-y-1">
                <span className="text-slate-500">Mã đơn hàng:</span>
                <div className="font-mono font-black text-slate-900 text-sm">#{confirmedPaymentDetails?.orderId || orderId}</div>
              </div>
              <div className="bg-white p-3.5 rounded-xl border border-emerald-100 space-y-1">
                <span className="text-slate-500">Mã tham chiếu ngân hàng:</span>
                <div className="font-mono font-black text-emerald-700 text-sm">
                  {confirmedPaymentDetails?.transactionNo || confirmedPaymentDetails?.txnRef || ('MB_' + (confirmedPaymentDetails?.orderId || orderId))}
                </div>
              </div>
              <div className="bg-white p-3.5 rounded-xl border border-emerald-100 space-y-1">
                <span className="text-slate-500">Ngân hàng thụ hưởng:</span>
                <div className="font-bold text-blue-900">MB Bank (Ngân hàng TMCP Quân Đội)</div>
              </div>
              <div className="bg-white p-3.5 rounded-xl border border-emerald-100 space-y-1">
                <span className="text-slate-500">Số tài khoản & Chủ sở hữu:</span>
                <div className="font-mono font-bold text-slate-900">0346746521 - NGUYỄN VĂN KIÊN</div>
              </div>
            </div>

            <div className="text-[11px] text-slate-500 flex items-center gap-1.5 pt-1">
              <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>Giao dịch được bảo hộ thanh toán ký quỹ an toàn (QuickShop Escrow Safe Pay).</span>
            </div>
          </div>

          {/* Thông tin người nhận hàng */}
          <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/80 text-left text-xs space-y-2.5">
            <div className="flex justify-between pb-2 border-b border-slate-200 font-bold text-slate-700 uppercase tracking-wide">
              <span>Thông tin nhận hàng</span>
              <span className="text-emerald-700 font-semibold">{shippingMethod === 'express' ? 'Hỏa Tốc 2H' : 'Tiêu Chuẩn 1-2 Ngày'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Người nhận:</span>
              <span className="font-bold text-slate-900">{address.name} ({address.phone})</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Địa chỉ:</span>
              <span className="font-medium text-slate-800 text-right max-w-[280px] sm:max-w-sm">
                {address.addressLine}, {address.city}
              </span>
            </div>
          </div>

          {/* Timeline tiến trình đơn hàng */}
          <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/80 text-left space-y-3">
            <div className="text-xs font-bold text-slate-700 uppercase tracking-wide">Tiến độ đơn hàng</div>
            <div className="grid grid-cols-4 gap-2 text-center text-[11px]">
              <div className="space-y-1">
                <div className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center mx-auto font-bold shadow-sm">
                  <Check className="w-4 h-4" />
                </div>
                <span className="font-bold text-emerald-700">Đã đặt hàng</span>
              </div>
              <div className="space-y-1">
                <div className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center mx-auto font-bold shadow-sm animate-bounce">
                  <Check className="w-4 h-4" />
                </div>
                <span className="font-black text-emerald-700">Đã thanh toán</span>
              </div>
              <div className="space-y-1">
                <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center mx-auto font-bold">
                  3
                </div>
                <span className="text-slate-500">Chuẩn bị hàng</span>
              </div>
              <div className="space-y-1">
                <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center mx-auto font-bold">
                  4
                </div>
                <span className="text-slate-500">Đang giao</span>
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="pt-2 flex flex-col sm:flex-row justify-center gap-3">
            <button
              onClick={onBack}
              className="px-8 py-3.5 bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-700 hover:from-emerald-700 hover:to-teal-800 text-white font-bold text-xs rounded-xl shadow-lg shadow-emerald-500/25 transition active:scale-95 flex items-center justify-center gap-2"
            >
              <span>Tiếp tục mua sắm các deal sốc khác</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => window.print()}
              className="px-6 py-3.5 bg-slate-100 hover:bg-slate-200 text-slate-800 font-bold text-xs rounded-xl transition active:scale-95"
            >
              In biên lai thanh toán
            </button>
          </div>
        </div>
      </div>
    );
  }

  // =========================================================================
  // MÀN HÌNH CHỜ THANH TOÁN (ORDER PLACED / PENDING PAYMENT)
  // =========================================================================
  if (orderSuccess) {
    return (
      <div className="max-w-3xl mx-auto py-8 px-4 animate-in fade-in duration-300">
        <div className="bg-white rounded-3xl border border-slate-200/90 p-8 sm:p-12 shadow-sm text-center space-y-6">
          <div className="w-20 h-20 rounded-full bg-blue-100 text-blue-600 mx-auto flex items-center justify-center shadow-inner">
            <QrCode className="w-10 h-10 stroke-[2.5]" />
          </div>

          <div className="space-y-2">
            <span className="inline-block px-3 py-1 bg-blue-50 text-blue-700 text-xs font-bold rounded-full uppercase tracking-wider">
              {paymentMethod === 'vnpay' ? 'Đang chờ thanh toán chuyển khoản' : 'Đặt hàng thành công'}
            </span>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900">
              {paymentMethod === 'vnpay' ? 'Vui lòng quét mã QR để hoàn tất thanh toán' : 'Cảm ơn bạn đã mua hàng tại QuickShop!'}
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
              Đơn hàng <strong className="text-orange-600 font-mono">#{orderId}</strong> của bạn đã được khởi tạo. Sau khi thanh toán, màn hình sẽ tự động cập nhật sang giao diện <strong>ĐÃ THANH TOÁN THÀNH CÔNG</strong>.
            </p>
          </div>

          {/* Order Details Brief Box */}
          <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200/80 text-left text-xs space-y-3 max-w-lg mx-auto">
            <div className="flex justify-between pb-2 border-b border-slate-200">
              <span className="text-slate-500">Mã đơn hàng:</span>
              <span className="font-mono font-bold text-slate-900">{orderId}</span>
            </div>
            <div className="flex justify-between pb-2 border-b border-slate-200">
              <span className="text-slate-500">Người nhận hàng:</span>
              <span className="font-semibold text-slate-900">{address.name} ({address.phone})</span>
            </div>
            <div className="flex justify-between pb-2 border-b border-slate-200">
              <span className="text-slate-500">Địa chỉ giao:</span>
              <span className="font-medium text-slate-800 text-right max-w-[280px]">{address.addressLine}, {address.city}</span>
            </div>
            <div className="flex justify-between pb-2 border-b border-slate-200">
              <span className="text-slate-500">Phương thức thanh toán:</span>
              <span className="font-semibold text-slate-900">
                {paymentMethod === 'cod' && 'Thanh toán tiền mặt khi nhận hàng (COD)'}
                {(paymentMethod === 'payos' || paymentMethod === 'vnpay') && 'Chuyển khoản QR Ngân Hàng (PayOS / MB Bank / VietQR)'}
                {paymentMethod === 'credit' && 'Thẻ tín dụng / Ghi nợ Quốc tế'}
                {paymentMethod === 'momo' && 'Ví điện tử MoMo'}
              </span>
            </div>
            <div className="flex justify-between pt-1 text-sm font-bold">
              <span className="text-slate-900">Tổng thanh toán:</span>
              <span className="text-orange-600 font-black">{formatVND(finalTotal)}</span>
            </div>
          </div>

          {/* Hộp Mã QR PayOS / MB Bank */}
          {(paymentMethod === 'payos' || paymentMethod === 'vnpay') && (
            <div className="bg-gradient-to-br from-blue-50/90 via-white to-blue-50/50 rounded-2xl p-6 border-2 border-blue-400/50 shadow-sm max-w-lg mx-auto text-center space-y-4">
              <div className="flex items-center justify-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                <span className="text-xs font-bold text-blue-900 uppercase tracking-wide">
                  Mã QR Chuyển Khoản PayOS / VietQR cho đơn hàng #{orderId}
                </span>
              </div>

              {/* Thanh trạng thái tự động lắng nghe thời gian thực */}
              <div className="flex items-center justify-center gap-2 text-[11px] font-semibold text-blue-800 bg-blue-100/70 py-1.5 px-3 rounded-full border border-blue-200">
                <span className="w-2 h-2 rounded-full bg-blue-600 animate-ping"></span>
                <span>Đang tự động lắng nghe chuyển khoản từ MB Bank (Tự động báo khi tiền về)</span>
              </div>

              <div className="p-3 bg-white rounded-2xl border-2 border-blue-200 inline-block shadow-md">
                <img
                  src={
                    payOsData?.qrCode && payOsData.qrCode.startsWith('http')
                      ? payOsData.qrCode
                      : `https://img.vietqr.io/image/970422-0346746521-compact2.png?amount=${finalTotal}&addInfo=${encodeURIComponent('QS DH' + (orderId.replace(/[^0-9]/g, '') || orderId))}&accountName=NGUYEN%20VAN%20KIEN`
                  }
                  alt="Mã QR PayOS MB Bank"
                  className="w-48 h-48 object-contain rounded-xl mx-auto"
                />
                <div className="mt-1.5 text-[11px] font-bold text-blue-700 bg-blue-50 py-1 px-3 rounded-lg">
                  Quét mã bằng MB Bank hoặc mọi App Ngân Hàng
                </div>
              </div>

              <div className="text-xs text-slate-600 bg-white/80 p-3.5 rounded-xl border border-blue-100 text-left space-y-2">
                <div className="flex justify-between items-center py-1 border-b border-slate-100">
                  <span className="text-slate-500">Ngân hàng:</span>
                  <span className="font-bold text-blue-900">MB Bank (Ngân hàng TMCP Quân Đội)</span>
                </div>
                <div className="flex justify-between items-center py-1 border-b border-slate-100">
                  <span className="text-slate-500">Số tài khoản:</span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-black text-blue-700 bg-blue-50 px-2.5 py-0.5 rounded text-sm tracking-wider">
                      {payOsData?.accountNumber || '0346746521'}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopy(payOsData?.accountNumber || '0346746521', 'stk_success')}
                      className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                    >
                      {copiedField === 'stk_success' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedField === 'stk_success' ? 'Đã chép' : 'Sao chép'}</span>
                    </button>
                  </div>
                </div>
                <div className="flex justify-between items-center py-1 border-b border-slate-100">
                  <span className="text-slate-500">Chủ sở hữu (Chủ TK):</span>
                  <span className="font-bold text-slate-900 uppercase">{payOsData?.accountName || 'NGUYỄN VĂN KIÊN'}</span>
                </div>
                <div className="flex justify-between items-center py-1 border-b border-slate-100">
                  <span className="text-slate-500">Số tiền:</span>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-rose-600 text-sm">{formatVND(finalTotal)}</span>
                    <button
                      type="button"
                      onClick={() => handleCopy(String(finalTotal), 'amount_success')}
                      className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                    >
                      {copiedField === 'amount_success' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedField === 'amount_success' ? 'Đã chép' : 'Sao chép'}</span>
                    </button>
                  </div>
                </div>
                <div className="flex justify-between items-center py-1">
                  <span className="text-slate-500">Nội dung chuyển khoản:</span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-900 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                      {payOsData?.orderCode ? `QS DH${payOsData.orderCode}` : `QS DH${orderId.replace(/[^0-9]/g, '') || orderId}`}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopy(payOsData?.orderCode ? `QS DH${payOsData.orderCode}` : `QS DH${orderId.replace(/[^0-9]/g, '') || orderId}`, 'content_success')}
                      className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                    >
                      {copiedField === 'content_success' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                      <span>{copiedField === 'content_success' ? 'Đã chép' : 'Sao chép'}</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* Nút kiểm tra thủ công & mở trang PayOS */}
              <div className="flex flex-col sm:flex-row items-center justify-center gap-2 pt-1">
                <button
                  type="button"
                  onClick={handleManualCheckPayment}
                  disabled={isCheckingPayment}
                  className="w-full sm:w-auto px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-xl shadow transition active:scale-95 flex items-center justify-center gap-1.5"
                >
                  {isCheckingPayment ? (
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <RefreshCw className="w-3.5 h-3.5" />
                  )}
                  <span>Kiểm tra trạng thái ngay</span>
                </button>
                {payOsData?.checkoutUrl && (
                  <a
                    href={payOsData.checkoutUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="w-full sm:w-auto px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow transition active:scale-95 flex items-center justify-center gap-1.5"
                  >
                    <ExternalLink className="w-3.5 h-3.5" />
                    <span>Mở trang thanh toán PayOS</span>
                  </a>
                )}
              </div>

              <p className="text-[11px] text-slate-500">
                Sau khi chuyển khoản thành công, hệ thống sẽ tự động phát hiện, phát âm thanh chuông báo và chuyển ngay sang giao diện <strong>ĐÃ THANH TOÁN THÀNH CÔNG</strong>!
              </p>
            </div>
          )}

          <div className="pt-2 flex flex-col sm:flex-row justify-center gap-3">
            {payOsData?.checkoutUrl ? (
              <a
                href={payOsData.checkoutUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-blue-500/20 transition active:scale-95"
              >
                <span>Mở Cổng Thanh Toán PayOS (MB Bank)</span>
                <ExternalLink className="w-4 h-4" />
              </a>
            ) : (paymentMethod === 'payos' || paymentMethod === 'vnpay') && (
              <button
                type="button"
                onClick={async () => {
                  const data = await handleCreatePayOsPayment(orderId, finalTotal);
                  if (data?.checkoutUrl) window.open(data.checkoutUrl, '_blank');
                }}
                disabled={isGeneratingPayOs}
                className="inline-flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-blue-500/20 transition active:scale-95"
              >
                {isGeneratingPayOs ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <ExternalLink className="w-4 h-4" />
                )}
                <span>Mở Cổng Thanh Toán PayOS</span>
              </button>
            )}
            <button
              onClick={onBack}
              className="px-6 py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-orange-500/20 transition active:scale-95"
            >
              Tiếp tục mua sắm các deal sốc khác
            </button>
          </div>
        </div>
      </div>
    );
  }

  // EMPTY CHECKOUT ITEMS SCREEN
  if (displayItems.length === 0) {
    return (
      <div className="max-w-4xl mx-auto py-12 px-4 animate-in fade-in duration-300">
        <div className="bg-white rounded-3xl border border-slate-200/90 p-10 sm:p-16 text-center space-y-4 shadow-sm">
          <div className="w-20 h-20 rounded-3xl bg-orange-50 text-orange-400 mx-auto flex items-center justify-center shadow-inner">
            <ShoppingBag className="w-10 h-10" />
          </div>
          <h2 className="text-xl font-black text-slate-800">Chưa có sản phẩm nào được chọn để thanh toán</h2>
          <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
            Vui lòng quay lại Giỏ hàng và tích chọn các sản phẩm bạn muốn đặt mua trước khi thanh toán.
          </p>
          <div className="pt-4">
            <button
              onClick={onBack}
              className="px-6 py-3 bg-gradient-to-r from-orange-500 to-rose-600 text-white font-bold text-xs rounded-xl shadow-lg shadow-orange-500/20 hover:from-orange-600 hover:to-rose-700 transition inline-flex items-center gap-2 active:scale-95"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Quay lại Giỏ hàng</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-16 animate-in fade-in duration-300">
      {/* 1. Header Back Navigation & Breadcrumb */}
      <div className="bg-white rounded-3xl border border-slate-200/90 p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
        <button
          onClick={onBack}
          className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-orange-50 hover:text-orange-600 text-slate-700 transition flex items-center gap-2 text-xs font-bold active:scale-95 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại Giỏ hàng</span>
        </button>

        <div className="flex items-center gap-2 text-xs text-slate-500 font-medium">
          <span className="text-slate-400 cursor-pointer hover:text-orange-600" onClick={onBack}>Giỏ hàng</span>
          <ChevronRight className="w-3.5 h-3.5" />
          <span className="text-orange-600 font-bold">Thanh toán đơn hàng</span>
          <span className="bg-orange-100 text-orange-700 font-bold px-2 py-0.5 rounded-full text-[11px] ml-1">
            {displayItems.reduce((s, i) => s + i.quantity, 0)} sản phẩm đã chọn
          </span>
        </div>
      </div>

      {/* 2. Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        
        {/* LEFT COLUMN (2/3 width): Shipping Address, Items by Shop, Delivery & Payment */}
        <div className="lg:col-span-2 space-y-6">

          {/* Section A: Shipping Address Card */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-4">
            <div className="flex justify-between items-center pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2 text-sm font-black text-slate-900 uppercase tracking-wide">
                <MapPin className="w-4 h-4 text-orange-600" />
                <span>Địa Chỉ Nhận Hàng</span>
              </div>
              <button
                onClick={() => setIsEditingAddress(!isEditingAddress)}
                className="text-xs font-bold text-orange-600 hover:text-orange-700 flex items-center gap-1 transition"
              >
                <Edit3 className="w-3.5 h-3.5" />
                <span>{isEditingAddress ? 'Xong' : 'Thay đổi'}</span>
              </button>
            </div>

            {isEditingAddress ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs pt-1">
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Họ và tên người nhận:</label>
                  <input
                    type="text"
                    value={address.name}
                    onChange={(e) => setAddress({ ...address, name: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-orange-500"
                  />
                </div>
                <div>
                  <label className="block text-slate-600 font-semibold mb-1">Số điện thoại liên hệ:</label>
                  <input
                    type="text"
                    value={address.phone}
                    onChange={(e) => setAddress({ ...address, phone: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-orange-500"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-slate-600 font-semibold mb-1">Địa chỉ chi tiết (Số nhà, đường, phường, quận):</label>
                  <input
                    type="text"
                    value={address.addressLine}
                    onChange={(e) => setAddress({ ...address, addressLine: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:border-orange-500"
                  />
                </div>
              </div>
            ) : (
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-orange-50/40 p-4 rounded-2xl border border-orange-100">
                <div className="space-y-1 text-xs">
                  <div className="flex items-center gap-2">
                    <span className="font-black text-slate-900 text-sm">{address.name}</span>
                    <span className="font-bold text-slate-600">({address.phone})</span>
                    <span className="bg-orange-100 text-orange-700 font-bold text-[10px] px-2 py-0.5 rounded-md">
                      {address.label}
                    </span>
                  </div>
                  <div className="text-slate-600">
                    {address.addressLine}, {address.city}
                  </div>
                </div>
                <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
                  ✓ Giao tận cửa
                </span>
              </div>
            )}
          </div>

          {/* Section B: Cart Items Grouped by Shop */}
          <div className="space-y-4">
            <div className="text-sm font-black text-slate-900 uppercase tracking-wide flex items-center gap-2 px-1">
              <ShoppingBag className="w-4 h-4 text-orange-600" />
              <span>Sản Phẩm Trong Đơn Hàng</span>
            </div>

            {shopGroups.map((group) => (
              <div key={group.shopId} className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
                {/* Shop Header */}
                <div className="bg-slate-50 p-4 border-b border-slate-100 flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Store className="w-4 h-4 text-orange-600" />
                    <span className="font-bold text-xs sm:text-sm text-slate-900">{group.shopName}</span>
                    <span className="bg-rose-600 text-white text-[9px] font-black px-1.5 py-0.5 rounded uppercase">
                      Mall
                    </span>
                  </div>
                  <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-100">
                    Giao từ Hà Nội • Chuẩn bị nhanh
                  </span>
                </div>

                {/* Items in Shop */}
                <div className="divide-y divide-slate-100 p-4">
                  {group.items.map((item) => (
                    <div key={item.variantId} className="py-3.5 flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
                      {/* Product Thumbnail & Details */}
                      <div className="flex gap-3.5 items-center flex-1 min-w-0">
                        <img
                          src={item.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                          alt={item.productName}
                          className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl object-cover bg-slate-50 border border-slate-200/80 flex-shrink-0"
                        />
                        <div className="space-y-1 min-w-0">
                          <h4 
                            onClick={() => onSelectProduct && onSelectProduct({ id: item.productId, name: item.productName })}
                            className="text-xs sm:text-sm font-bold text-slate-800 line-clamp-2 hover:text-orange-600 transition cursor-pointer"
                          >
                            {item.productName}
                          </h4>
                          <div className="text-[11px] text-slate-500 flex flex-wrap gap-2">
                            {item.color && (
                              <span className="bg-slate-100 px-2 py-0.5 rounded-md font-medium text-slate-700">
                                Màu: {item.color}
                              </span>
                            )}
                            {item.size && (
                              <span className="bg-slate-100 px-2 py-0.5 rounded-md font-medium text-slate-700">
                                Size: {item.size}
                              </span>
                            )}
                          </div>
                          <div className="text-xs sm:text-sm font-black text-rose-600">
                            {formatVND(item.price)}
                          </div>
                        </div>
                      </div>

                      {/* Quantity Controls & Total for this item */}
                      <div className="flex items-center justify-between sm:justify-end gap-4 w-full sm:w-auto pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                        {/* Quantity Counter */}
                        <div className="flex items-center border border-slate-200 rounded-xl overflow-hidden shadow-inner bg-slate-50">
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity - 1)}
                            className="px-3 py-1 bg-white hover:bg-slate-100 font-bold text-slate-700 transition active:scale-95"
                          >
                            -
                          </button>
                          <span className="px-3.5 py-1 text-xs font-bold text-slate-900 bg-white">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => updateQuantity(item.variantId, item.quantity + 1)}
                            className="px-3 py-1 bg-white hover:bg-slate-100 font-bold text-slate-700 transition active:scale-95"
                          >
                            +
                          </button>
                        </div>

                        {/* Line Total */}
                        <div className="text-right min-w-[100px]">
                          <div className="text-xs sm:text-sm font-black text-orange-600">
                            {formatVND(item.price * item.quantity)}
                          </div>
                        </div>

                        {/* Delete Button */}
                        <button
                          onClick={() => removeFromCart(item.variantId)}
                          className="p-2 text-slate-400 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition"
                          title="Xóa sản phẩm này"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {/* Section C: Shipping Method Selection */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-sm font-black text-slate-900 uppercase tracking-wide pb-2 border-b border-slate-100">
              <Truck className="w-4 h-4 text-orange-600" />
              <span>Phương Thức Vận Chuyển</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {/* Option 1: Standard */}
              <div
                onClick={() => setShippingMethod('standard')}
                className={`p-3.5 rounded-2xl border transition cursor-pointer flex flex-col justify-between space-y-2 ${
                  shippingMethod === 'standard'
                    ? 'border-orange-500 bg-orange-50/50 ring-2 ring-orange-500/20'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-slate-800">Giao Nhanh Tiêu Chuẩn</span>
                  {shippingMethod === 'standard' && <Check className="w-4 h-4 text-orange-600" />}
                </div>
                <div className="text-[11px] text-slate-500">Nhận hàng sau 1 - 2 ngày làm việc</div>
                <div className="text-xs font-black text-emerald-600">
                  {subTotal > 99000 ? 'Miễn phí (Freeship)' : '25.000₫'}
                </div>
              </div>

              {/* Option 2: Express */}
              <div
                onClick={() => setShippingMethod('express')}
                className={`p-3.5 rounded-2xl border transition cursor-pointer flex flex-col justify-between space-y-2 ${
                  shippingMethod === 'express'
                    ? 'border-orange-500 bg-orange-50/50 ring-2 ring-orange-500/20'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-slate-800">Giao Hỏa Tốc 2H</span>
                  {shippingMethod === 'express' && <Check className="w-4 h-4 text-orange-600" />}
                </div>
                <div className="text-[11px] text-slate-500">Nhận ngay trong 2 giờ nội thành</div>
                <div className="text-xs font-black text-orange-600">35.000₫</div>
              </div>

              {/* Option 3: Economy */}
              <div
                onClick={() => setShippingMethod('economy')}
                className={`p-3.5 rounded-2xl border transition cursor-pointer flex flex-col justify-between space-y-2 ${
                  shippingMethod === 'economy'
                    ? 'border-orange-500 bg-orange-50/50 ring-2 ring-orange-500/20'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-slate-800">Giao Tiết Kiệm</span>
                  {shippingMethod === 'economy' && <Check className="w-4 h-4 text-orange-600" />}
                </div>
                <div className="text-[11px] text-slate-500">Nhận hàng sau 3 - 4 ngày</div>
                <div className="text-xs font-black text-emerald-600">Miễn phí 0₫</div>
              </div>
            </div>
          </div>

          {/* Section D: Payment Method Selection */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-sm font-black text-slate-900 uppercase tracking-wide pb-2 border-b border-slate-100">
              <CreditCard className="w-4 h-4 text-orange-600" />
              <span>Phương Thức Thanh Toán</span>
            </div>

            <div className="space-y-2.5">
              {/* Option 1: COD */}
              <label
                onClick={() => setPaymentMethod('cod')}
                className={`p-3.5 rounded-2xl border transition cursor-pointer flex items-center justify-between ${
                  paymentMethod === 'cod'
                    ? 'border-orange-500 bg-orange-50/50 ring-2 ring-orange-500/20'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold text-xs">
                    <DollarSign className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900">Thanh toán khi nhận hàng (COD)</div>
                    <div className="text-[11px] text-slate-500">Được đồng kiểm và thử hàng trước khi thanh toán tiền mặt</div>
                  </div>
                </div>
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === 'cod'}
                  onChange={() => setPaymentMethod('cod')}
                  className="accent-orange-600 w-4 h-4"
                />
              </label>

              {/* Option 2: VNPay QR */}
              <div className="space-y-3">
                <label
                  onClick={() => setPaymentMethod('payos')}
                  className={`p-3.5 rounded-2xl border transition cursor-pointer flex items-center justify-between ${
                    paymentMethod === 'payos' || paymentMethod === 'vnpay'
                      ? 'border-blue-500 bg-blue-50/50 ring-2 ring-blue-500/20'
                      : 'border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-xs">
                      <QrCode className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="text-xs font-bold text-slate-900 flex items-center gap-2">
                        <span>Chuyển khoản QR Ngân Hàng (PayOS / MB Bank 24/7)</span>
                        <span className="px-1.5 py-0.5 bg-blue-600 text-white font-extrabold text-[9px] rounded-md tracking-wider">
                          PayOS & VietQR
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-500">Quét mã QR chuyển khoản tức thì 24/7 qua MB Bank hoặc mọi App Ngân Hàng</div>
                    </div>
                  </div>
                  <input
                    type="radio"
                    name="payment"
                    checked={paymentMethod === 'payos' || paymentMethod === 'vnpay'}
                    onChange={() => setPaymentMethod('payos')}
                    className="accent-blue-600 w-4 h-4"
                  />
                </label>

                {/* Khung Mã QR PayOS / VietQR (MB Bank) hiện ra ngay khi ấn chọn Chuyển khoản */}
                {(paymentMethod === 'payos' || paymentMethod === 'vnpay') && (
                  <div className="p-4 sm:p-5 bg-gradient-to-br from-blue-50 via-white to-blue-50/60 rounded-2xl border-2 border-blue-400/60 shadow-sm space-y-4 animate-in fade-in slide-in-from-top-2 duration-300">
                    <div className="flex items-center justify-between pb-3 border-b border-blue-100">
                      <div className="flex items-center gap-2">
                        <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                        <span className="text-xs font-bold text-blue-900 uppercase tracking-wide">
                          Mã QR Chuyển Khoản PayOS / VietQR (MB Bank)
                        </span>
                      </div>
                      <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] font-bold rounded-full flex items-center gap-1">
                        <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                        Tài khoản MB Bank chính chủ
                      </span>
                    </div>

                    <div className="flex flex-col sm:flex-row items-center gap-4">
                      {/* Ảnh mã QR Quét thanh toán */}
                      <div className="relative p-2.5 bg-white rounded-2xl border-2 border-blue-200 shadow-md flex-shrink-0 text-center">
                        <img
                          src={`https://img.vietqr.io/image/970422-0346746521-compact2.png?amount=${finalTotal}&addInfo=${encodeURIComponent('QS THANHTOAN')}&accountName=NGUYEN%20VAN%20KIEN`}
                          alt="Mã QR Chuyển khoản PayOS MB Bank"
                          className="w-48 h-48 object-contain rounded-xl mx-auto"
                        />
                        <div className="mt-2 text-[10px] font-bold text-blue-700 bg-blue-50 py-1 px-2 rounded-lg">
                          Quét mã bằng MB Bank hoặc mọi App Ngân Hàng
                        </div>
                      </div>

                      {/* Thông tin chuyển khoản chi tiết */}
                      <div className="text-xs space-y-2 flex-1 w-full bg-white/80 p-3.5 rounded-xl border border-blue-100">
                        <div className="flex justify-between items-center py-1 border-b border-slate-100">
                          <span className="text-slate-500">Ngân hàng thụ hưởng:</span>
                          <span className="font-bold text-blue-900">MB Bank (Ngân hàng TMCP Quân Đội)</span>
                        </div>
                        <div className="flex justify-between items-center py-1 border-b border-slate-100">
                          <span className="text-slate-500">Số tài khoản:</span>
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-black text-blue-700 bg-blue-50 px-2.5 py-1 rounded text-sm sm:text-base tracking-wider">
                              0346746521
                            </span>
                            <button
                              type="button"
                              onClick={() => handleCopy('0346746521', 'stk_opt')}
                              className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                            >
                              {copiedField === 'stk_opt' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                              <span>{copiedField === 'stk_opt' ? 'Đã chép' : 'Sao chép'}</span>
                            </button>
                          </div>
                        </div>
                        <div className="flex justify-between items-center py-1 border-b border-slate-100">
                          <span className="text-slate-500">Tên người nhận (Chủ TK):</span>
                          <span className="font-bold text-slate-900 uppercase">NGUYỄN VĂN KIÊN</span>
                        </div>
                        <div className="flex justify-between items-center py-1 border-b border-slate-100">
                          <span className="text-slate-500">Số tiền:</span>
                          <div className="flex items-center gap-2">
                            <span className="font-black text-rose-600 text-sm">{formatVND(finalTotal)}</span>
                            <button
                              type="button"
                              onClick={() => handleCopy(String(finalTotal), 'amount_opt')}
                              className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                            >
                              {copiedField === 'amount_opt' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                              <span>{copiedField === 'amount_opt' ? 'Đã chép' : 'Sao chép'}</span>
                            </button>
                          </div>
                        </div>
                        <div className="flex justify-between items-center py-1">
                          <span className="text-slate-500">Nội dung chuyển khoản:</span>
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-bold text-slate-900 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                              QS THANHTOAN
                            </span>
                            <button
                              type="button"
                              onClick={() => handleCopy('QS THANHTOAN', 'content_opt')}
                              className="text-[10px] text-blue-600 hover:text-blue-800 font-semibold px-2 py-0.5 bg-blue-50 rounded border border-blue-200 flex items-center gap-1 active:scale-95 transition"
                            >
                              {copiedField === 'content_opt' ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                              <span>{copiedField === 'content_opt' ? 'Đã chép' : 'Sao chép'}</span>
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Hướng dẫn hoàn tất đơn hàng */}
                    <div className="p-3 bg-gradient-to-r from-amber-50 to-orange-50 rounded-xl border border-amber-200/90 flex items-start gap-2.5 text-xs text-amber-950">
                      <Sparkles className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                      <div className="space-y-0.5">
                        <div className="font-bold text-amber-900">👉 Bước tiếp theo: Bấm nút "ĐẶT HÀNG & THANH TOÁN PAYOS" ở cuối trang</div>
                        <div className="text-[11px] text-amber-800 leading-relaxed">
                          Sau khi quét mã chuyển khoản, bạn chỉ cần bấm nút <strong>"ĐẶT HÀNG & THANH TOÁN PAYOS"</strong>. Hệ thống sẽ tự động đối soát số tiền và hiển thị ngay màn hình <strong>ĐÃ THANH TOÁN THÀNH CÔNG</strong>!
                        </div>
                      </div>
                    </div>

                    <div className="pt-1 flex flex-col sm:flex-row items-center justify-between gap-2 text-[11px] text-slate-600 bg-blue-50/70 p-2.5 rounded-xl border border-blue-100">
                      <div className="flex items-center gap-1.5">
                        <Sparkles className="w-4 h-4 text-amber-500 flex-shrink-0" />
                        <span>Hỗ trợ quét qua MB, Vietcombank, Techcombank, VPBank, PayOS, MoMo...</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={async () => {
                            const data = await handleCreatePayOsPayment('PREVIEW_' + Date.now(), finalTotal);
                            if (data?.checkoutUrl) window.open(data.checkoutUrl, '_blank');
                          }}
                          disabled={isGeneratingPayOs}
                          className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-lg shadow-sm transition active:scale-95 flex items-center gap-1.5 whitespace-nowrap text-[11px]"
                        >
                          {isGeneratingPayOs ? (
                            <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <ExternalLink className="w-3.5 h-3.5" />
                          )}
                          <span>Xem Trang Thanh Toán PayOS</span>
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Option 3: Credit Card */}
              <label
                onClick={() => setPaymentMethod('credit')}
                className={`p-3.5 rounded-2xl border transition cursor-pointer flex items-center justify-between ${
                  paymentMethod === 'credit'
                    ? 'border-orange-500 bg-orange-50/50 ring-2 ring-orange-500/20'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-xs">
                    <CreditCard className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs font-bold text-slate-900">Thẻ Quốc Tế (Visa, MasterCard, JCB)</div>
                    <div className="text-[11px] text-slate-500">Bảo mật chuẩn quốc tế PCI-DSS 256-bit</div>
                  </div>
                </div>
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === 'credit'}
                  onChange={() => setPaymentMethod('credit')}
                  className="accent-orange-600 w-4 h-4"
                />
              </label>
            </div>
          </div>

          {/* Section E: Order Note */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-2">
            <label className="block text-xs font-bold text-slate-800">
              Ghi chú cho Người bán & Đơn vị vận chuyển:
            </label>
            <input
              type="text"
              value={orderNote}
              onChange={(e) => setOrderNote(e.target.value)}
              placeholder="Ví dụ: Giao vào giờ hành chính, gọi điện trước khi đến 15 phút..."
              className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-orange-500"
            />
          </div>
        </div>

        {/* RIGHT COLUMN (1/3 width, Sticky Order Summary Box) */}
        <div className="space-y-6 lg:sticky lg:top-24">

          {/* Vouchers & Coupons Card (Chuẩn Shopee: Phân chia rõ ràng Voucher Sàn & Voucher Shop) */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-3.5">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <div className="flex items-center gap-2 text-xs font-black text-slate-900 uppercase tracking-wide">
                <Ticket className="w-4 h-4 text-orange-600" />
                <span>Mã Giảm Giá & Voucher</span>
              </div>
              <span className="text-[10px] font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded-full">
                Chuẩn Shopee
              </span>
            </div>

            {/* Shopee-style Clickable Voucher Row 1: Sàn & Freeship */}
            <div
              onClick={() => setIsVoucherModalOpen(true)}
              className="p-3 bg-gradient-to-r from-orange-50/70 to-rose-50/40 hover:from-orange-100/60 hover:to-rose-100/40 border border-orange-200 rounded-2xl cursor-pointer transition flex items-center justify-between gap-2 shadow-sm group"
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-7 h-7 rounded-xl bg-orange-600 text-white flex items-center justify-center flex-shrink-0">
                  <Tag className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <div className="text-xs font-black text-slate-900 flex items-center gap-1.5 flex-wrap">
                    <span>QuickShop Voucher:</span>
                    {appliedPlatformVoucher ? (
                      <span className="bg-orange-600 text-white text-[10px] font-black px-1.5 py-0.2 rounded shadow-sm">
                        -{formatVND(appliedPlatformVoucher.discount)}
                      </span>
                    ) : (
                      <span className="text-[11px] text-orange-600 font-semibold">Chưa chọn</span>
                    )}

                    {appliedShippingVoucher && (
                      <span className="bg-emerald-600 text-white text-[10px] font-black px-1.5 py-0.2 rounded shadow-sm">
                        🚚 Freeship -{formatVND(appliedShippingVoucher.discount)}
                      </span>
                    )}
                  </div>
                  <div className="text-[10px] text-slate-500 truncate mt-0.5">
                    {appliedPlatformVoucher?.title || 'Bấm để chọn mã giảm giá & freeship sàn'}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-1 text-xs font-bold text-orange-600 group-hover:translate-x-0.5 transition flex-shrink-0">
                <span className="text-[11px]">{appliedPlatformVoucher || appliedShippingVoucher ? 'Đổi mã' : 'Chọn mã'}</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </div>
            </div>

            {/* Shopee-style Clickable Voucher Row 2: Voucher Của Shop */}
            <div
              onClick={() => setIsVoucherModalOpen(true)}
              className="p-3 bg-gradient-to-r from-purple-50/70 to-indigo-50/40 hover:from-purple-100/60 hover:to-indigo-100/40 border border-purple-200 rounded-2xl cursor-pointer transition flex items-center justify-between gap-2 shadow-sm group"
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-7 h-7 rounded-xl bg-purple-600 text-white flex items-center justify-center flex-shrink-0">
                  <Store className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <div className="text-xs font-black text-slate-900 flex items-center gap-1.5 flex-wrap">
                    <span>Voucher Của Shop:</span>
                    {shopDiscount > 0 ? (
                      <span className="bg-purple-600 text-white text-[10px] font-black px-1.5 py-0.2 rounded shadow-sm">
                        -{formatVND(shopDiscount)}
                      </span>
                    ) : (
                      <span className="text-[11px] text-purple-600 font-semibold">Chưa chọn</span>
                    )}
                  </div>
                  <div className="text-[10px] text-slate-500 truncate mt-0.5">
                    {shopDiscount > 0
                      ? Object.values(appliedShopVouchers).map(v => v.shopName).join(', ')
                      : 'Bấm chọn voucher riêng của từng Shop trong đơn'}
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-1 text-xs font-bold text-purple-600 group-hover:translate-x-0.5 transition flex-shrink-0">
                <span className="text-[11px]">{shopDiscount > 0 ? 'Đổi mã' : 'Chọn mã'}</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </div>
            </div>

            {/* Big Shopee CTA Button to Open Voucher Modal */}
            <button
              type="button"
              onClick={() => setIsVoucherModalOpen(true)}
              className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-2 shadow-sm active:scale-95"
            >
              <Sparkles className="w-4 h-4 text-amber-300" />
              <span>Bấm Chọn Hoặc Nhập Mã Voucher (Sàn & Shop)</span>
            </button>

            {/* Quick manual voucher input */}
            <div className="pt-2 border-t border-slate-100 space-y-2">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={voucherCode}
                  onChange={(e) => setVoucherCode(e.target.value)}
                  placeholder="Hoặc nhập mã trực tiếp..."
                  className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs uppercase font-mono font-bold focus:outline-none focus:border-orange-500"
                />
                <button
                  type="button"
                  onClick={() => handleQuickApplyCode()}
                  className="px-3.5 py-2 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl transition active:scale-95"
                >
                  Nhập
                </button>
              </div>

              {voucherApplied && (
                <div className={`text-[11px] ${voucherApplied.startsWith('✓') ? 'text-emerald-600 font-bold' : 'text-rose-500 font-medium'}`}>
                  {voucherApplied}
                </div>
              )}
            </div>
          </div>

          {/* Checkout Breakdown Box */}
          <div className="bg-white rounded-3xl border border-slate-200/90 p-5 sm:p-6 shadow-sm space-y-4">
            <h3 className="text-sm font-black text-slate-900 uppercase tracking-wide pb-2 border-b border-slate-100">
              Chi Tiết Thanh Toán
            </h3>

            <div className="space-y-2.5 text-xs text-slate-600">
              <div className="flex justify-between">
                <span>Tạm tính hàng hóa:</span>
                <span className="font-bold text-slate-900">{formatVND(subTotal)}</span>
              </div>

              <div className="flex justify-between">
                <span>Phí vận chuyển:</span>
                <span className="font-semibold text-slate-900">
                  {actualShippingFee === 0 ? (
                    <span className="text-emerald-600 font-bold">Miễn phí (Freeship)</span>
                  ) : (
                    formatVND(actualShippingFee)
                  )}
                </span>
              </div>

              {shippingDiscount > 0 && (
                <div className="flex justify-between text-emerald-600 font-bold">
                  <span>Giảm giá Vận chuyển (Freeship):</span>
                  <span>-{formatVND(shippingDiscount)}</span>
                </div>
              )}

              {platformDiscount > 0 && (
                <div className="flex justify-between text-orange-600 font-bold">
                  <span>Giảm giá Voucher Sàn:</span>
                  <span>-{formatVND(platformDiscount)}</span>
                </div>
              )}

              {shopDiscount > 0 && (
                <div className="flex justify-between text-purple-600 font-bold">
                  <span>Giảm giá Voucher Shop:</span>
                  <span>-{formatVND(shopDiscount)}</span>
                </div>
              )}

              <div className="pt-3 border-t border-slate-200 flex justify-between items-baseline">
                <span className="text-sm font-bold text-slate-900">Tổng cộng thanh toán:</span>
                <span className="text-xl font-black text-rose-600">{formatVND(finalTotal)}</span>
              </div>

              {(shippingDiscount + platformDiscount + shopDiscount) > 0 && (
                <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-[11px] font-bold text-center flex items-center justify-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Tiết kiệm được {formatVND(shippingDiscount + platformDiscount + shopDiscount)} cho đơn này!</span>
                </div>
              )}

              <div className="text-[10px] text-slate-400 text-right">
                (Đã bao gồm thuế GTGT nếu có)
              </div>
            </div>

            {/* Place Order CTA Button */}
            <button
              onClick={handlePlaceOrder}
              disabled={isSubmitting}
              className={`w-full py-4 text-white font-black text-sm rounded-2xl transition shadow-xl active:scale-95 flex items-center justify-center gap-2 disabled:opacity-50 ${
                (paymentMethod === 'payos' || paymentMethod === 'vnpay')
                  ? 'bg-gradient-to-r from-blue-600 via-indigo-600 to-blue-700 hover:from-blue-700 hover:to-indigo-700 shadow-blue-500/30'
                  : 'bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 shadow-orange-500/30'
              }`}
            >
              {isSubmitting ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Đang xử lý đơn hàng...</span>
                </>
              ) : (
                <>
                  {(paymentMethod === 'payos' || paymentMethod === 'vnpay') ? (
                    <>
                      <QrCode className="w-4 h-4" />
                      <span>Đặt Hàng & Thanh Toán PayOS (MB Bank)</span>
                    </>
                  ) : (
                    <>
                      <span>Tiến hành Đặt Hàng Ngay</span>
                      <ChevronRight className="w-4 h-4" />
                    </>
                  )}
                </>
              )}
            </button>

            {/* Security Guarantee */}
            <div className="pt-2 border-t border-slate-100 space-y-2 text-[10px] text-slate-500">
              <div className="flex items-center gap-2 text-emerald-600 font-bold">
                <ShieldCheck className="w-4 h-4 flex-shrink-0" />
                <span>Bảo mật thanh toán chuẩn quốc tế PCI-DSS & Escrow</span>
              </div>
              <div className="flex items-center gap-2">
                <Sparkles className="w-3.5 h-3.5 text-amber-500 flex-shrink-0" />
                <span>Cam kết 100% hàng chính hãng, đổi trả miễn phí 7 ngày</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Voucher Selection Modal (Chuẩn Shopee) */}
      <VoucherModal
        isOpen={isVoucherModalOpen}
        onClose={() => setIsVoucherModalOpen(false)}
        subTotal={subTotal}
        shopsInOrder={shopGroups.map(g => ({ shopId: g.shopId, shopName: g.shopName }))}
        appliedVouchers={{
          shippingVoucher: appliedShippingVoucher,
          platformVoucher: appliedPlatformVoucher,
          shopVouchers: appliedShopVouchers,
        }}
        onApplyVouchers={handleApplyVouchers}
      />
    </div>
  );
}

