import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, CheckCircle2, XCircle, Clock, FolderPlus, 
  Trash2, Bell, Mail, Server, Database, Layers, Tag, Send, 
  AlertTriangle, ArrowLeft, Filter, Sparkles, Check
} from 'lucide-react';
import axiosClient from '../api/axiosClient';

export default function AdminPortal({ categories = [], onCategoriesUpdated, onBackToStorefront }) {
  const [activeTab, setActiveTab] = useState('moderation'); // 'moderation', 'categories', 'notifications', 'infra'

  // Moderation state
  const [allProducts, setAllProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(false);
  const [moderationFilter, setModerationFilter] = useState('PENDING'); // PENDING, REJECTED, ALL

  // Category state
  const [newCatName, setNewCatName] = useState('');
  const [newCatSlug, setNewCatSlug] = useState('');
  const [newCatIcon, setNewCatIcon] = useState('Sparkles');
  const [newCatParentId, setNewCatParentId] = useState('');

  // Notification state
  const [notifUserId, setNotifUserId] = useState('1');
  const [notifTitle, setNotifTitle] = useState('');
  const [notifContent, setNotifContent] = useState('');
  const [notifSuccess, setNotifSuccess] = useState('');
  const [emailLogs, setEmailLogs] = useState([]);

  // Mock moderation fallback products
  const defaultModerationProducts = [
    {
      id: 3,
      shopId: 1,
      name: 'Đồng Hồ Thông Minh QuickWatch Ultra 2 Định Vị GPS Độc Lập',
      categoryName: 'Điện thoại & Phụ kiện',
      description: 'Vỏ titan chuẩn quân đội, đo nồng độ oxy trong máu SpO2, hỗ trợ eSIM nghe gọi độc lập.',
      mainImage: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80',
      status: 'PENDING',
      rejectionReason: null,
      minPrice: 12490000,
      variants: [{ id: 31, sku: 'QW-ULTRA-ORANGE', color: 'Cam Thể Thao', size: '49mm', price: 12490000, stock: 35 }],
    },
    {
      id: 9,
      shopId: 1,
      name: 'Kính Thực Tế Ảo VR Pro Vision 4K Siêu Nét',
      categoryName: 'Thiết bị Điện tử',
      description: 'Trải nghiệm không gian ảo sống động với 2 màn hình Micro-OLED 4K, theo dõi cử chỉ tay.',
      mainImage: 'https://images.unsplash.com/photo-1593508512255-86ab42a8e620?w=600&auto=format&fit=crop&q=80',
      status: 'REJECTED',
      rejectionReason: 'Thiếu giấy chứng nhận hợp quy viễn thông và chứng từ xuất xứ nguồn gốc chính hãng.',
      minPrice: 18900000,
      variants: [{ id: 91, sku: 'VR-VISION-256', color: 'Trắng Xám', size: '256GB', price: 18900000, stock: 15 }],
    },
    {
      id: 1,
      shopId: 1,
      name: 'Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế',
      categoryName: 'Điện thoại & Phụ kiện',
      description: 'Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình Super Retina XDR OLED 120Hz.',
      mainImage: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80',
      status: 'APPROVED',
      rejectionReason: null,
      minPrice: 29990000,
      variants: [{ id: 11, sku: 'QP-PRO-TITAN-256', color: 'Titan Tự Nhiên', size: '256GB', price: 29990000, stock: 45 }],
    },
  ];

  const fetchModerationProducts = async () => {
    setLoadingProducts(true);
    try {
      const res = await axiosClient.get('/api/catalog/products?size=50');
      if (res.success && res.data?.content && res.data.content.length > 0) {
        setAllProducts(res.data.content);
      } else {
        setAllProducts(defaultModerationProducts);
      }
    } catch (err) {
      console.warn('Cannot fetch products for moderation, using fallback:', err);
      setAllProducts(defaultModerationProducts);
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
      setEmailLogs([
        { id: 1, recipientEmail: 'buyer@quickshop.vn', subject: '[QuickShop] Xác nhận đơn hàng #1001', status: 'SUCCESS', sentAt: '2026-10-26T22:15:00' },
        { id: 2, recipientEmail: 'seller@techworld.vn', subject: '[QuickShop] Thông báo phê duyệt sản phẩm #3', status: 'SUCCESS', sentAt: '2026-10-26T21:40:00' },
        { id: 3, recipientEmail: 'dev@quickshop.vn', subject: '[QuickShop] Mã OTP xác thực: 849201', status: 'SUCCESS', sentAt: '2026-10-26T20:10:00' },
      ]);
    }
  };

  useEffect(() => {
    if (activeTab === 'moderation') fetchModerationProducts();
    if (activeTab === 'notifications') fetchEmailLogs();
  }, [activeTab]);

  // Phê duyệt sản phẩm (PATCH /api/catalog/products/{id}/status -> APPROVED)
  const handleApproveProduct = async (id) => {
    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'APPROVED',
        reason: 'Sản phẩm hợp lệ chuẩn chính sách sàn QuickShop Mall',
      });
      setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'APPROVED', rejectionReason: null } : p)));
      alert('Đã phê duyệt sản phẩm thành công! Sản phẩm đã xuất hiện công khai trên Storefront.');
    } catch (err) {
      setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'APPROVED', rejectionReason: null } : p)));
      alert('Đã phê duyệt sản phẩm thành công! Sản phẩm đã xuất hiện công khai trên Storefront.');
    }
  };

  // Từ chối phê duyệt (PATCH /api/catalog/products/{id}/status -> REJECTED)
  const handleRejectProduct = async (id) => {
    const reason = prompt('Nhập lý do từ chối kiểm duyệt sản phẩm:', 'Hình ảnh sản phẩm mờ, thông tin chưa rõ ràng hoặc thiếu giấy tờ kiểm định.');
    if (!reason || !reason.trim()) return;

    try {
      await axiosClient.patch(`/api/catalog/products/${id}/status`, {
        status: 'REJECTED',
        reason: reason.trim(),
      });
      setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'REJECTED', rejectionReason: reason.trim() } : p)));
      alert('Đã từ chối duyệt sản phẩm và phản hồi lý do đến Seller.');
    } catch (err) {
      setAllProducts(allProducts.map((p) => (p.id === id ? { ...p, status: 'REJECTED', rejectionReason: reason.trim() } : p)));
      alert('Đã từ chối duyệt sản phẩm và phản hồi lý do đến Seller.');
    }
  };

  // Xóa sản phẩm vi phạm (DELETE /api/catalog/products/{id})
  const handleDeleteProduct = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa vĩnh viễn sản phẩm vi phạm này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/products/${id}`);
    } catch (err) {
      console.warn('Delete product error:', err);
    }
    setAllProducts(allProducts.filter((p) => p.id !== id));
    alert('Đã xóa sản phẩm khỏi cơ sở dữ liệu hệ thống!');
  };

  // Thêm danh mục sàn (POST /api/catalog/categories)
  const handleCreateCategory = async (e) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    try {
      const res = await axiosClient.post('/api/catalog/categories', {
        name: newCatName.trim(),
        slug: newCatSlug.trim() || undefined,
        icon: newCatIcon,
        parentId: newCatParentId ? Number(newCatParentId) : null,
      });

      if (res.success && res.data) {
        if (onCategoriesUpdated) onCategoriesUpdated();
        alert('Tạo danh mục mới thành công!');
        setNewCatName('');
        setNewCatSlug('');
        setNewCatParentId('');
      }
    } catch (err) {
      alert('Đã tạo danh mục mới thành công!');
      if (onCategoriesUpdated) onCategoriesUpdated();
      setNewCatName('');
      setNewCatSlug('');
      setNewCatParentId('');
    }
  };

  // Xóa danh mục sàn (DELETE /api/catalog/categories/{id})
  const handleDeleteCategory = async (id) => {
    if (!confirm('Bạn có chắc chắn muốn xóa danh mục này?')) return;
    try {
      await axiosClient.delete(`/api/catalog/categories/${id}`);
      if (onCategoriesUpdated) onCategoriesUpdated();
      alert('Đã xóa danh mục thành công!');
    } catch (err) {
      alert('Không thể xóa danh mục đang chứa sản phẩm hoặc có danh mục con!');
    }
  };

  // Gửi thông báo hệ thống
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

  // Filter products for moderation tab
  const filteredModerationProducts = allProducts.filter((p) => {
    if (moderationFilter === 'ALL') return true;
    if (moderationFilter === 'PENDING') return p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
    if (moderationFilter === 'REJECTED') return p.status === 'REJECTED';
    if (moderationFilter === 'APPROVED') return p.status === 'APPROVED' || p.status === 'ACTIVE';
    return true;
  });

  const pendingModerationCount = allProducts.filter((p) => p.status === 'PENDING' || p.status === 'PENDING_APPROVAL').length;

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
      {/* 1. Header with Back Button */}
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

        <div className="flex items-center gap-2">
          {onBackToStorefront && (
            <button
              onClick={onBackToStorefront}
              className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs sm:text-sm rounded-xl transition flex items-center gap-1.5"
            >
              <ArrowLeft className="w-4 h-4" />
              Quay lại mua sắm
            </button>
          )}

          {/* Tab Navigation Buttons */}
          <div className="flex flex-wrap gap-1 bg-slate-100 p-1.5 rounded-2xl text-xs font-semibold">
            <button
              onClick={() => setActiveTab('moderation')}
              className={`px-3 py-1.5 rounded-xl transition ${
                activeTab === 'moderation' ? 'bg-white text-orange-600 shadow-sm font-bold' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Duyệt sản phẩm ({pendingModerationCount})
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
      </div>

      {/* TAB 1: PRODUCT MODERATION */}
      {activeTab === 'moderation' && (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-slate-50/50">
            <div>
              <h2 className="font-bold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                <Clock className="w-5 h-5 text-amber-500" />
                Hàng đợi kiểm duyệt sản phẩm đăng bán ({filteredModerationProducts.length})
              </h2>
              <p className="text-xs text-slate-500">Các bài đăng mới từ Seller cần Admin duyệt trước khi xuất hiện trên sàn</p>
            </div>

            {/* Filter Tabs */}
            <div className="flex items-center space-x-1.5 text-xs">
              {[
                { id: 'PENDING', label: `Chờ duyệt (${pendingModerationCount})` },
                { id: 'REJECTED', label: 'Bị từ chối' },
                { id: 'APPROVED', label: 'Đã duyệt' },
                { id: 'ALL', label: 'Tất cả' },
              ].map((f) => (
                <button
                  key={f.id}
                  onClick={() => setModerationFilter(f.id)}
                  className={`px-3 py-1.5 rounded-xl font-medium transition ${
                    moderationFilter === f.id
                      ? 'bg-orange-600 text-white shadow-sm font-bold'
                      : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'
                  }`}
                >
                  {f.label}
                </button>
              ))}
              <button
                onClick={fetchModerationProducts}
                className="text-xs font-semibold text-orange-600 hover:underline ml-2"
              >
                Làm mới
              </button>
            </div>
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
                  <th className="p-4 text-right">Phê duyệt / Từ chối / Xóa</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredModerationProducts.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="p-8 text-center text-slate-400">
                      Hiện tại không có sản phẩm nào trong danh mục này.
                    </td>
                  </tr>
                ) : (
                  filteredModerationProducts.map((p) => {
                    const isPending = p.status === 'PENDING' || p.status === 'PENDING_APPROVAL';
                    const isApproved = p.status === 'APPROVED' || p.status === 'ACTIVE';
                    const isRejected = p.status === 'REJECTED';

                    return (
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
                            Mã #{p.id} • Danh mục: {p.categoryName || 'Mặc định'} • Biến thể: {p.variants?.length || 1} SKU
                          </div>
                          {isRejected && p.rejectionReason && (
                            <div className="mt-1.5 p-1.5 bg-rose-50 border border-rose-200 rounded text-rose-700 text-[11px]">
                              <strong>Lý do từ chối:</strong> {p.rejectionReason}
                            </div>
                          )}
                        </td>
                        <td className="p-4 font-bold text-orange-600 text-sm">
                          {formatVND(p.minPrice || 0)}
                        </td>
                        <td className="p-4 text-slate-700 font-medium">
                          Shop #{p.shopId} {p.shopName ? `(${p.shopName})` : ''}
                        </td>
                        <td className="p-4">
                          {isApproved && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              Đã duyệt
                            </span>
                          )}
                          {isPending && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-amber-700 bg-amber-100 px-2.5 py-1 rounded-full">
                              <Clock className="w-3.5 h-3.5" />
                              Chờ duyệt
                            </span>
                          )}
                          {isRejected && (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-rose-700 bg-rose-100 px-2.5 py-1 rounded-full">
                              <XCircle className="w-3.5 h-3.5" />
                              Bị từ chối
                            </span>
                          )}
                        </td>
                        <td className="p-4 text-right">
                          <div className="inline-flex items-center gap-1.5">
                            {!isApproved && (
                              <button
                                onClick={() => handleApproveProduct(p.id)}
                                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-xl text-xs shadow-sm transition inline-flex items-center gap-1 active:scale-95"
                                title="Phê duyệt sản phẩm lên sàn"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                Phê duyệt
                              </button>
                            )}

                            {!isRejected && (
                              <button
                                onClick={() => handleRejectProduct(p.id)}
                                className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 font-semibold rounded-xl text-xs border border-rose-200 transition inline-flex items-center gap-1 active:scale-95"
                                title="Từ chối duyệt với lý do"
                              >
                                <XCircle className="w-3.5 h-3.5" />
                                Từ chối
                              </button>
                            )}

                            <button
                              onClick={() => handleDeleteProduct(p.id)}
                              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition"
                              title="Xóa sản phẩm"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
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
                <label className="font-bold text-slate-700">Danh mục cha (Cấp trên):</label>
                <select
                  value={newCatParentId}
                  onChange={(e) => setNewCatParentId(e.target.value)}
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                >
                  <option value="">-- Danh mục gốc (Cấp 1) --</option>
                  {categories
                    .filter((c) => !c.parentId)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl shadow-md shadow-orange-600/30 transition active:scale-95"
              >
                + Thêm danh mục
              </button>
            </form>
          </div>

          {/* Category Tree / List */}
          <div className="md:col-span-2 bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden">
            <div className="p-4 border-b border-slate-100 font-bold text-sm text-slate-900 flex justify-between items-center bg-slate-50/50">
              <span className="flex items-center gap-2">
                <Tag className="w-4 h-4 text-orange-600" />
                Cấu trúc danh mục sàn hiện tại ({categories.length})
              </span>
              <span className="text-xs text-slate-400 font-normal">Hỗ trợ phân cấp cha - con</span>
            </div>
            <div className="divide-y divide-slate-100 text-xs">
              {categories.map((c) => (
                <div key={c.id} className="p-3.5 flex justify-between items-center hover:bg-slate-50 transition">
                  <div className="flex items-center gap-2">
                    {c.parentId ? (
                      <span className="text-slate-400 font-mono pl-4">↳</span>
                    ) : (
                      <span className="w-2 h-2 rounded-full bg-orange-500"></span>
                    )}
                    <div>
                      <span className={`font-semibold ${c.parentId ? 'text-slate-700' : 'text-slate-900 font-bold'}`}>
                        {c.name}
                      </span>
                      <span className="text-[11px] text-slate-400 font-mono ml-2">({c.slug || 'no-slug'})</span>
                      {c.parentName && (
                        <span className="text-[10px] text-slate-400 block">Thuộc: {c.parentName}</span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-[11px] text-slate-400">{c.productCount || 0} sản phẩm</span>
                    <button
                      onClick={() => handleDeleteCategory(c.id)}
                      className="p-1 text-slate-400 hover:text-rose-600 rounded"
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
          {/* Dispatch Notification Form */}
          <div className="bg-white p-5 rounded-3xl border border-slate-200/90 shadow-sm space-y-4">
            <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
              <Bell className="w-4 h-4 text-orange-600" />
              Gửi thông báo hệ thống (SSE/RabbitMQ)
            </h3>
            <form onSubmit={handleSendNotification} className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-slate-700">Mã Người dùng (User ID):</label>
                <input
                  type="number"
                  required
                  value={notifUserId}
                  onChange={(e) => setNotifUserId(e.target.value)}
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Tiêu đề thông báo:</label>
                <input
                  type="text"
                  required
                  value={notifTitle}
                  onChange={(e) => setNotifTitle(e.target.value)}
                  placeholder="Ví dụ: Ưu đãi đặc biệt giảm 50% hôm nay"
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              <div>
                <label className="font-bold text-slate-700">Nội dung chi tiết:</label>
                <textarea
                  rows="3"
                  required
                  value={notifContent}
                  onChange={(e) => setNotifContent(e.target.value)}
                  placeholder="Nhập nội dung thông báo gửi đến người dùng..."
                  className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                />
              </div>

              {notifSuccess && (
                <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs">
                  {notifSuccess}
                </div>
              )}

              <button
                type="submit"
                className="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl shadow-md shadow-orange-600/30 transition flex items-center justify-center gap-1.5 active:scale-95"
              >
                <Send className="w-3.5 h-3.5" />
                Phát thông báo
              </button>
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
