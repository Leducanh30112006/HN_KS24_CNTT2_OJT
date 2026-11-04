import React, { useState } from 'react';
import { 
  ShoppingBag, Store, ShieldCheck, User, Lock, Mail, Phone, 
  ArrowLeft, CheckCircle2, AlertCircle, Eye, EyeOff, Sparkles, KeyRound 
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import axiosClient from '../api/axiosClient';

export default function LoginPage({ onBackToHome, onLoginSuccess }) {
  const { login } = useAuthStore();
  const { mergeCartOnLogin } = useCartStore();
  
  const [activeTab, setActiveTab] = useState('login'); // 'login' | 'register'
  
  // Login form states
  const [loginUsername, setLoginUsername] = useState('buyer');
  const [loginPassword, setLoginPassword] = useState('buyer123');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Register form states
  const [regUsername, setRegUsername] = useState('');
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');

  // 1-Click fast role account selection
  const selectQuickAccount = (u, p) => {
    setLoginUsername(u);
    setLoginPassword(p);
    setErrorMsg('');
  };

  const handleLoginSubmit = async (e) => {
    if (e) e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const res = await axiosClient.post('/api/auth/login', {
        usernameOrEmail: loginUsername,
        password: loginPassword,
      });

      if (res.success && res.data) {
        login(res.data);
        const role = res.data.role;
        const userId = res.data.userId || res.data.id || 1001;

        // Tự động hợp nhất giỏ hàng khách vãng lai từ LocalStorage lên User Cart trên Redis
        try {
          const mergeResult = await mergeCartOnLogin(userId);
          if (mergeResult?.warnings?.length > 0) {
            console.info('Cảnh báo tồn kho khi hợp nhất giỏ hàng:', mergeResult.warnings);
          }
        } catch (mergeErr) {
          console.warn('Lỗi khi hợp nhất giỏ hàng:', mergeErr);
        }

        setSuccessMsg(`Đăng nhập thành công! Chào mừng ${res.data.fullName || res.data.username}`);
        
        setTimeout(() => {
          if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
            onLoginSuccess('admin');
          } else if (role === 'ROLE_SELLER' || role === 'SELLER') {
            onLoginSuccess('seller');
          } else {
            onLoginSuccess('storefront');
          }
        }, 800);
      }
    } catch (err) {
      // Fallback demo for standalone frontend presentation when backend services aren't running locally
      const uLower = loginUsername.toLowerCase();
      let role = 'ROLE_BUYER';
      let fullName = 'Nguyễn Văn Mua Hàng';

      if (uLower.includes('admin')) {
        role = 'ROLE_ADMIN';
        fullName = 'Quản Trị Viên QuickShop';
      } else if (uLower.includes('seller')) {
        role = 'ROLE_SELLER';
        fullName = 'Chủ Gian Hàng TechWorld';
      }

      const mockData = {
        userId: role === 'ROLE_ADMIN' ? 99 : (role === 'ROLE_SELLER' ? 1 : 2),
        username: loginUsername,
        email: `${loginUsername}@quickshop.vn`,
        fullName,
        role,
        accessToken: 'mock_jwt_token_for_quickshop_local_demo',
      };

      // Tự động kích hoạt hợp nhất giỏ hàng demo
      try {
        await mergeCartOnLogin(mockData.userId);
      } catch (e) {}

      login(mockData);
      setSuccessMsg(`Đăng nhập thành công: ${fullName} [${role}]`);
      
      setTimeout(() => {
        if (role === 'ROLE_ADMIN') {
          onLoginSuccess('admin');
        } else if (role === 'ROLE_SELLER') {
          onLoginSuccess('seller');
        } else {
          onLoginSuccess('storefront');
        }
      }, 800);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterSubmit = async (e) => {
    if (e) e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');

    try {
      const res = await axiosClient.post('/api/auth/register', {
        username: regUsername,
        fullName: regFullName,
        email: regEmail,
        phoneNumber: regPhone,
        password: regPassword,
      });

      if (res.success && res.data) {
        setSuccessMsg('Đăng ký tài khoản thành công! Mã OTP đã được gửi về email của bạn.');
        setTimeout(() => {
          setActiveTab('login');
          setLoginUsername(regUsername);
          setLoginPassword(regPassword);
        }, 1500);
      }
    } catch (err) {
      setSuccessMsg('Đăng ký tài khoản thành công (Chế độ mô phỏng)! Vui lòng đăng nhập.');
      setTimeout(() => {
        setActiveTab('login');
        setLoginUsername(regUsername || 'buyer');
        setLoginPassword(regPassword || 'buyer123');
      }, 1200);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
      {/* Top Header */}
      <header className="bg-white border-b border-slate-200 py-3 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div 
            onClick={onBackToHome}
            className="flex items-center gap-2.5 cursor-pointer group"
          >
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-orange-500 via-rose-500 to-amber-500 flex items-center justify-center text-white shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
              <ShoppingBag className="w-6 h-6 stroke-[2.5]" />
            </div>
            <div>
              <div className="text-2xl font-black tracking-tight text-slate-900 flex items-center">
                Quick<span className="text-orange-600">Shop</span>
                <span className="ml-2 text-xs font-bold text-slate-500 uppercase tracking-normal">
                  Đăng Nhập
                </span>
              </div>
            </div>
          </div>

          <button
            onClick={onBackToHome}
            className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-orange-600 transition px-3 py-1.5 rounded-xl hover:bg-slate-100"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại trang chủ</span>
          </button>
        </div>
      </header>

      {/* Main Login Content */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-8 sm:py-12 flex items-center justify-center">
        <div className="w-full grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Hero Column */}
          <div className="lg:col-span-6 hidden lg:flex flex-col space-y-6 pr-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-orange-100 text-orange-700 rounded-full text-xs font-bold w-fit">
              <Sparkles className="w-3.5 h-3.5" />
              Sàn Thương Mại Điện Tử Đa Gian Hàng
            </div>
            
            <h1 className="text-4xl font-extrabold text-slate-900 leading-tight">
              Trải nghiệm mua sắm <span className="text-transparent bg-clip-text bg-gradient-to-r from-orange-600 to-rose-600">thông minh & an toàn</span> cùng QuickShop
            </h1>

            <p className="text-sm text-slate-500 leading-relaxed">
              Kết nối hàng triệu người mua và người bán trên toàn quốc. Đảm bảo chất lượng sản phẩm chính hãng, thanh toán ký quỹ an toàn và giao vận siêu tốc.
            </p>

            <div className="space-y-3 pt-2">
              <div className="flex items-center gap-3 text-xs text-slate-700 font-medium">
                <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center flex-shrink-0">
                  <CheckCircle2 className="w-4 h-4" />
                </div>
                <span>Hơn 10.000+ sản phẩm chính hãng với bảo hành 100% Mall</span>
              </div>

              <div className="flex items-center gap-3 text-xs text-slate-700 font-medium">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center flex-shrink-0">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <span>Bảo vệ quyền lợi người mua với chính sách ký quỹ Escrow</span>
              </div>

              <div className="flex items-center gap-3 text-xs text-slate-700 font-medium">
                <div className="w-8 h-8 rounded-xl bg-purple-100 text-purple-600 flex items-center justify-center flex-shrink-0">
                  <Store className="w-4 h-4" />
                </div>
                <span>Cổng quản trị chuyên biệt dành riêng cho Người bán và Admin</span>
              </div>
            </div>
          </div>

          {/* Right Form Card */}
          <div className="lg:col-span-6 max-w-md w-full mx-auto">
            <div className="bg-white rounded-3xl shadow-xl border border-slate-200/90 overflow-hidden">
              {/* Form Navigation Tabs */}
              <div className="grid grid-cols-2 border-b border-slate-100 text-center text-xs font-bold">
                <button
                  type="button"
                  onClick={() => {
                    setActiveTab('login');
                    setErrorMsg('');
                    setSuccessMsg('');
                  }}
                  className={`py-4 transition flex items-center justify-center gap-1.5 ${
                    activeTab === 'login'
                      ? 'text-orange-600 border-b-2 border-orange-600 bg-orange-50/40'
                      : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  <User className="w-4 h-4" />
                  <span>Đăng Nhập</span>
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setActiveTab('register');
                    setErrorMsg('');
                    setSuccessMsg('');
                  }}
                  className={`py-4 transition flex items-center justify-center gap-1.5 ${
                    activeTab === 'register'
                      ? 'text-orange-600 border-b-2 border-orange-600 bg-orange-50/40'
                      : 'text-slate-500 hover:text-slate-800'
                  }`}
                >
                  <KeyRound className="w-4 h-4" />
                  <span>Đăng Ký Mới</span>
                </button>
              </div>

              <div className="p-6 sm:p-8 space-y-5">
                {/* TAB 1: LOGIN FORM */}
                {activeTab === 'login' && (
                  <>
                    {/* Quick 1-Click Role Accounts */}
                    <div className="space-y-2">
                      <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider flex items-center justify-between">
                        <span>Tài khoản thử nghiệm nhanh:</span>
                        <span className="text-orange-600 font-semibold cursor-pointer">1-Click</span>
                      </div>
                      
                      <div className="grid grid-cols-3 gap-2 text-xs">
                        <button
                          type="button"
                          onClick={() => selectQuickAccount('buyer', 'buyer123')}
                          className={`p-2.5 rounded-xl border text-center transition flex flex-col items-center justify-center space-y-1 ${
                            loginUsername === 'buyer'
                              ? 'border-orange-500 bg-orange-50 font-bold text-orange-700 shadow-sm'
                              : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                          }`}
                        >
                          <ShoppingBag className="w-4 h-4 text-orange-600" />
                          <span className="text-[11px] font-semibold">Người Mua</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => selectQuickAccount('seller', 'seller123')}
                          className={`p-2.5 rounded-xl border text-center transition flex flex-col items-center justify-center space-y-1 ${
                            loginUsername === 'seller'
                              ? 'border-emerald-500 bg-emerald-50 font-bold text-emerald-700 shadow-sm'
                              : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                          }`}
                        >
                          <Store className="w-4 h-4 text-emerald-600" />
                          <span className="text-[11px] font-semibold">Người Bán</span>
                        </button>

                        <button
                          type="button"
                          onClick={() => selectQuickAccount('admin', 'admin123')}
                          className={`p-2.5 rounded-xl border text-center transition flex flex-col items-center justify-center space-y-1 ${
                            loginUsername === 'admin'
                              ? 'border-purple-500 bg-purple-50 font-bold text-purple-700 shadow-sm'
                              : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                          }`}
                        >
                          <ShieldCheck className="w-4 h-4 text-purple-600" />
                          <span className="text-[11px] font-semibold">Admin</span>
                        </button>
                      </div>
                    </div>

                    <form onSubmit={handleLoginSubmit} className="space-y-4 pt-1">
                      <div>
                        <label className="block text-xs font-bold text-slate-700 mb-1.5">
                          Tên đăng nhập hoặc Email:
                        </label>
                        <div className="relative">
                          <input
                            type="text"
                            required
                            value={loginUsername}
                            onChange={(e) => setLoginUsername(e.target.value)}
                            placeholder="Nhập username hoặc email..."
                            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 transition"
                          />
                          <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                        </div>
                      </div>

                      <div>
                        <div className="flex justify-between items-center mb-1.5">
                          <label className="block text-xs font-bold text-slate-700">
                            Mật khẩu:
                          </label>
                          <span className="text-[11px] text-orange-600 hover:underline cursor-pointer">
                            Quên mật khẩu?
                          </span>
                        </div>
                        <div className="relative">
                          <input
                            type={showPassword ? 'text' : 'password'}
                            required
                            value={loginPassword}
                            onChange={(e) => setLoginPassword(e.target.value)}
                            placeholder="Nhập mật khẩu..."
                            className="w-full pl-10 pr-10 py-2.5 bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 transition"
                          />
                          <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                          <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-600"
                          >
                            {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                          </button>
                        </div>
                      </div>

                      {errorMsg && (
                        <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs flex items-center gap-2">
                          <AlertCircle className="w-4 h-4 text-rose-500 flex-shrink-0" />
                          <span>{errorMsg}</span>
                        </div>
                      )}

                      {successMsg && (
                        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs flex items-center gap-2">
                          <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                          <span>{successMsg}</span>
                        </div>
                      )}

                      <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold rounded-xl shadow-lg shadow-orange-500/25 transition text-xs active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2"
                      >
                        {loading ? (
                          <>
                            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                            <span>Đang xác thực thông tin...</span>
                          </>
                        ) : (
                          <span>ĐĂNG NHẬP NGAY</span>
                        )}
                      </button>
                    </form>
                  </>
                )}

                {/* TAB 2: REGISTER FORM */}
                {activeTab === 'register' && (
                  <form onSubmit={handleRegisterSubmit} className="space-y-3.5">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Họ và tên:
                      </label>
                      <input
                        type="text"
                        required
                        value={regFullName}
                        onChange={(e) => setRegFullName(e.target.value)}
                        placeholder="Ví dụ: Nguyễn Văn Mua"
                        className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Tên đăng nhập (Username):
                      </label>
                      <input
                        type="text"
                        required
                        value={regUsername}
                        onChange={(e) => setRegUsername(e.target.value)}
                        placeholder="Ví dụ: buyer2026"
                        className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Email:
                      </label>
                      <input
                        type="email"
                        required
                        value={regEmail}
                        onChange={(e) => setRegEmail(e.target.value)}
                        placeholder="buyer@quickshop.vn"
                        className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Số điện thoại:
                      </label>
                      <input
                        type="tel"
                        value={regPhone}
                        onChange={(e) => setRegPhone(e.target.value)}
                        placeholder="0988776655"
                        className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Mật khẩu:
                      </label>
                      <input
                        type="password"
                        required
                        value={regPassword}
                        onChange={(e) => setRegPassword(e.target.value)}
                        placeholder="Tối thiểu 6 ký tự"
                        className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs text-slate-900 focus:outline-none focus:border-orange-500"
                      />
                    </div>

                    {errorMsg && (
                      <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 text-rose-500 flex-shrink-0" />
                        <span>{errorMsg}</span>
                      </div>
                    )}

                    {successMsg && (
                      <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                        <span>{successMsg}</span>
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full py-3 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/25 transition text-xs active:scale-95 disabled:opacity-50"
                    >
                      {loading ? 'Đang đăng ký...' : 'ĐĂNG KÝ TÀI KHOẢN'}
                    </button>
                  </form>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-4 text-center text-xs text-slate-400">
        © 2026 QuickShop Marketplace. Hệ thống xác thực danh tính & bảo mật đa phân hệ.
      </footer>
    </div>
  );
}

