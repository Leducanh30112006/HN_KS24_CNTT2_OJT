import React, { useState, useEffect } from 'react';
import { 
  Package, Search, Truck, CheckCircle2, Clock, XCircle, RotateCcw, 
  ChevronRight, Phone, MapPin, Eye, ArrowLeft, AlertCircle, 
  Sparkles, ExternalLink, RefreshCw, ShoppingCart, ShieldCheck
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { 
  getStoredOrders, 
  getStoredShipments, 
  cancelOrderById, 
  simulateNextCarrierStepInStore 
} from '../data/sampleOrders';
import axiosClient from '../api/axiosClient';

export default function OrderHistoryPage({ onBackToHome, onNavigateToProduct, onBuyAgain }) {
  const { user } = useAuthStore();
  const { addToCart } = useCartStore();

  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL' | 'PENDING' | 'PROCESSING' | 'DELIVERING' | 'COMPLETED' | 'CANCELLED'
  const [searchQuery, setSearchQuery] = useState('');
  const [orders, setOrders] = useState([]);
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(false);

  // Modal tracking
  const [selectedShipment, setSelectedShipment] = useState(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [trackingModalOpen, setTrackingModalOpen] = useState(false);

  // Cancel order modal
  const [orderToCancel, setOrderToCancel] = useState(null);
  const [cancelReason, setCancelReason] = useState('Đổi ý muốn mua sản phẩm khác');

  // Load orders and shipments
  const loadData = async () => {
    setLoading(true);
    const localOrders = getStoredOrders();
    const localShipments = getStoredShipments();

    // Thử lấy đơn hàng từ backend nếu có kết nối
    try {
      const res = await axiosClient.get('/api/orders', {
        params: { userId: user?.userId || 2 },
      });
      if (res?.data && Array.isArray(res.data) && res.data.length > 0) {
        // Tích hợp dữ liệu backend với format hiển thị
        setOrders(res.data);
      } else {
        setOrders(localOrders);
      }
    } catch {
      setOrders(localOrders);
    }

    setShipments(localShipments);
    setLoading(false);
  };

  useEffect(() => {
    loadData();
  }, [user]);

  // Lọc đơn hàng theo tab và từ khóa
  const filteredOrders = orders.filter((order) => {
    // Lọc theo tab trạng thái
    if (activeTab === 'PENDING' && order.status !== 'PENDING') return false;
    if (activeTab === 'PROCESSING' && order.status !== 'PROCESSING' && order.status !== 'PAID') return false;
    if (activeTab === 'DELIVERING' && order.status !== 'DELIVERING') return false;
    if (activeTab === 'COMPLETED' && order.status !== 'COMPLETED') return false;
    if (activeTab === 'CANCELLED' && order.status !== 'CANCELLED') return false;

    // Lọc theo từ khóa tìm kiếm
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      const codeMatch = (order.orderCode || '').toLowerCase().includes(q);
      const productMatch = (order.subOrders || []).some((sub) =>
        (sub.items || []).some((it) => (it.productName || '').toLowerCase().includes(q))
      );
      return codeMatch || productMatch;
    }

    return true;
  });

  // Mở modal theo dõi vận chuyển cho đơn hàng
  const handleOpenTracking = (subOrder, order) => {
    const trackingCode = subOrder.trackingCode;
    const currentShipments = getStoredShipments();
    let found = currentShipments.find((s) => s.trackingCode === trackingCode || String(s.subOrderId) === String(subOrder.id));

    if (!found) {
      // Tạo shipment giả lập tức thì nếu chưa có
      found = {
        id: Date.now(),
        trackingCode: trackingCode || `GHN-VNM-${Math.floor(100000 + Math.random() * 900000)}`,
        carrierName: subOrder.carrierName || 'Giao Hàng Nhanh (GHN)',
        shippingStatus: subOrder.status === 'COMPLETED' ? 'DELIVERED' : subOrder.status === 'DELIVERING' ? 'DELIVERING' : 'PICKED',
        recipientName: order.recipientName || 'Khách Hàng',
        recipientPhone: order.recipientPhone || '0988 123 456',
        toAddress: order.deliveryAddress,
        shipperName: 'Nguyễn Văn Hùng',
        shipperPhone: '0979 123 456',
        shipperVehicle: '29-D1 567.89 (Honda Wave)',
        codAmount: order.paymentStatus === 'PAID' ? 0 : order.finalAmount,
        timeline: [
          { step: 1, title: 'Đơn hàng đã đặt', description: 'Đã xác nhận đơn hàng', location: 'QuickShop', time: order.createdAt || 'Hôm nay', completed: true },
          { step: 2, title: 'Đã xuất kho', description: 'Bàn giao cho bưu cục', location: 'Kho người bán', time: 'Hôm nay', completed: true },
          { step: 3, title: 'Đang vận chuyển', description: 'Shipper đang trên tuyến giao', location: 'Cầu Giấy, Hà Nội', time: 'Hiện tại', completed: true, current: true },
        ]
      };
    }

    setSelectedShipment(found);
    setTrackingModalOpen(true);
  };

  // Mô phỏng bước tiếp theo của bưu tá vận chuyển
  const handleSimulateNextStep = async () => {
    if (!selectedShipment) return;
    setIsSimulating(true);

    try {
      // Cố gắng gọi API backend nếu có
      await axiosClient.post(`/api/shipping/simulate-step/${selectedShipment.trackingCode}`);
    } catch {
      // Fallback cục bộ
    }

    simulateNextCarrierStepInStore(selectedShipment.trackingCode);
    const updatedShipments = getStoredShipments();
    const updated = updatedShipments.find((s) => s.trackingCode === selectedShipment.trackingCode);
    if (updated) {
      setSelectedShipment(updated);
    }

    // Làm mới danh sách đơn hàng
    setOrders(getStoredOrders());
    setIsSimulating(false);
  };

  // Hủy đơn hàng
  const handleConfirmCancelOrder = () => {
    if (!orderToCancel) return;
    cancelOrderById(orderToCancel.id, cancelReason);
    setOrders(getStoredOrders());
    setOrderToCancel(null);
  };

  // Mua lại sản phẩm
  const handleBuyAgain = async (item) => {
    await addToCart(
      { id: item.productId, name: item.productName, price: item.price, mainImage: item.imageUrl },
      { variantId: item.variantId, variantName: item.variantName, color: item.color, size: item.size, price: item.price },
      1
    );
    if (onBuyAgain) {
      onBuyAgain();
    } else {
      alert(`Đã thêm lại sản phẩm "${item.productName}" vào giỏ hàng!`);
    }
  };

  const formatVND = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-full text-[11px] font-bold">Chờ xác nhận</span>;
      case 'PROCESSING':
      case 'PAID':
        return <span className="px-2.5 py-1 bg-blue-100 text-blue-800 rounded-full text-[11px] font-bold">Đang chuẩn bị hàng</span>;
      case 'DELIVERING':
        return <span className="px-2.5 py-1 bg-orange-100 text-orange-800 rounded-full text-[11px] font-bold animate-pulse">Đang giao hàng</span>;
      case 'COMPLETED':
        return <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-full text-[11px] font-bold">Giao thành công</span>;
      case 'CANCELLED':
        return <span className="px-2.5 py-1 bg-rose-100 text-rose-800 rounded-full text-[11px] font-bold">Đã hủy đơn</span>;
      default:
        return <span className="px-2.5 py-1 bg-slate-100 text-slate-700 rounded-full text-[11px] font-medium">{status}</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        {/* Navigation Top Bar */}
        <div className="flex items-center justify-between">
          <button
            onClick={onBackToHome}
            className="inline-flex items-center gap-2 text-xs font-semibold text-slate-600 hover:text-orange-600 transition group"
          >
            <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
            <span>Tiếp tục mua sắm</span>
          </button>
          <div className="text-xs text-slate-400">
            Tra cứu đơn hàng trực tiếp với tiến trình vận chuyển bưu tá
          </div>
        </div>

        {/* Page Header */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center shadow-sm">
              <Package className="w-6 h-6 stroke-[2.5]" />
            </div>
            <div>
              <h1 className="text-xl font-black text-slate-900 tracking-tight">Đơn Mua Của Bạn</h1>
              <p className="text-xs text-slate-500">Quản lý các đơn hàng đã đặt, theo dõi lộ trình vận chuyển và mua lại</p>
            </div>
          </div>

          <div className="relative w-full sm:w-72">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo Mã đơn hoặc Tên sản phẩm..."
              className="w-full pl-9 pr-4 py-2 bg-slate-100 focus:bg-white text-xs rounded-xl border border-transparent focus:border-orange-500 outline-none transition"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          </div>
        </div>

        {/* Status Filter Tabs (Shopee Style) */}
        <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-x-auto">
          <div className="flex border-b border-slate-100 min-w-max">
            {[
              { id: 'ALL', label: 'Tất cả đơn' },
              { id: 'PENDING', label: 'Chờ xác nhận' },
              { id: 'PROCESSING', label: 'Đang xử lý / Đã thanh toán' },
              { id: 'DELIVERING', label: 'Đang giao hàng' },
              { id: 'COMPLETED', label: 'Đã giao' },
              { id: 'CANCELLED', label: 'Đã hủy' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-5 py-3.5 text-xs font-bold transition border-b-2 whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'border-orange-600 text-orange-600 bg-orange-50/30'
                    : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Orders List */}
        {filteredOrders.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-3">
            <div className="w-16 h-16 rounded-full bg-slate-100 text-slate-400 mx-auto flex items-center justify-center">
              <Package className="w-8 h-8" />
            </div>
            <h3 className="font-bold text-slate-800 text-sm">Chưa có đơn hàng nào ở trạng thái này</h3>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              Hãy tiếp tục khám phá hàng ngàn deal giảm giá và voucher miễn phí vận chuyển trên sàn QuickShop!
            </p>
            <button
              onClick={onBackToHome}
              className="px-6 py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold text-xs rounded-xl shadow-md transition active:scale-95"
            >
              Mua sắm ngay
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {filteredOrders.map((order) => (
              <div
                key={order.id}
                className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden hover:shadow-md transition"
              >
                {/* Header card: Mã đơn & Trạng thái */}
                <div className="p-4 sm:p-5 bg-slate-50/70 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="font-mono font-black text-xs sm:text-sm text-slate-900">
                      #{order.orderCode || order.id}
                    </span>
                    <span className="text-slate-300">|</span>
                    <span className="text-[11px] text-slate-500">
                      {new Date(order.createdAt).toLocaleString('vi-VN')}
                    </span>
                    <span className="text-slate-300">|</span>
                    <span className="text-[11px] font-semibold text-slate-700">
                      PTTT: {order.paymentMethod} ({order.paymentStatus})
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {getStatusBadge(order.status)}
                  </div>
                </div>

                {/* SubOrders & Items */}
                <div className="divide-y divide-slate-100">
                  {(order.subOrders || []).map((subOrder) => (
                    <div key={subOrder.id} className="p-4 sm:p-5 space-y-4">
                      {/* Shop line */}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <img
                            src={subOrder.shopAvatar || 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?w=50&auto=format&fit=crop&q=80'}
                            alt=""
                            className="w-6 h-6 rounded-lg object-cover"
                          />
                          <span className="font-bold text-xs text-slate-900">{subOrder.shopName}</span>
                          <span className="text-[10px] bg-orange-100 text-orange-700 px-1.5 py-0.2 rounded font-semibold">
                            Mall
                          </span>
                        </div>

                        {subOrder.trackingCode && (
                          <div className="flex items-center gap-2 text-xs">
                            <span className="text-slate-400 text-[11px]">Vận đơn:</span>
                            <span className="font-mono font-bold text-blue-600">{subOrder.trackingCode}</span>
                          </div>
                        )}
                      </div>

                      {/* Items table */}
                      <div className="space-y-3">
                        {(subOrder.items || []).map((item) => (
                          <div key={item.id} className="flex items-center gap-3.5">
                            <img
                              src={item.imageUrl}
                              alt={item.productName}
                              className="w-16 h-16 rounded-xl object-cover border border-slate-200 flex-shrink-0"
                            />
                            <div className="flex-1 min-w-0">
                              <h4 className="text-xs font-semibold text-slate-900 line-clamp-1">{item.productName}</h4>
                              <div className="text-[11px] text-slate-500 pt-0.5">
                                Phân loại: {item.variantName || `${item.color} - ${item.size}`}
                              </div>
                              <div className="text-[11px] text-slate-400">Số lượng: x{item.quantity}</div>
                            </div>
                            <div className="text-right flex-shrink-0">
                              <div className="text-xs font-bold text-slate-900">{formatVND(item.price)}</div>
                              <button
                                onClick={() => handleBuyAgain(item)}
                                className="mt-1 text-[11px] text-orange-600 hover:underline font-semibold flex items-center gap-1 ml-auto"
                              >
                                <ShoppingCart className="w-3 h-3" />
                                <span>Mua lại</span>
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>

                      {/* Action buttons per sub-order */}
                      <div className="pt-2 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100">
                        <div className="text-xs text-slate-500 flex items-center gap-1.5">
                          <Truck className="w-4 h-4 text-orange-600" />
                          <span>{subOrder.carrierName || 'Giao Hàng Nhanh (GHN)'}</span>
                        </div>

                        <div className="flex items-center gap-2">
                          {subOrder.trackingCode && (
                            <button
                              onClick={() => handleOpenTracking(subOrder, order)}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-orange-50 hover:bg-orange-100 text-orange-600 rounded-xl text-xs font-bold transition shadow-sm"
                            >
                              <Truck className="w-3.5 h-3.5" />
                              <span>Theo dõi vận chuyển</span>
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Footer card: Summary & Main Actions */}
                <div className="p-4 sm:p-5 bg-slate-50/50 border-t border-slate-200/80 flex flex-col sm:flex-row items-center justify-between gap-4">
                  <div className="text-xs text-slate-500 text-center sm:text-left">
                    <div>
                      Địa chỉ nhận: <strong className="text-slate-800">{order.deliveryAddress}</strong>
                    </div>
                    {order.cancelReason && (
                      <div className="text-rose-600 font-semibold pt-0.5">
                        Lý do hủy: {order.cancelReason}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="text-right">
                      <span className="text-[11px] text-slate-400">Thành tiền:</span>
                      <div className="text-base sm:text-lg font-black text-orange-600">
                        {formatVND(order.finalAmount || order.totalAmount)}
                      </div>
                    </div>

                    {order.status === 'PENDING' && (
                      <button
                        onClick={() => setOrderToCancel(order)}
                        className="px-4 py-2 border border-rose-300 text-rose-600 hover:bg-rose-50 rounded-xl text-xs font-bold transition"
                      >
                        Hủy đơn hàng
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* MODAL 1: Theo dõi đơn hàng & Hành trình Shipper trực quan */}
      {trackingModalOpen && selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
                  <Truck className="w-5 h-5 stroke-[2.5]" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-slate-900">Chi Tiết Vận Chuyển</h3>
                  <p className="text-[11px] font-mono font-bold text-orange-600">
                    {selectedShipment.trackingCode} • {selectedShipment.carrierName}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setTrackingModalOpen(false)}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition"
              >
                ✕
              </button>
            </div>

            {/* Shipper info card */}
            <div className="bg-gradient-to-r from-orange-50 to-amber-50 p-4 rounded-2xl border border-orange-200/80 space-y-2">
              <div className="flex items-center justify-between">
                <div className="font-bold text-xs text-slate-900 flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Bưu tá phụ trách: {selectedShipment.shipperName || 'Bưu tá GHN Express'}</span>
                </div>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-white text-orange-700 shadow-sm">
                  {selectedShipment.shippingStatus}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-600 pt-1">
                <div>
                  SĐT Shipper:{' '}
                  <a href={`tel:${selectedShipment.shipperPhone}`} className="font-bold text-blue-600 hover:underline">
                    {selectedShipment.shipperPhone || '0988 123 456'}
                  </a>
                </div>
                <div>Phương tiện: <span className="font-mono">{selectedShipment.shipperVehicle || 'Xe máy'}</span></div>
                <div className="col-span-2">
                  Tiền COD cần thu:{' '}
                  <strong className="text-rose-600 font-bold">
                    {selectedShipment.codAmount > 0 ? formatVND(selectedShipment.codAmount) : '0₫ (Đã thanh toán online)'}
                  </strong>
                </div>
              </div>
            </div>

            {/* Timeline Stepper */}
            <div className="space-y-4 max-h-72 overflow-y-auto px-1">
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Hành trình kiện hàng</h4>
              <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                {(selectedShipment.timeline || []).map((tl, idx) => (
                  <div key={idx} className="relative">
                    <div
                      className={`absolute -left-6 top-0.5 w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] font-bold ${
                        tl.completed
                          ? tl.current
                            ? 'bg-orange-500 border-orange-200 text-white animate-pulse'
                            : 'bg-emerald-500 border-emerald-200 text-white'
                          : 'bg-white border-slate-300 text-slate-400'
                      }`}
                    >
                      {tl.completed ? '✓' : idx + 1}
                    </div>
                    <div>
                      <div className="text-xs font-bold text-slate-900">{tl.title}</div>
                      <div className="text-[11px] text-slate-600 pt-0.5">{tl.description}</div>
                      <div className="text-[10px] text-slate-400 flex items-center gap-2 pt-0.5">
                        <span>{tl.time}</span>
                        {tl.location && <span>• {tl.location}</span>}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Action simulation footer */}
            <div className="pt-2 border-t border-slate-100 flex items-center justify-between gap-3">
              <button
                type="button"
                onClick={handleSimulateNextStep}
                disabled={isSimulating || selectedShipment.shippingStatus === 'DELIVERED'}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 text-white text-xs font-bold rounded-xl shadow-md transition active:scale-95 disabled:opacity-50"
                title="Mô phỏng bưu tá thực hiện bước giao hàng kế tiếp"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${isSimulating ? 'animate-spin' : ''}`} />
                <span>{isSimulating ? 'Đang mô phỏng...' : 'Mô phỏng bước giao kế tiếp'}</span>
              </button>

              <button
                type="button"
                onClick={() => setTrackingModalOpen(false)}
                className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: Xác nhận hủy đơn */}
      {orderToCancel && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
              <AlertCircle className="w-6 h-6 stroke-[2.5]" />
            </div>

            <div className="text-center space-y-1">
              <h3 className="font-bold text-sm text-slate-900">Xác Nhận Hủy Đơn Hàng?</h3>
              <p className="text-xs text-slate-500">
                Đơn hàng #{orderToCancel.orderCode} sẽ bị hủy bỏ và không thể khôi phục lại.
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Lý do hủy đơn:</label>
              <select
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none"
              >
                <option value="Đổi ý muốn mua sản phẩm khác">Đổi ý muốn mua sản phẩm khác</option>
                <option value="Tìm thấy giá rẻ hơn ở nơi khác">Tìm thấy giá rẻ hơn ở nơi khác</option>
                <option value="Thời gian giao hàng dự kiến quá lâu">Thời gian giao hàng dự kiến quá lâu</option>
                <option value="Cần thay đổi địa chỉ nhận hàng">Cần thay đổi địa chỉ nhận hàng</option>
                <option value="Khác">Lý do khác</option>
              </select>
            </div>

            <div className="pt-2 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setOrderToCancel(null)}
                className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition"
              >
                Không, giữ lại
              </button>
              <button
                type="button"
                onClick={handleConfirmCancelOrder}
                className="px-5 py-2.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-md transition active:scale-95"
              >
                Xác nhận hủy đơn
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
