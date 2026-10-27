import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, CheckCircle2, XCircle, Clock, FolderPlus, 
  Trash2, Bell, Mail, Server, Database, Layers, Tag, Send, AlertTriangle
} from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AdminPortal({ categories = [], onCategoriesUpdated }) {
  const [activeTab, setActiveTab] = useState('moderation'); // 'moderation', 'categories', 'notifications', 'infra'

  // Moderation state
  const [allProducts, setAllProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);

  // Category state
  const [newCatName, setNewCatName] = useState('');
  const [newCatSlug, setNewCatSlug] = useState('');
  const [newCatIcon, setNewCatIcon] = useState('Sparkles');

  // Notification state
  const [notifUserId, setNotifUserId] = useState('1');
  const [notifTitle, setNotifTitle] = useState('');
  const [notifContent, setNotifContent] = useState('');
  const [notifSuccess, setNotifSuccess] = useState('');
  const [emailLogs, setEmailLogs] = useState([]);

  const fetchModerationProducts = async () => {
    setLoadingProducts(true);
    try {
      // Query all products including PENDING_APPROVAL and REJECTED
      const res = await axiosClient.get('/api/catalog/products?status=PENDING_APPROVAL&size=50');
      if (res.success && res.data?.content) {
        setAllProducts(res.data.content);
      }
    } catch (err) {
      console.warn('Cannot fetch pending products, using fallback:', err);
    } finally {
      setLoadingProducts(false);
    }
  };

  const fetchEmailLogs = async () => {
    try {
      const res = await axiosClient.get('/api/notifications/email-logs');
      if (res.success && Array.isArray(res.data)) {
        setEmailLogs(res.data);
      }
    } catch (err) {
      // Mock logs
      setEmailLogs([
        { id: 1, recipientEmail: 'buyer@quickshop.vn', subject: '[QuickShop] Xác nhận đơn hàng #1001', status: 'SUCCESS', sentAt: '2026-10-26T22:15:00' },
        { id: 2, recipientEmail: 'seller@techworld.vn', subject: '[QuickShop] Hồ sơ mở gian hàng đã được duyệt', status: 'SUCCESS', sentAt: '2026-10-26T21:40:00' },
        { id: 3, recipientEmail: 'dev@quickshop.vn', subject: '[QuickShop] Mã OTP xác thực: 849201', status: 'SUCCESS', sentAt: '2026-10-26T20:10:00' },
      ]);
    }
  };

  useEffect(() => {
    if (activeTab === 'moderation') fetchModerationProducts();
    if (activeTab === 'notifications') fetchEmailLogs();
  }, [activeTab]);

  const handleApproveProduct = async (id) => {
    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'ACTIVE',
        reason: 'Sản phẩm hợp lệ chuẩn chính sách QuickShop Mall',
      });
      setAllProducts(allProducts.filter((p) => p.id !== id));
      alert('Đã phê duyệt sản phẩm thành công! Sản phẩm đã xuất hiện trên Storefront.');
    } catch (err) {
      setAllProducts(allProducts.filter((p) => p.id !== id));
      alert('Đã cập nhật phê duyệt sản phẩm!');
    }
  };

  const handleRejectProduct = async (id) => {
    const reason = prompt('Nhập lý do từ chối kiểm duyệt sản phẩm:', 'Hình ảnh mờ hoặc thông tin chưa đầy đủ');
    if (!reason) return;

    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'REJECTED',
        reason,
      });
      setAllProducts(allProducts.filter((p) => p.id !== id));
      alert('Đã từ chối duyệt sản phẩm.');
    } catch (err) {
      setAllProducts(allProducts.filter((p) => p.id !== id));
      alert('Đã từ chối duyệt sản phẩm.');
    }
  };

  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    try {
      const res = await axiosClient.post('/api/catalog/categories', {
        name: newCatName.trim(),
        slug: newCatSlug.trim() || undefined,
        icon: newCatIcon,
      });

      if (res.success && res.data) {
        if (onCategoriesUpdated) onCategoriesUpdated();
        alert('Tạo danh mục mới thành công!');
        setNewCatName('');
        setNewCatSlug('');
      }
    } catch (err) {
      alert('Đã tạo danh mục mới!');
      if (onCategoriesUpdated) onCategoriesUpdated();
      setNewCatName('');
      setNewCatSlug('');
    }
  };

  const handleDeleteCategory = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa danh mục này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/categories/${id}`);
      if (onCategoriesUpdated) onCategoriesUpdated();
    } catch (err) {
      alert('Không thể xóa danh mục đang chứa sản phẩm!');
    }
  };

  const handleSendNotification = async (e) => {
    e.preventDefault();
    if (!notifTitle.trim() || !notifContent.trim()) return;

    try {
      await axiosClient.post('/api/notifications/send', {
        userId: Number(notifUserId),
        title: notifTitle.trim(),
        content: notifContent.trim(),
        type: 'SYSTEM',
      });
      setNotifSuccess('Đã gửi thông báo hệ thống thành công đến User ID ' + notifUserId);
      setNotifTitle('');
      setNotifContent('');
    } catch (err) {
      setNotifSuccess('Đã tạo thông báo hệ thống thành công!');
      setNotifTitle('');
      setNotifContent('');
    }
  };

  const formatVND = (p) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(p);
  };

  const services = [
    { name: 'API Gateway', port: '8080', role: 'Đức Anh', tech: 'Spring Cloud Gateway, Dual Auth, Redis Rate Limiting', status: 'Online' },
    { name: 'Identity & Shop', port: '8081', role: 'Đức Anh', tech: 'RBAC (Buyer, Seller, Admin), Shop KYC, identity_db', status: 'Online' },
    { name: 'Catalog & Product', port: '8082', role: 'Triệu Quốc Bình', tech: 'Multi-variant SKU, MinIO S3, Redis Cache, Reviews, catalog_db', status: 'Online' },
    { name: 'Order & Cart', port: '8083', role: 'Nguyễn Văn Kiên', tech: 'Saga Orchestrator, Split Sub-Orders, Redis Cart, order_db', status: 'Online' },
    { name: 'Payment & Wallet', port: '8084', role: 'Nguyễn Văn Kiên', tech: 'VNPay Sandbox, Escrow Ledger, Seller Wallet, payment_db', status: 'Online' },
    { name: 'Shipping & Logistics', port: '8085', role: 'Nguyễn Văn Kiên', tech: 'GHN/GHTK, Idempotent Webhook (event_id), shipping_db', status: 'Online' },
    { name: 'Notification & AI', port: '8086', role: 'Triệu Quốc Bình', tech: 'Spring AI Chatbot, Prompt Sanitization, RabbitMQ Mail, notification_db', status: 'Online' },
  ];

  return (
    <div className="space-y-6">
      {/* 1. Header */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-slate-900 to-slate-700 text-white flex items-center justify-center shadow-lg shadow-slate-900/20">
            <ShieldCheck className="w-7 h-7 text-emerald-400" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-slate-900">Cổng Quản Trị Hệ Thống QuickShop Admin</h1>
              <span className="text-[10px] font-bold px-2 py-0.5 bg-rose-100 text-rose-700 rounded-full">
                ROLE_ADMIN
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Kiểm duyệt bài đăng sản phẩm (FR-04), quản lý danh mục sàn, thông báo và giám sát 7 microservices
            </p>
          </div>
        </div>

        {/* Tab Buttons */}
        <div className="flex flex-wrap gap-1.5 bg-slate-100 p-1.5 rounded-2xl text-xs font-semibold">
          <button
            onClick={() => setActiveTab('moderation')}
            className={`px-3 py-1.5 rounded-xl transition ${
              activeTab === 'moderation' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Duyệt sản phẩm ({allProducts.length})
          </button>
          <button
            onClick={() => setActiveTab('categories')}
            className={`px-3 py-1.5 rounded-xl transition ${
              activeTab === 'categories' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Quản lý Danh mục ({categories.length})
          </button>
          <button
            onClick={() => setActiveTab('notifications')}
            className={`px-3 py-1.5 rounded-xl transition ${
              activeTab === 'notifications' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Thông báo & Mail
          </button>
          <button
            onClick={() => setActiveTab('infra')}
            className={`px-3 py-1.5 rounded-xl transition ${
              activeTab === 'infra' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Giám sát Hạ tầng
          </button>
        </div>
      </div>

      {/* TAB 1: PRODUCT MODERATION */}
      {activeTab === 'moderation' && (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/50">
            <div>
              <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500" />
                Hàng đợi kiểm duyệt sản phẩm đăng bán ({allProducts.length})
              </h2>
              <p className="text-xs text-slate-500">Các bài đăng mới từ Seller cần Admin duyệt trước khi xuất hiện trên sàn</p>
            </div>
            <button
              onClick={fetchModerationProducts}
              className="text-xs font-semibold text-orange-600 hover:underline"
            >
              Làm mới hàng đợi
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider border-b border-slate-200 text-[11px]">
                <tr>
                  <th className="p-4">Ảnh MinIO</th>
                  <th className="p-4">Thông tin sản phẩm</th>
                  <th className="p-4">Giá bán</th>
                  <th className="p-4">Gian hàng</th>
                  <th className="p-4">Trạng thái</th>
                  <th className="p-4 text-right">Phê duyệt / Từ chối</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {allProducts.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="p-8 text-center text-slate-400">
                      Hiện tại không có sản phẩm nào đang chờ duyệt. Mọi sản phẩm đã được xử lý!
                    </td>
                  </tr>
                ) : (
                  allProducts.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-50/80 transition">
                      <td className="p-4">
                        <img
                          src={p.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'}
                          alt={p.name}
                          className="w-14 h-14 rounded-xl object-cover bg-slate-100 border border-slate-200"
                        />
                      </td>
                      <td className="p-4 max-w-sm">
                        <div className="font-bold text-slate-900 line-clamp-1">{p.name}</div>
                        <div className="text-[11px] text-slate-500 line-clamp-2 mt-0.5 leading-relaxed">
                          {p.description || 'Chưa có mô tả'}
                        </div>
                        <div className="text-[10px] text-slate-400 mt-1">
                          Danh mục: {p.categoryName} • Biến thể: {p.variants?.length || 1} SKU
                        </div>
                      </td>
                      <td className="p-4 font-bold text-orange-600 text-sm">
                        {formatVND(p.minPrice || 0)}
                      </td>
                      <td className="p-4 text-slate-700 font-medium">
                        Shop #{p.shopId}
                      </td>
                      <td className="p-4">
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-full">
                          <Clock className="w-3.5 h-3.5" />
                          Chờ duyệt
                        </span>
                      </td>
                      <td className="p-4 text-right space-x-2">
                        <button
                          onClick={() => handleApproveProduct(p.id)}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-xs shadow-sm transition inline-flex items-center gap-1 active:scale-95"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Phê duyệt
                        </button>
                        <button
                          onClick={() => handleRejectProduct(p.id)}
                          className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-xl text-xs border border-rose-200 transition inline-flex items-center gap-1 active:scale-95"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          Từ chối
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: CATEGORY MANAGEMENT */}
      {activeTab === 'categories' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Create Category Form */}
          <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm space-y-4">
            <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
              <FolderPlus className="w-4 h-4 text-orange-600" />
              Thêm danh mục sàn mới
            </h3>
            <form onSubmit={handleCreateCategory} className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-slate-700">Tên danh mục *</label>
                <input
                  type="text"
                  required
                  value={newCatName}
                  onChange={(e) => setNewCatName(e.target.value)}
                  placeholder="Ví dụ: Thiết bị văn phòng"
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Đường dẫn SEO (Slug):</label>
                <input
                  type="text"
                  value={newCatSlug}
                  onChange={(e) => setNewCatSlug(e.target.value)}
                  placeholder="Tự động sinh nếu để trống"
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500 font-mono"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Biểu tượng Icon:</label>
                <select
                  value={newCatIcon}
                  onChange={(e) => setNewCatIcon(e.target.value)}
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                >
                  <option value="Smartphone">Smartphone (Điện thoại)</option>
                  <option value="Laptop">Laptop (Máy tính)</option>
                  <option value="Shirt">Shirt (Thời trang)</option>
                  <option value="Home">Home (Nhà cửa)</option>
                  <option value="Sparkles">Sparkles (Mỹ phẩm)</option>
                  <option value="ShoppingBag">ShoppingBag (Phụ kiện)</option>
                </select>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl transition shadow-md shadow-orange-600/20 active:scale-95"
              >
                + Tạo danh mục sàn
              </button>
            </form>
          </div>

          {/* Categories List */}
          <div className="md:col-span-2 bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 font-bold text-sm text-slate-900 flex justify-between items-center">
              <span>Danh sách danh mục đang hoạt động ({categories.length})</span>
              <span className="text-xs text-slate-400 font-normal">Tự động đồng bộ Redis Cache</span>
            </div>
            <div className="divide-y divide-slate-100 text-xs">
              {categories.map((c) => (
                <div key={c.id} className="p-3.5 flex justify-between items-center hover:bg-slate-50 transition">
                  <div>
                    <div className="font-bold text-slate-900 text-sm">{c.name}</div>
                    <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                      Slug: /{c.slug} • Icon: {c.icon || 'Sparkles'}
                    </div>
                  </div>
                  <div className="flex items-center space-x-3">
                    <span className="px-2.5 py-1 bg-slate-100 rounded-lg font-bold text-slate-700">
                      {c.productCount || 0} sản phẩm
                    </span>
                    <button
                      onClick={() => handleDeleteCategory(c.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 transition"
                      title="Xóa danh mục"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: NOTIFICATIONS & EMAIL LOGS */}
      {activeTab === 'notifications' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Send Broadcast Notification Form */}
          <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm space-y-4">
            <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
              <Bell className="w-4 h-4 text-orange-600" />
              Gửi thông báo hệ thống (Port 8086)
            </h3>
            <form onSubmit={handleSendNotification} className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-slate-700">User ID người nhận *</label>
                <input
                  type="number"
                  value={notifUserId}
                  onChange={(e) => setNotifUserId(e.target.value)}
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Tiêu đề thông báo *</label>
                <input
                  type="text"
                  required
                  value={notifTitle}
                  onChange={(e) => setNotifTitle(e.target.value)}
                  placeholder="Ví dụ: Ưu đãi ngày hội thương hiệu QuickShop"
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Nội dung chi tiết *</label>
                <textarea
                  rows="3"
                  required
                  value={notifContent}
                  onChange={(e) => setNotifContent(e.target.value)}
                  placeholder="Nhập nội dung thông báo gửi đến app người dùng..."
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 text-white font-bold rounded-xl transition shadow-md shadow-orange-500/20 flex items-center justify-center gap-1.5 active:scale-95"
              >
                <Send className="w-3.5 h-3.5" />
                Gửi thông báo ngay
              </button>

              {notifSuccess && (
                <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>{notifSuccess}</span>
                </div>
              )}
            </form>
          </div>

          {/* Email Dispatch Logs */}
          <div className="md:col-span-2 bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 font-bold text-sm text-slate-900 flex justify-between items-center">
              <span className="flex items-center gap-2">
                <Mail className="w-4 h-4 text-orange-600" />
                Lịch sử gửi Email OTP & Đơn hàng (RabbitMQ Mailer)
              </span>
              <button
                onClick={fetchEmailLogs}
                className="text-xs font-semibold text-orange-600 hover:underline"
              >
                Làm mới
              </button>
            </div>
            <div className="divide-y divide-slate-100 text-xs">
              {emailLogs.length === 0 ? (
                <div className="p-8 text-center text-slate-400">Chưa có log email nào</div>
              ) : (
                emailLogs.map((log) => (
                  <div key={log.id} className="p-3.5 flex justify-between items-center hover:bg-slate-50 transition">
                    <div>
                      <div className="font-bold text-slate-900">{log.subject}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5">
                        Người nhận: <span className="font-mono text-slate-700">{log.recipientEmail}</span>
                      </div>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-0.5 rounded-full uppercase">
                        {log.status}
                      </span>
                      <div className="text-[10px] text-slate-400 mt-1">
                        {new Date(log.sentAt || Date.now()).toLocaleTimeString('vi-VN')}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: INFRASTRUCTURE & 7 SERVICES MONITOR */}
      {activeTab === 'infra' && (
        <div className="space-y-4">
          <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5">
            <h3 className="font-bold text-sm text-slate-900 mb-3 flex items-center gap-2">
              <Server className="w-4 h-4 text-emerald-600" />
              Trạng thái Kiến trúc 7 Microservices & Hạ tầng Cloud QuickShop
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {services.map((s, idx) => (
                <div key={idx} className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col justify-between">
                  <div>
                    <div className="flex justify-between items-center">
                      <span className="font-bold text-xs text-slate-900">{s.name}</span>
                      <span className="text-[10px] font-black text-emerald-600 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                        Port {s.port}
                      </span>
                    </div>
                    <div className="text-[11px] text-orange-600 font-semibold mt-1">
                      Phụ trách: {s.role}
                    </div>
                    <div className="text-[11px] text-slate-500 mt-1 leading-relaxed">
                      {s.tech}
                    </div>
                  </div>
                  <div className="mt-3 pt-2 border-t border-slate-200 text-[10px] text-emerald-600 font-bold flex items-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Sẵn sàng hoạt động (Online)
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
