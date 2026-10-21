import React, { useState } from 'react';
import { useAuthStore } from './stores/useAuthStore';
import axiosClient from './api/axiosClient';
import { ShoppingBag, ShieldCheck, Database, Layers, CheckCircle2, User, Key, Server } from 'lucide-react';

export default function App() {
  const { user, isAuthenticated, login, logout } = useAuthStore();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      // Gọi API login qua API Gateway (Port 8080) -> Định tuyến đến Identity Service (Port 8081)
      const res = await axiosClient.post('/api/auth/login', {
        usernameOrEmail: username,
        password: password,
      });

      if (res.success && res.data) {
        login(res.data);
        setSuccessMsg(`Đăng nhập thành công với tài khoản: ${res.data.fullName} (${res.data.role})`);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Không thể kết nối đến API Gateway');
    } finally {
      setLoading(false);
    }
  };

  const services = [
    { name: 'API Gateway', port: '8080', tech: 'Spring Cloud Gateway, Redis Rate Limiter', status: 'Online' },
    { name: 'Identity Service', port: '8081', tech: 'Spring Security, JWT, identity_db', status: 'Online' },
    { name: 'Catalog Service', port: '8082', tech: 'Redis, MinIO, RabbitMQ, catalog_db', status: 'Online' },
    { name: 'Order Service', port: '8083', tech: 'Saga Choreography, Redis, order_db', status: 'Online' },
    { name: 'Payment Service', port: '8084', tech: 'VNPay Integration, RabbitMQ', status: 'Online' },
    { name: 'Notification & AI', port: '8085', tech: 'Spring AI, RabbitMQ, Mail OTP', status: 'Online' },
  ];

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6 md:p-12">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Header */}
        <header className="flex flex-col md:flex-row justify-between items-start md:items-center border-b border-slate-800 pb-6 gap-4">
          <div className="flex items-center space-x-3">
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-400">
              <ShoppingBag className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
                QuickShop Microservices
                <span className="text-xs bg-emerald-950 text-emerald-400 border border-emerald-800 px-2 py-0.5 rounded-full font-mono">
                  v1.0.0 Architecture
                </span>
              </h1>
              <p className="text-sm text-slate-400">Hệ thống thương mại điện tử phân tán chuẩn Enterprise</p>
            </div>
          </div>

          <div>
            {isAuthenticated ? (
              <div className="flex items-center gap-4 bg-slate-800/80 p-2.5 rounded-xl border border-slate-700">
                <div className="flex items-center gap-2 text-sm text-slate-200">
                  <User className="w-4 h-4 text-emerald-400" />
                  <span>{user?.fullName}</span>
                  <span className="text-xs bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded border border-emerald-500/30">
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
              <div className="text-xs text-amber-400 bg-amber-950/40 border border-amber-800/50 px-3 py-2 rounded-lg flex items-center gap-2">
                <Key className="w-3.5 h-3.5" />
                Chưa đăng nhập (Khách vãng lai)
              </div>
            )}
          </div>
        </header>

        {/* Grid content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Microservices Topology List */}
          <div className="lg:col-span-2 space-y-4">
            <h2 className="text-lg font-semibold text-slate-200 flex items-center gap-2">
              <Server className="w-5 h-5 text-emerald-400" />
              Tô pô các dịch vụ phân tán (Backend Services)
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {services.map((svc) => (
                <div
                  key={svc.name}
                  className="bg-slate-800/60 border border-slate-700/80 p-4 rounded-xl hover:border-emerald-500/40 transition group"
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className="font-semibold text-white group-hover:text-emerald-400 transition">
                      {svc.name}
                    </span>
                    <span className="text-xs bg-slate-700 text-slate-300 font-mono px-2 py-0.5 rounded">
                      Port {svc.port}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mb-3">{svc.tech}</p>
                  <div className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>Healthcheck: {svc.status}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Test Gateway & Auth Panel */}
          <div className="bg-slate-800/80 border border-slate-700 p-6 rounded-2xl flex flex-col justify-between">
            <div>
              <h2 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-400" />
                Kiểm thử xác thực qua API Gateway
              </h2>
              <p className="text-xs text-slate-400 mb-4">
                Đăng nhập tài khoản seed sẵn qua Endpoint <code className="text-emerald-400">/api/auth/login</code>
              </p>

              {errorMsg && (
                <div className="mb-4 text-xs bg-rose-950/50 border border-rose-800 text-rose-300 p-3 rounded-lg">
                  {errorMsg}
                </div>
              )}

              {successMsg && (
                <div className="mb-4 text-xs bg-emerald-950/50 border border-emerald-800 text-emerald-300 p-3 rounded-lg">
                  {successMsg}
                </div>
              )}

              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Username / Email</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                    placeholder="admin hoặc customer"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Mật khẩu</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500"
                    placeholder="admin123"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-emerald-600 hover:bg-emerald-500 text-white font-medium py-2 rounded-lg text-sm transition shadow-lg shadow-emerald-900/30 disabled:opacity-50"
                >
                  {loading ? 'Đang gửi yêu cầu...' : 'Gửi yêu cầu đăng nhập'}
                </button>
              </form>
            </div>

            <div className="mt-6 pt-4 border-t border-slate-700/60 text-[11px] text-slate-400 space-y-1">
              <div>• Gateway URL: <code className="text-slate-300">http://localhost:8080</code></div>
              <div>• Auth Route: <code className="text-slate-300">Gateway 8080 ➔ Identity 8081</code></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
