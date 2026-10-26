import React, { useState } from 'react';
import { useAuthStore } from './stores/useAuthStore';
import axiosClient from './api/axiosClient';
import { 
  ShoppingBag, ShieldCheck, Database, Layers, CheckCircle2, User, Key, Server, 
  Store, ShoppingCart, Truck, CreditCard, Bot, ArrowRight, Sparkles, AlertCircle
} from 'lucide-react';

export default function App() {
  const { user, isAuthenticated, login, logout } = useAuthStore();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  
  // Tab navigation
  const [activeTab, setActiveTab] = useState('services'); // 'services', 'auth', 'orders', 'ai'

  // AI Chat test state
  const [aiMessage, setAiMessage] = useState('');
  const [aiResponse, setAiResponse] = useState('');
  const [aiLoading, setAiLoading] = useState(false);

  const handleLogin = async (e) => {
    if (e) e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const res = await axiosClient.post('/api/auth/login', {
        usernameOrEmail: username,
        password: password,
      });

      if (res.success && res.data) {
        login(res.data);
        setSuccessMsg(`Đăng nhập thành công với vai trò: ${res.data.fullName} [${res.data.role}]`);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Không thể kết nối đến API Gateway (Port 8080)');
    } finally {
      setLoading(false);
    }
  };

  const setRoleAccount = (u, p) => {
    setUsername(u);
    setPassword(p);
    setErrorMsg('');
    setSuccessMsg('');
  };

  const handleAiChat = async (e) => {
    e.preventDefault();
    if (!aiMessage.trim()) return;
    setAiLoading(true);
    try {
      const res = await axiosClient.post('/api/ai/chat', { message: aiMessage });
      if (res.success && res.data) {
        setAiResponse(res.data.reply);
      }
    } catch (err) {
      setAiResponse('Lỗi kết nối Trợ lý AI: ' + (err.message || 'Vui lòng kiểm tra Notification & AI Service (Port 8086)'));
    } finally {
      setAiLoading(false);
    }
  };

  const services = [
    { name: 'API Gateway', port: '8080', icon: ShieldCheck, tech: 'Spring Cloud Gateway, Redis Rate Limiter (CF-Connecting-IP), Dual Auth', status: 'Online' },
    { name: 'Identity & Shop', port: '8081', icon: User, tech: 'RBAC (Buyer, Seller, Admin), Shop KYC, identity_db', status: 'Online' },
    { name: 'Catalog & Product', port: '8082', icon: Store, tech: 'Multi-variant SKU, MinIO S3, Redis Cache, Reviews, catalog_db', status: 'Online' },
    { name: 'Order & Cart', port: '8083', icon: ShoppingCart, tech: 'Saga Orchestrator, Split Sub-Orders, Redis Cart, order.dlq, order_db', status: 'Online' },
    { name: 'Payment & Wallet', port: '8084', icon: CreditCard, tech: 'VNPay Sandbox, Escrow Ledger, Seller Wallet, payment_db', status: 'Online' },
    { name: 'Shipping & Logistics', port: '8085', icon: Truck, tech: 'GHN/GHTK, Idempotent Webhook (event_id), shipping_db', status: 'Online' },
    { name: 'Notification & AI', port: '8086', icon: Bot, tech: 'Spring AI Chatbot, Prompt Sanitization, RabbitMQ Mail OTP, notification_db', status: 'Online' },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-8 font-sans">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center bg-slate-900/90 border border-slate-800 p-6 rounded-2xl gap-4 shadow-xl backdrop-blur-sm">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl text-emerald-400 shadow-inner">
              <ShoppingBag className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                QuickShop Marketplace
                <span className="text-xs bg-emerald-950 text-emerald-400 border border-emerald-800 px-2.5 py-0.5 rounded-full font-mono font-medium">
                  v2.2.0 Production-Ready
                </span>
              </h1>
              <p className="text-sm text-slate-400">
                Sàn TMĐT Đa người bán (Multi-Vendor) • 7 Microservices • 6 MySQL DBs • Saga Orchestrator
              </p>
            </div>
          </div>

          <div>
            {isAuthenticated ? (
              <div className="flex items-center gap-4 bg-slate-800/90 p-2.5 px-4 rounded-xl border border-slate-700">
                <div className="flex items-center gap-2 text-sm text-slate-200">
                  <User className="w-4 h-4 text-emerald-400" />
                  <span className="font-medium">{user?.fullName}</span>
                  <span className="text-xs bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded border border-emerald-500/30 font-mono">
                    {user?.role}
                  </span>
                </div>
                <button
                  onClick={logout}
                  className="text-xs bg-rose-600/80 hover:bg-rose-600 text-white px-3 py-1.5 rounded-lg transition"
                >
                  Đăng xuất
                </button>
              </div>
            ) : (
              <div className="text-xs text-amber-400 bg-amber-950/40 border border-amber-800/50 px-3.5 py-2 rounded-xl flex items-center gap-2">
                <Key className="w-4 h-4 text-amber-400" />
                <span>Chưa đăng nhập (Khách vãng lai)</span>
              </div>
            )}
          </div>
        </header>

        {/* Navigation Tabs */}
        <div className="flex flex-wrap gap-2 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('services')}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition flex items-center gap-2 ${
              activeTab === 'services'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/30'
                : 'bg-slate-900/60 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Server className="w-4 h-4" />
            Tô pô 7 Services ({services.length})
          </button>

          <button
            onClick={() => setActiveTab('auth')}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition flex items-center gap-2 ${
              activeTab === 'auth'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/30'
                : 'bg-slate-900/60 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <ShieldCheck className="w-4 h-4" />
            Kiểm thử RBAC (3 Roles)
          </button>

          <button
            onClick={() => setActiveTab('orders')}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition flex items-center gap-2 ${
              activeTab === 'orders'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/30'
                : 'bg-slate-900/60 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Layers className="w-4 h-4" />
            Mô phỏng Đơn hàng & Saga
          </button>

          <button
            onClick={() => setActiveTab('ai')}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition flex items-center gap-2 ${
              activeTab === 'ai'
                ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-900/30'
                : 'bg-slate-900/60 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Bot className="w-4 h-4" />
            Trợ lý QuickShop AI
          </button>
        </div>

        {/* Tab 1: Services Grid */}
        {activeTab === 'services' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-lg font-semibold text-slate-200 flex items-center gap-2">
                <Server className="w-5 h-5 text-emerald-400" />
                Các Microservices độc lập theo chuẩn SRS v2.2.0
              </h2>
              <span className="text-xs text-slate-400 font-mono">Gateway Route Port: 8080</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {services.map((svc) => {
                const IconComponent = svc.icon;
                return (
                  <div
                    key={svc.name}
                    className="bg-slate-900/80 border border-slate-800 p-5 rounded-2xl hover:border-emerald-500/50 transition group flex flex-col justify-between shadow-lg"
                  >
                    <div>
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex items-center gap-2.5">
                          <div className="p-2 bg-slate-800 rounded-xl text-emerald-400 group-hover:bg-emerald-500/10 transition">
                            <IconComponent className="w-5 h-5" />
                          </div>
                          <span className="font-semibold text-white group-hover:text-emerald-400 transition">
                            {svc.name}
                          </span>
                        </div>
                        <span className="text-xs bg-slate-800 text-emerald-400 font-mono px-2.5 py-1 rounded-lg border border-slate-700">
                          Port {svc.port}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mb-4 leading-relaxed">{svc.tech}</p>
                    </div>

                    <div className="flex items-center justify-between pt-3 border-t border-slate-800/80 text-xs">
                      <div className="flex items-center gap-1.5 text-emerald-400 font-medium">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Ready</span>
                      </div>
                      <span className="text-slate-500 font-mono">Database per Service</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Tab 2: RBAC Auth Test */}
        {activeTab === 'auth' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-4">
              <h2 className="text-lg font-semibold text-white flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                Kiểm thử Phân quyền (3 Roles Đã Seed Sẵn)
              </h2>
              <p className="text-sm text-slate-400 leading-relaxed">
                Hệ thống QuickShop Marketplace phân quyền nghiêm ngặt 3 vai trò: <strong className="text-emerald-400">ROLE_ADMIN</strong>, <strong className="text-sky-400">ROLE_BUYER</strong>, và <strong className="text-amber-400">ROLE_SELLER</strong> (gắn với Shop Profile). Bấm các nút chọn nhanh bên dưới để đăng nhập:
              </p>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <button
                  type="button"
                  onClick={() => setRoleAccount('admin', 'admin123')}
                  className="p-4 bg-slate-900 border border-slate-800 rounded-xl text-left hover:border-emerald-500 transition group"
                >
                  <div className="text-xs text-emerald-400 font-mono mb-1">ROLE_ADMIN</div>
                  <div className="font-semibold text-white">Quản trị viên Sàn</div>
                  <div className="text-xs text-slate-400 mt-1 font-mono">admin / admin123</div>
                </button>

                <button
                  type="button"
                  onClick={() => setRoleAccount('buyer', 'buyer123')}
                  className="p-4 bg-slate-900 border border-slate-800 rounded-xl text-left hover:border-sky-500 transition group"
                >
                  <div className="text-xs text-sky-400 font-mono mb-1">ROLE_BUYER</div>
                  <div className="font-semibold text-white">Khách hàng Mua</div>
                  <div className="text-xs text-slate-400 mt-1 font-mono">buyer / buyer123</div>
                </button>

                <button
                  type="button"
                  onClick={() => setRoleAccount('seller', 'seller123')}
                  className="p-4 bg-slate-900 border border-slate-800 rounded-xl text-left hover:border-amber-500 transition group"
                >
                  <div className="text-xs text-amber-400 font-mono mb-1">ROLE_SELLER</div>
                  <div className="font-semibold text-white">Chủ Gian hàng</div>
                  <div className="text-xs text-slate-400 mt-1 font-mono">seller / seller123</div>
                </button>
              </div>

              <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-xl text-xs text-slate-300 space-y-2">
                <div className="font-semibold text-white flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-emerald-400" />
                  Cơ chế Dual Auth tại API Gateway (Port 8080):
                </div>
                <div>• Hỗ trợ trích xuất Token từ cả Header <code className="text-emerald-400">Authorization: Bearer &lt;token&gt;</code> lẫn Cookie <code className="text-emerald-400">accessToken</code>.</div>
                <div>• Rate Limiting tự động ưu tiên nhận diện IP gốc qua header <code className="text-emerald-400">CF-Connecting-IP</code> từ Cloudflare.</div>
              </div>
            </div>

            {/* Login Box */}
            <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl flex flex-col justify-between shadow-xl">
              <form onSubmit={handleLogin} className="space-y-4">
                <h3 className="font-semibold text-white flex items-center gap-2">
                  <Key className="w-4 h-4 text-emerald-400" />
                  Đăng nhập qua Gateway
                </h3>

                {errorMsg && (
                  <div className="text-xs bg-rose-950/60 border border-rose-800 text-rose-300 p-3 rounded-xl">
                    {errorMsg}
                  </div>
                )}

                {successMsg && (
                  <div className="text-xs bg-emerald-950/60 border border-emerald-800 text-emerald-300 p-3 rounded-xl">
                    {successMsg}
                  </div>
                )}

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Tên đăng nhập / Email</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-emerald-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Mật khẩu</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-sm text-white focus:outline-none focus:border-emerald-500"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-medium py-2.5 rounded-xl text-sm transition shadow-lg shadow-emerald-900/30 disabled:opacity-50"
                >
                  {loading ? 'Đang xác thực qua Gateway...' : 'Xác thực tài khoản'}
                </button>
              </form>

              <div className="mt-4 pt-3 border-t border-slate-800 text-[11px] text-slate-400">
                Endpoint: <code className="text-slate-300">POST /api/auth/login</code>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Marketplace Multi-Vendor Orders & Saga Flow */}
        {activeTab === 'orders' && (
          <div className="space-y-6">
            <div className="bg-slate-900/90 border border-slate-800 p-6 rounded-2xl space-y-4">
              <h2 className="text-lg font-semibold text-white flex items-center gap-2">
                <Layers className="w-5 h-5 text-emerald-400" />
                Mô hình Tách Đơn Hàng Đa Gian Hàng (Master Order & Sub-Orders)
              </h2>
              <p className="text-sm text-slate-400 leading-relaxed">
                Khi khách hàng đặt giỏ hàng chứa nhiều sản phẩm từ các Shop khác nhau, Order Service đóng vai trò <strong>Saga Orchestrator</strong> tự động tạo 1 Master Order và các Sub-Orders độc lập:
              </p>

              {/* Diagram / Card Flow */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-2">
                <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl">
                  <div className="text-xs text-emerald-400 font-mono mb-1">Bước 1: Giỏ hàng đa Shop</div>
                  <div className="font-medium text-white">Buyer Checkout Giỏ hàng</div>
                  <p className="text-xs text-slate-400 mt-2">
                    Gom nhóm mặt hàng theo Shop ID. Áp dụng Voucher Sàn + Voucher từng Shop.
                  </p>
                </div>

                <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl">
                  <div className="text-xs text-emerald-400 font-mono mb-1">Bước 2: Saga Giữ kho 15 phút</div>
                  <div className="font-medium text-white">RabbitMQ: order.created</div>
                  <p className="text-xs text-slate-400 mt-2">
                    Gửi message sang Catalog Service giữ tồn kho có thời hạn. Nếu thất bại sau 3 lần retry $\rightarrow$ đẩy vào <code className="text-rose-400">order.dlq</code>.
                  </p>
                </div>

                <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl">
                  <div className="text-xs text-emerald-400 font-mono mb-1">Bước 3: Escrow & Sổ cái</div>
                  <div className="font-medium text-white">Payment & Escrow Holding</div>
                  <p className="text-xs text-slate-400 mt-2">
                    Tạm giữ tiền trong Escrow. Sau khi Sub-Order DELIVERED quá 3 ngày không khiếu nại $\rightarrow$ giải phóng tiền vào Ví Seller qua Double-Entry Ledger.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 4: AI Shopping Assistant */}
        {activeTab === 'ai' && (
          <div className="max-w-2xl mx-auto bg-slate-900 border border-slate-800 p-6 rounded-2xl shadow-xl space-y-4">
            <div className="flex items-center gap-3 border-b border-slate-800 pb-4">
              <div className="p-2.5 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400">
                <Bot className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-semibold text-white">Trợ lý Mua sắm QuickShop AI</h3>
                <p className="text-xs text-slate-400">Tích hợp Spring AI, Prompt Sanitization chống Injection (Port 8086)</p>
              </div>
            </div>

            {aiResponse && (
              <div className="bg-slate-950 border border-slate-800 p-4 rounded-xl text-sm leading-relaxed text-slate-200">
                <div className="text-xs font-semibold text-emerald-400 mb-1 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5" />
                  QuickShop AI Phản hồi:
                </div>
                {aiResponse}
              </div>
            )}

            <form onSubmit={handleAiChat} className="space-y-3">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={aiMessage}
                  onChange={(e) => setAiMessage(e.target.value)}
                  placeholder="Hỏi về điện thoại QuickPhone Pro Max hoặc laptop QuickBook..."
                  className="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-emerald-500"
                />
                <button
                  type="submit"
                  disabled={aiLoading}
                  className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium px-5 py-2.5 rounded-xl text-sm transition shadow-lg shadow-emerald-900/30 disabled:opacity-50 flex items-center gap-1.5"
                >
                  {aiLoading ? 'Đang suy nghĩ...' : 'Gửi'}
                </button>
              </div>
              <div className="text-[11px] text-slate-500 flex items-center gap-1">
                <AlertCircle className="w-3.5 h-3.5 text-slate-400" />
                Mọi truy vấn đều được làm sạch qua bộ lọc Prompt Sanitization trước khi gửi đến LLM.
              </div>
            </form>
          </div>
        )}

      </div>
    </div>
  );
}
