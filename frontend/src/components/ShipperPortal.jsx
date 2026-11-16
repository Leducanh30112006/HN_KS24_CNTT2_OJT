import React, { useState, useEffect } from 'react';
import { 
  Truck, Package, CheckCircle2, Clock, XCircle, Phone, 
  MapPin, Search, Filter, AlertTriangle, ArrowLeft, RefreshCw, 
  DollarSign, ShieldCheck, ChevronRight, Navigation, Bike, Check, Eye,
  LogOut, ShoppingBag, Menu, X, Wallet, User, ChevronDown
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

  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [shipments, setShipments] = useState([]);
  const [activeFilter, setActiveFilter] = useState('ALL'); // 'ALL' | 'READY_TO_PICK' | 'DELIVERING' | 'DELIVERED' | 'DELIVERY_FAILED' | 'COD_REPORT'
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
  const countFailed = shipments.filter((s) => s.shippingStatus === 'DELIVERY_FAILED').length;
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

  const navItems = [
    { id: 'ALL', label: 'Tất cả vận đơn', icon: Truck, count: shipments.length, color: 'text-blue-400' },
    { id: 'READY_TO_PICK', label: 'Cần lấy tại kho', icon: Package, count: countReady, badgeBg: 'bg-amber-500/20 text-amber-300 border-amber-500/30' },
    { id: 'DELIVERING', label: 'Đang giao hàng', icon: Bike, count: countDelivering, badgeBg: 'bg-blue-500/20 text-blue-300 border-blue-500/30' },
    { id: 'DELIVERED', label: 'Giao thành công', icon: CheckCircle2, count: countDelivered, badgeBg: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' },
    { id: 'DELIVERY_FAILED', label: 'Giao thất bại', icon: AlertTriangle, count: countFailed, badgeBg: 'bg-rose-500/20 text-rose-300 border-rose-500/30' },
    { id: 'COD_REPORT', label: 'Báo cáo tiền COD', icon: Wallet, count: null },
  ];

  return (
    <div className="min-h-screen bg-slate-100 flex font-sans">
      {/* ========================================================== */}
      {/* 1. PROFESSIONAL SIDEBAR NAVIGATION (THANH BÊN ĐIỀU HƯỚNG) */}
      {/* ========================================================== */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 bg-slate-900 text-slate-300 flex flex-col transition-transform duration-300 ease-in-out md:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        } shadow-2xl md:shadow-none border-r border-slate-800`}
      >
        {/* Sidebar Header / Brand */}
        <div className="h-16 flex items-center justify-between px-5 border-b border-slate-800 bg-slate-950/50">
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center shadow-lg shadow-blue-600/30 font-bold">
              <Truck className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="text-sm font-black tracking-tight text-white flex items-center gap-1.5">
                <span>QUICKSHOP</span>
                <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  SHIPPER
                </span>
              </div>
              <p className="text-[10px] text-slate-400">Logistics & Giao Vận</p>
            </div>
          </div>

          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Shipper Profile Quick Card */}
        <div className="p-4 mx-3 my-3 bg-slate-800/60 rounded-2xl border border-slate-800/80">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 text-blue-400 flex items-center justify-center font-bold text-sm">
              <Bike className="w-5 h-5 text-blue-400" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-xs font-bold text-white truncate">{user?.fullName || 'Trần Văn Thành'}</div>
              <div className="text-[10px] text-blue-400 font-medium">Bưu tá: #SP-892</div>
              <div className="text-[9px] text-emerald-400 font-semibold flex items-center gap-1 mt-0.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                <span>Ca ngày • Xe: 29-B1 688.92</span>
              </div>
            </div>
          </div>
        </div>

        {/* Sidebar Nav Items */}
        <div className="px-3 py-2 flex-1 space-y-1 overflow-y-auto">
          <div className="px-3 pb-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            Quản Lý Vận Chuyển
          </div>

          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeFilter === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveFilter(item.id);
                  setSidebarOpen(false);
                }}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold transition ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-600/20 font-bold'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/70'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                  <span>{item.label}</span>
                </div>
                {item.count !== null && (
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                      isActive
                        ? 'bg-white/20 text-white border-white/25'
                        : item.badgeBg || 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}
                  >
                    {item.count}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        {/* Sidebar Footer Actions */}
        <div className="p-3 border-t border-slate-800 space-y-2 bg-slate-950/40">
          {onBackToHome && (
            <button
              onClick={onBackToHome}
              className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
            >
              <ShoppingBag className="w-3.5 h-3.5 text-orange-400" />
              <span>Xem Sàn Mua Sắm</span>
            </button>
          )}

          <div className="flex items-center justify-between pt-1 px-1">
            <div className="flex items-center space-x-2">
              <div className="w-7 h-7 rounded-lg bg-blue-500/20 text-blue-400 font-bold flex items-center justify-center text-xs border border-blue-500/30">
                {user?.fullName?.charAt(0) || 'S'}
              </div>
              <div className="text-left">
                <div className="text-xs font-bold text-white truncate max-w-[100px]">{user?.fullName || 'Shipper'}</div>
                <div className="text-[9px] text-blue-400 font-medium">ROLE_SHIPPER</div>
              </div>
            </div>

            <button
              onClick={handleLogout}
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </aside>

      {/* Backdrop for mobile */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 bg-slate-950/70 backdrop-blur-xs z-40 md:hidden"
        />
      )}

      {/* ========================================================== */}
      {/* 2. MAIN CONTENT AREA (BÊN PHẢI THANH SIDEBAR) */}
      {/* ========================================================== */}
      <div className="flex-1 flex flex-col md:pl-64 min-w-0">
        {/* Top Navbar */}
        <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200 h-16 flex items-center justify-between px-4 sm:px-6 shadow-xs">
          <div className="flex items-center space-x-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-2 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-slate-900 flex items-center gap-2">
                <span>Kênh Giao Vận & Điều Phối Bưu Tá</span>
                <span className="hidden sm:inline-block text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                  {activeFilter === 'ALL' && 'Tất Cả Vận Đơn'}
                  {activeFilter === 'READY_TO_PICK' && 'Chờ Lấy Hàng Tại Kho'}
                  {activeFilter === 'DELIVERING' && 'Đang Trên Đường Giao'}
                  {activeFilter === 'DELIVERED' && 'Giao Thành Công'}
                  {activeFilter === 'DELIVERY_FAILED' && 'Giao Thất Bại'}
                  {activeFilter === 'COD_REPORT' && 'Báo Cáo Tiền COD'}
                </span>
              </h2>
              <p className="text-[11px] text-slate-500 hidden sm:block">
                Tuyến: Cầu Giấy - Ba Đình, Hà Nội • Ca làm việc: Ca ngày (08:00 - 18:00)
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={loadShipments}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
              title="Làm mới danh sách"
            >
              <RefreshCw className={`w-3.5 h-3.5 text-blue-600 ${loading ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Làm mới</span>
            </button>
          </div>
        </header>

        {/* Main Body */}
        <main className="p-4 sm:p-6 lg:p-8 space-y-6 flex-1 max-w-7xl w-full mx-auto">
          {/* Banner Card */}
          <div className="bg-gradient-to-r from-blue-700 via-indigo-700 to-sky-700 rounded-3xl p-6 sm:p-7 text-white shadow-xl shadow-blue-700/15">
            <div className="flex flex-col md:flex-row items-center justify-between gap-5">
              <div className="flex items-center gap-4 text-center sm:text-left">
                <div className="w-14 h-14 rounded-2xl bg-white/20 backdrop-blur-md border border-white/30 flex items-center justify-center text-white shadow-inner flex-shrink-0">
                  <Bike className="w-7 h-7" />
                </div>
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-start">
                    <h1 className="text-lg sm:text-xl font-black tracking-tight">Kênh Quản Lý Vận Chuyển (Shipper)</h1>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-400 text-slate-900 shadow-sm">
                      ĐANG HOẠT ĐỘNG
                    </span>
                  </div>
                  <p className="text-xs text-blue-100">
                    Bưu tá: <strong className="text-white">{user?.fullName || 'Trần Văn Thành'} (ID: #SP-892)</strong> • Tuyến: Cầu Giấy - Ba Đình
                  </p>
                  <div className="text-[11px] text-blue-200">
                    Phương tiện: 29-B1 688.92 (Xe máy giao hàng) • Ca làm việc: Ca ngày (08:00 - 18:00)
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={loadShipments}
                  disabled={loading}
                  className="inline-flex items-center gap-2 px-3.5 py-2 bg-white/20 hover:bg-white/30 text-white rounded-xl text-xs font-bold backdrop-blur-md border border-white/25 transition active:scale-95"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
                  <span>Đồng bộ đơn</span>
                </button>
              </div>
            </div>
          </div>

          {/* Action toast */}
          {actionSuccessMsg && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in duration-150 shadow-sm">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              <span>{actionSuccessMsg}</span>
            </div>
          )}

          {/* KPI Metrics Dashboard */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div 
              onClick={() => setActiveFilter('READY_TO_PICK')}
              className={`p-5 rounded-2xl border cursor-pointer transition ${
                activeFilter === 'READY_TO_PICK' 
                  ? 'bg-amber-50 border-amber-300 ring-2 ring-amber-400/30' 
                  : 'bg-white border-slate-200 hover:border-amber-300'
              } shadow-xs space-y-1.5`}
            >
              <div className="flex items-center justify-between text-slate-500">
                <span className="text-xs font-bold uppercase tracking-wider">Cần lấy tại kho</span>
                <Package className="w-4 h-4 text-amber-500" />
              </div>
              <div className="text-2xl font-black text-amber-600">{countReady}</div>
              <div className="text-[11px] text-slate-400">Đơn hàng sẵn sàng lấy từ shop</div>
            </div>

            <div 
              onClick={() => setActiveFilter('DELIVERING')}
              className={`p-5 rounded-2xl border cursor-pointer transition ${
                activeFilter === 'DELIVERING' 
                  ? 'bg-blue-50 border-blue-300 ring-2 ring-blue-400/30' 
                  : 'bg-white border-slate-200 hover:border-blue-300'
              } shadow-xs space-y-1.5`}
            >
              <div className="flex items-center justify-between text-slate-500">
                <span className="text-xs font-bold uppercase tracking-wider">Đang trên đường giao</span>
                <Truck className="w-4 h-4 text-blue-500" />
              </div>
              <div className="text-2xl font-black text-blue-600">{countDelivering}</div>
              <div className="text-[11px] text-slate-400">Đang giao đến địa chỉ khách</div>
            </div>

            <div 
              onClick={() => setActiveFilter('DELIVERED')}
              className={`p-5 rounded-2xl border cursor-pointer transition ${
                activeFilter === 'DELIVERED' 
                  ? 'bg-emerald-50 border-emerald-300 ring-2 ring-emerald-400/30' 
                  : 'bg-white border-slate-200 hover:border-emerald-300'
              } shadow-xs space-y-1.5`}
            >
              <div className="flex items-center justify-between text-slate-500">
                <span className="text-xs font-bold uppercase tracking-wider">Giao thành công</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              </div>
              <div className="text-2xl font-black text-emerald-600">{countDelivered}</div>
              <div className="text-[11px] text-slate-400">Đã bàn giao cho người nhận</div>
            </div>

            <div 
              onClick={() => setActiveFilter('COD_REPORT')}
              className={`p-5 rounded-2xl border cursor-pointer transition ${
                activeFilter === 'COD_REPORT' 
                  ? 'bg-rose-50 border-rose-300 ring-2 ring-rose-400/30' 
                  : 'bg-white border-slate-200 hover:border-rose-300'
              } shadow-xs space-y-1.5`}
            >
              <div className="flex items-center justify-between text-slate-500">
                <span className="text-xs font-bold uppercase tracking-wider">Tiền COD đã thu</span>
                <DollarSign className="w-4 h-4 text-rose-500" />
              </div>
              <div className="text-xl sm:text-2xl font-black text-rose-600">{formatVND(totalCodCollected)}</div>
              <div className="text-[11px] text-slate-400">Cần nộp về kế toán bưu cục cuối ngày</div>
            </div>
          </div>

          {/* COD REPORT VIEW */}
          {activeFilter === 'COD_REPORT' && (
            <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
                <div>
                  <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                    <Wallet className="w-5 h-5 text-rose-500" />
                    <span>Báo Cáo Thu Hộ COD & Đối Soát Bưu Cục</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Tổng hợp tất cả các khoản thu hộ tiền mặt từ người nhận trong ca làm việc
                  </p>
                </div>
                <div className="text-right">
                  <div className="text-xs text-slate-500 font-semibold">Tổng quỹ cần nộp:</div>
                  <div className="text-2xl font-black text-rose-600">{formatVND(totalCodCollected)}</div>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-200 text-slate-400 uppercase text-[10px] tracking-wider bg-slate-50">
                      <th className="py-3 px-4 font-bold">Mã vận đơn</th>
                      <th className="py-3 px-4 font-bold">Khách hàng</th>
                      <th className="py-3 px-4 font-bold">Số điện thoại</th>
                      <th className="py-3 px-4 font-bold">Địa chỉ giao</th>
                      <th className="py-3 px-4 font-bold text-right">Tiền COD</th>
                      <th className="py-3 px-4 font-bold text-center">Trạng thái thu</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {shipments
                      .filter((s) => s.shippingStatus === 'DELIVERED')
                      .map((s) => (
                        <tr key={s.id} className="hover:bg-slate-50 transition">
                          <td className="py-3 px-4 font-mono font-bold text-blue-600">{s.trackingCode}</td>
                          <td className="py-3 px-4 font-semibold text-slate-800">{s.recipientName}</td>
                          <td className="py-3 px-4 text-slate-600">{s.recipientPhone}</td>
                          <td className="py-3 px-4 text-slate-500 max-w-xs truncate">{s.toAddress}</td>
                          <td className="py-3 px-4 text-right font-black text-rose-600">{formatVND(s.codAmount)}</td>
                          <td className="py-3 px-4 text-center">
                            <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                              Đã thu tiền mặt
                            </span>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* SHIPMENTS LIST VIEW */}
          {activeFilter !== 'COD_REPORT' && (
            <>
              {/* Controls: Search */}
              <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
                  <Filter className="w-4 h-4 text-slate-400" />
                  <span>
                    Đang hiển thị: <strong className="text-blue-600">{filteredShipments.length}</strong> kiện hàng
                  </span>
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
            </>
          )}
        </main>

        {/* Footer */}
        <footer className="py-4 border-t border-slate-200 bg-white text-center text-xs text-slate-400">
          QuickShop Logistics • Hệ Thống Điều Phối & Giao Nhận Bưu Tá Toàn Quốc
        </footer>
      </div>

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

            <div data-lenis-prevent className="space-y-4 max-h-80 overflow-y-auto px-1">
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
    </div>
  );
}
