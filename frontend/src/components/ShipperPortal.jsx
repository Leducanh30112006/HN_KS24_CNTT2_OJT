import React, { useState, useEffect } from 'react';
import { 
  Truck, Package, CheckCircle2, Clock, XCircle, Phone, 
  MapPin, Search, Filter, AlertTriangle, ArrowLeft, RefreshCw, 
  DollarSign, ShieldCheck, ChevronRight, Navigation, Bike, Check, Eye,
  LogOut, ShoppingBag
} from 'lucide-react';
import { 
  getStoredShipments, 
  updateShipmentStatusInStore, 
  simulateNextCarrierStepInStore 
} from '../data/sampleOrders';
import axiosClient from '../api/axiosClient';
import { useAuthStore } from '../stores/useAuthStore';

export default function ShipperPortal({ onBackToHome }) {
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    if (onBackToHome) {
      onBackToHome();
    }
  };

  const [shipments, setShipments] = useState([]);
  const [activeFilter, setActiveFilter] = useState('ALL'); // 'ALL' | 'READY_TO_PICK' | 'DELIVERING' | 'DELIVERED' | 'DELIVERY_FAILED'
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [actionSuccessMsg, setActionSuccessMsg] = useState(null);

  // Failure modal
  const [failureTarget, setFailureTarget] = useState(null);
  const [failureReason, setFailureReason] = useState('Khách hàng không nghe máy (Gọi 3 cuộc)');

  // Selected for details modal
  const [selectedShipment, setSelectedShipment] = useState(null);

  const loadShipments = async () => {
    setLoading(true);
    const local = getStoredShipments();

    try {
      // Gọi thử backend GET /api/shipping nếu có
      const res = await axiosClient.get('/api/shipping');
      if (res?.data && Array.isArray(res.data) && res.data.length > 0) {
        setShipments(res.data);
      } else {
        setShipments(local);
      }
    } catch {
      setShipments(local);
    }
    setLoading(false);
  };

  useEffect(() => {
    loadShipments();
  }, []);

  // Filter shipments
  const filteredShipments = shipments.filter((item) => {
    if (activeFilter === 'READY_TO_PICK' && item.shippingStatus !== 'READY_TO_PICK') return false;
    if (activeFilter === 'DELIVERING' && item.shippingStatus !== 'DELIVERING' && item.shippingStatus !== 'PICKED') return false;
    if (activeFilter === 'DELIVERED' && item.shippingStatus !== 'DELIVERED') return false;
    if (activeFilter === 'DELIVERY_FAILED' && item.shippingStatus !== 'DELIVERY_FAILED') return false;

    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      const codeMatch = (item.trackingCode || '').toLowerCase().includes(q);
      const nameMatch = (item.recipientName || '').toLowerCase().includes(q);
      const phoneMatch = (item.recipientPhone || '').toLowerCase().includes(q);
      const addrMatch = (item.toAddress || '').toLowerCase().includes(q);
      return codeMatch || nameMatch || phoneMatch || addrMatch;
    }

    return true;
  });

  // KPI Calculations
  const countReady = shipments.filter((s) => s.shippingStatus === 'READY_TO_PICK').length;
  const countDelivering = shipments.filter((s) => s.shippingStatus === 'DELIVERING' || s.shippingStatus === 'PICKED').length;
  const countDelivered = shipments.filter((s) => s.shippingStatus === 'DELIVERED').length;
  const totalCodCollected = shipments
    .filter((s) => s.shippingStatus === 'DELIVERED')
    .reduce((sum, s) => sum + (s.codAmount || 0), 0);

  // Status transitions
  const handleUpdateStatus = async (shipmentId, newStatus, reason = '') => {
    try {
      await axiosClient.put(`/api/shipping/${shipmentId}/status`, {
        status: newStatus,
        reason: reason || undefined,
      });
    } catch {
      // Backend offline -> handled locally
    }

    const updated = updateShipmentStatusInStore(shipmentId, newStatus, reason);
    setShipments(updated);

    let msg = `Đã cập nhật vận đơn sang trạng thái: ${newStatus}`;
    if (newStatus === 'PICKED') msg = 'Đã nhận kiện hàng từ Shop thành công!';
    if (newStatus === 'DELIVERING') msg = 'Đã bắt đầu lộ trình giao hàng đến người nhận!';
    if (newStatus === 'DELIVERED') msg = 'Xác nhận giao hàng thành công & Ghi nhận thu COD!';
    if (newStatus === 'DELIVERY_FAILED') msg = 'Đã ghi nhận giao hàng thất bại!';

    setActionSuccessMsg(msg);
    setTimeout(() => setActionSuccessMsg(null), 3000);
  };

  // Simulate next carrier step
  const handleSimulateStep = async (trackingCode) => {
    try {
      await axiosClient.post(`/api/shipping/simulate-step/${trackingCode}`);
    } catch {}

    simulateNextCarrierStepInStore(trackingCode);
    setShipments(getStoredShipments());
    setActionSuccessMsg(`Đã mô phỏng bước kế tiếp cho vận đơn ${trackingCode}!`);
    setTimeout(() => setActionSuccessMsg(null), 3000);
  };

  const handleConfirmFailure = () => {
    if (!failureTarget) return;
    handleUpdateStatus(failureTarget.id, 'DELIVERY_FAILED', failureReason);
    setFailureTarget(null);
  };

  const formatVND = (val) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* 1. Standalone Dedicated Shipper Top Navbar */}
      <header className="sticky top-0 z-40 bg-slate-900 text-white shadow-md border-b border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center shadow-md font-bold">
              <Truck className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-base tracking-tight text-white">QUICKSHOP</span>
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  Logistics Dispatch
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block">Kênh Giao Vận & Điều Phối Bưu Tá Siêu Tốc</p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {onBackToHome && (
              <button
                onClick={onBackToHome}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition"
              >
                <ShoppingBag className="w-3.5 h-3.5 text-orange-400" />
                <span>Xem Sàn Mua Sắm</span>
              </button>
            )}

            <div className="h-6 w-px bg-slate-700 hidden sm:block" />

            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-400 font-bold flex items-center justify-center text-xs border border-blue-500/30">
                {user?.fullName?.charAt(0) || 'T'}
              </div>
              <div className="text-left hidden md:block">
                <div className="text-xs font-bold text-white truncate max-w-[130px]">{user?.fullName || 'Tài Xế Giao Hàng'}</div>
                <div className="text-[10px] text-blue-400 font-semibold">{user?.role || 'ROLE_SHIPPER'}</div>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full flex-1 space-y-6">
        {/* Shipper Portal Banner Card */}
        <div className="bg-gradient-to-r from-blue-700 via-indigo-700 to-sky-700 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-blue-700/15">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-4 text-center sm:text-left">
              <div className="w-16 h-16 rounded-2xl bg-white/20 backdrop-blur-md border border-white/30 flex items-center justify-center text-white shadow-inner">
                <Bike className="w-8 h-8" />
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-start">
                  <h1 className="text-xl sm:text-2xl font-black tracking-tight">Kênh Quản Lý Vận Chuyển (Shipper)</h1>
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-400 text-slate-900 shadow-sm">
                    ĐANG HOẠT ĐỘNG
                  </span>
                </div>
                <p className="text-xs text-blue-100">
                  Bưu tá: <strong className="text-white">{user?.fullName || 'Trần Văn Thành'} (ID: #SP-892)</strong> • Tuyến: Cầu Giấy - Ba Đình, Hà Nội
                </p>
                <div className="text-[11px] text-blue-200">
                  Phương tiện: 29-B1 688.92 (Xe máy giao hàng) • Ca làm việc: Ca ngày (08:00 - 18:00)
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={loadShipments}
                disabled={loading}
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-white/20 hover:bg-white/30 text-white rounded-xl text-xs font-bold backdrop-blur-md border border-white/25 transition active:scale-95"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                <span>Làm mới danh sách</span>
              </button>
            </div>
          </div>
        </div>

        {/* Action toast */}
        {actionSuccessMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in duration-150">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
            <span>{actionSuccessMsg}</span>
          </div>
        )}

        {/* KPI Metrics Dashboard */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-2">
            <div className="flex items-center justify-between text-slate-500">
              <span className="text-xs font-bold uppercase tracking-wider">Cần lấy tại kho</span>
              <Package className="w-4 h-4 text-amber-500" />
            </div>
            <div className="text-2xl font-black text-amber-600">{countReady}</div>
            <div className="text-[11px] text-slate-400">Đơn hàng sẵn sàng lấy từ shop</div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-2">
            <div className="flex items-center justify-between text-slate-500">
              <span className="text-xs font-bold uppercase tracking-wider">Đang trên đường giao</span>
              <Truck className="w-4 h-4 text-blue-500" />
            </div>
            <div className="text-2xl font-black text-blue-600">{countDelivering}</div>
            <div className="text-[11px] text-slate-400">Đang giao đến địa chỉ khách</div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-2">
            <div className="flex items-center justify-between text-slate-500">
              <span className="text-xs font-bold uppercase tracking-wider">Giao thành công</span>
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
            </div>
            <div className="text-2xl font-black text-emerald-600">{countDelivered}</div>
            <div className="text-[11px] text-slate-400">Đã bàn giao cho người nhận</div>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-2">
            <div className="flex items-center justify-between text-slate-500">
              <span className="text-xs font-bold uppercase tracking-wider">Tiền COD đã thu</span>
              <DollarSign className="w-4 h-4 text-rose-500" />
            </div>
            <div className="text-2xl font-black text-rose-600">{formatVND(totalCodCollected)}</div>
            <div className="text-[11px] text-slate-400">Cần nộp về kế toán bưu cục cuối ngày</div>
          </div>
        </div>

        {/* Controls: Filter Tabs & Search */}
        <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex flex-wrap gap-1.5 w-full md:w-auto">
            {[
              { id: 'ALL', label: `Tất cả (${shipments.length})` },
              { id: 'READY_TO_PICK', label: `Chờ lấy (${countReady})` },
              { id: 'DELIVERING', label: `Đang giao (${countDelivering})` },
              { id: 'DELIVERED', label: `Đã giao (${countDelivered})` },
              { id: 'DELIVERY_FAILED', label: `Giao thất bại` },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveFilter(tab.id)}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold transition ${
                  activeFilter === tab.id
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="relative w-full md:w-80">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Mã vận đơn, Tên, SĐT, Địa chỉ..."
              className="w-full pl-9 pr-4 py-2 bg-slate-100 focus:bg-white text-xs rounded-xl border border-transparent focus:border-blue-500 outline-none transition"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          </div>
        </div>

        {/* Shipments List Table / Cards */}
        <div className="space-y-4">
          {filteredShipments.length === 0 ? (
            <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-2">
              <Truck className="w-12 h-12 text-slate-300 mx-auto" />
              <h3 className="font-bold text-slate-700 text-sm">Không tìm thấy kiện hàng nào</h3>
              <p className="text-xs text-slate-400">Không có vận đơn nào khớp với tiêu chí lọc của bạn.</p>
            </div>
          ) : (
            filteredShipments.map((s) => (
              <div
                key={s.id}
                className="bg-white rounded-3xl border border-slate-200 shadow-sm p-5 space-y-4 hover:border-blue-300 transition"
              >
                {/* Header row: Tracking code, Carrier, Status, COD amount */}
                <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-slate-100">
                  <div className="flex items-center gap-2.5">
                    <span className="font-mono font-black text-sm text-blue-700">{s.trackingCode}</span>
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                      {s.carrierName}
                    </span>
                    <span className="text-slate-300">|</span>
                    <span className="text-[11px] text-slate-500">Khối lượng: {s.weightGrams || 500}g</span>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <span className="text-[10px] text-slate-400 uppercase font-semibold">Thu tiền COD:</span>
                      <div className="font-black text-xs sm:text-sm text-rose-600">
                        {s.codAmount > 0 ? formatVND(s.codAmount) : '0₫ (Đã trả trước)'}
                      </div>
                    </div>

                    <span
                      className={`px-3 py-1 rounded-full text-[11px] font-bold ${
                        s.shippingStatus === 'READY_TO_PICK'
                          ? 'bg-amber-100 text-amber-800'
                          : s.shippingStatus === 'PICKED'
                          ? 'bg-indigo-100 text-indigo-800'
                          : s.shippingStatus === 'DELIVERING'
                          ? 'bg-blue-100 text-blue-800 animate-pulse'
                          : s.shippingStatus === 'DELIVERED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-rose-100 text-rose-800'
                      }`}
                    >
                      {s.shippingStatus === 'READY_TO_PICK' && 'Chờ lấy hàng'}
                      {s.shippingStatus === 'PICKED' && 'Đã lấy kiện'}
                      {s.shippingStatus === 'DELIVERING' && 'Đang giao'}
                      {s.shippingStatus === 'DELIVERED' && 'Giao thành công'}
                      {s.shippingStatus === 'DELIVERY_FAILED' && 'Giao thất bại'}
                    </span>
                  </div>
                </div>

                {/* Body row: Addresses & Contacts */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                  {/* From Shop Address */}
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200/80 space-y-1">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-amber-500" />
                      <span>Địa điểm lấy hàng (Kho Shop)</span>
                    </div>
                    <div className="font-semibold text-slate-800">{s.fromAddress}</div>
                  </div>

                  {/* To Customer Address */}
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200/80 space-y-1">
                    <div className="text-[10px] font-bold text-slate-400 uppercase flex items-center gap-1">
                      <Navigation className="w-3.5 h-3.5 text-blue-500" />
                      <span>Địa chỉ người nhận (Khách hàng)</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="font-bold text-slate-900">{s.recipientName}</div>
                      <a
                        href={`tel:${s.recipientPhone}`}
                        className="inline-flex items-center gap-1 px-2.5 py-1 bg-emerald-100 text-emerald-800 rounded-lg text-xs font-bold hover:bg-emerald-200 transition"
                      >
                        <Phone className="w-3.5 h-3.5" />
                        <span>{s.recipientPhone}</span>
                      </a>
                    </div>
                    <div className="text-slate-600">{s.toAddress}</div>
                  </div>
                </div>

                {s.failureReason && (
                  <div className="p-3 bg-rose-50 text-rose-800 text-xs font-semibold rounded-xl flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 flex-shrink-0" />
                    <span>Lý do không giao được: {s.failureReason}</span>
                  </div>
                )}

                {/* Footer row: Workflow Action buttons */}
                <div className="pt-2 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setSelectedShipment(s)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      <span>Xem lịch trình ({s.timeline?.length || 0} bước)</span>
                    </button>

                    <button
                      onClick={() => handleSimulateStep(s.trackingCode)}
                      className="inline-flex items-center gap-1 px-3 py-1.5 text-blue-600 hover:bg-blue-50 rounded-xl text-xs font-semibold transition"
                      title="Mô phỏng tự động chuyển sang bước tiếp theo"
                    >
                      <RefreshCw className="w-3.5 h-3.5" />
                      <span>Mô phỏng bước tiếp theo</span>
                    </button>
                  </div>

                  {/* Conditional Status Buttons */}
                  <div className="flex items-center gap-2 flex-wrap">
                    {s.shippingStatus === 'READY_TO_PICK' && (
                      <button
                        onClick={() => handleUpdateStatus(s.id, 'PICKED')}
                        className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-md transition active:scale-95 flex items-center gap-1.5"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Đã lấy hàng từ Shop</span>
                      </button>
                    )}

                    {(s.shippingStatus === 'PICKED' || s.shippingStatus === 'DELIVERY_FAILED') && (
                      <button
                        onClick={() => handleUpdateStatus(s.id, 'DELIVERING')}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold shadow-md transition active:scale-95 flex items-center gap-1.5"
                      >
                        <Bike className="w-3.5 h-3.5" />
                        <span>Bắt đầu đi giao hàng</span>
                      </button>
                    )}

                    {s.shippingStatus === 'DELIVERING' && (
                      <>
                        <button
                          onClick={() => setFailureTarget(s)}
                          className="px-3.5 py-2 border border-rose-300 text-rose-600 hover:bg-rose-50 rounded-xl text-xs font-bold transition flex items-center gap-1"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          <span>Báo giao thất bại</span>
                        </button>

                        <button
                          onClick={() => handleUpdateStatus(s.id, 'DELIVERED')}
                          className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold shadow-md transition active:scale-95 flex items-center gap-1.5"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Đã giao & Thu COD {s.codAmount > 0 ? `(${formatVND(s.codAmount)})` : ''}</span>
                        </button>
                      </>
                    )}

                    {s.shippingStatus === 'DELIVERED' && (
                      <span className="inline-flex items-center gap-1 text-emerald-600 text-xs font-bold px-3 py-1.5 bg-emerald-50 rounded-xl">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Kiện hàng đã hoàn tất</span>
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </main>

      {/* MODAL 1: Báo giao hàng thất bại */}
      {failureTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6 stroke-[2.5]" />
            </div>

            <div className="text-center space-y-1">
              <h3 className="font-bold text-sm text-slate-900">Báo Cáo Giao Hàng Không Thành Công</h3>
              <p className="text-xs text-slate-500">
                Vận đơn #{failureTarget.trackingCode} - Người nhận: {failureTarget.recipientName}
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Lý do không giao được:</label>
              <select
                value={failureReason}
                onChange={(e) => setFailureReason(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 outline-none"
              >
                <option value="Khách hàng không nghe máy (Đã gọi 3 cuộc)">Khách hàng không nghe máy (Đã gọi 3 cuộc)</option>
                <option value="Khách hàng hẹn giao lại vào ca làm việc sau">Khách hàng hẹn giao lại vào ca làm việc sau</option>
                <option value="Địa chỉ người nhận không chính xác / Không tìm thấy nhà">Địa chỉ người nhận không chính xác / Không tìm thấy nhà</option>
                <option value="Khách hàng không mang đủ tiền mặt để thanh toán COD">Khách hàng không mang đủ tiền mặt để thanh toán COD</option>
                <option value="Khách hàng từ chối nhận do không đặt đơn">Khách hàng từ chối nhận do không đặt đơn</option>
              </select>
            </div>

            <div className="pt-2 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setFailureTarget(null)}
                className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirmFailure}
                className="px-5 py-2.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-md transition active:scale-95"
              >
                Xác nhận báo thất bại
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: Xem chi tiết vận đơn & Timeline */}
      {selectedShipment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="font-bold text-sm text-slate-900">Chi Tiết Lộ Trình Vận Đơn</h3>
                <p className="text-xs font-mono font-bold text-blue-600">{selectedShipment.trackingCode}</p>
              </div>
              <button
                onClick={() => setSelectedShipment(null)}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 max-h-80 overflow-y-auto px-1">
              <div className="relative pl-6 space-y-5 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
                {(selectedShipment.timeline || []).map((tl, idx) => (
                  <div key={idx} className="relative">
                    <div
                      className={`absolute -left-6 top-0.5 w-5 h-5 rounded-full border-2 flex items-center justify-center text-[10px] font-bold ${
                        tl.completed ? 'bg-blue-600 border-blue-200 text-white' : 'bg-white border-slate-300 text-slate-400'
                      }`}
                    >
                      {tl.completed ? '✓' : idx + 1}
                    </div>
                    <div>
                      <div className="text-xs font-bold text-slate-900">{tl.title}</div>
                      <div className="text-[11px] text-slate-600 pt-0.5">{tl.description}</div>
                      <div className="text-[10px] text-slate-400 pt-0.5">
                        {tl.time} • {tl.location}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-2 border-t border-slate-100 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedShipment(null)}
                className="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Standalone Shipper Footer */}
      <footer className="py-4 border-t border-slate-200 bg-white text-center text-xs text-slate-400">
        QuickShop Logistics • Hệ Thống Điều Phối & Giao Nhận Bưu Tá Toàn Quốc
      </footer>
    </div>
  );
}
