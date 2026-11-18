import React, { useState } from 'react';
import { 
  X, User, Lock, Key, ShieldCheck, Store, ShoppingBag, Truck, 
  CheckCircle2, AlertCircle, Mail, Phone, Eye, EyeOff, KeyRound, Sparkles
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { generateValidSignedToken } from '../utils/tokenHelper';
import { validateRegisterInput, validateLoginInput } from '../utils/authValidation';
import axiosClient from '../api/axiosClient';
import { AnimatedPresence } from './motion/MotionPrimitives';

export default function AuthModal({ 
  isOpen, 
  onClose, 
  onLoginSuccess,
  onNavigateToRegisterSeller,
  onNavigateToRegisterShipper
}) {
  const { login } = useAuthStore();
  const { syncCartOnLogin } = useCartStore();
  
  const [activeTab, setActiveTab] = useState('login'); // 'login' | 'register'

  // Login form states
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Register form states
  const [regFullName, setRegFullName] = useState('');
  const [regUsername, setRegUsername] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showRegConfirmPassword, setShowRegConfirmPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(true);

  // Common UI states
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const handleTabSwitch = (tab) => {
    setActiveTab(tab);
    setErrorMsg('');
    setSuccessMsg('');
    setFieldErrors({});
  };

  const handleLoginSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setFieldErrors({});

    const loginValidation = validateLoginInput({
      usernameOrEmail: username,
      password: password,
    });

    if (!loginValidation.isValid) {
      setFieldErrors(loginValidation.errors);
      setErrorMsg(loginValidation.firstError);
      return;
    }

    const trimmedUser = username.trim();
    const trimmedPass = password.trim();

    setLoading(true);

    try {
      const res = await axiosClient.post('/api/auth/login', {
        usernameOrEmail: trimmedUser,
        password: trimmedPass,
      });

      if (res && (res.success || res.data)) {
        const data = res.data || {};
        const userId = data.userId ?? data.id ?? 1001;
        const role = data.role || 'ROLE_BUYER';
        login({ ...data, userId, role });
        try {
          await syncCartOnLogin(userId);
        } catch {}
        setSuccessMsg(`Đăng nhập thành công! Chào mừng ${data.fullName || data.username || trimmedUser}`);
        setTimeout(() => {
          onClose();
          if (onLoginSuccess) {
            const r = String(role).toUpperCase();
            if (r.includes('ADMIN')) onLoginSuccess('admin');
            else if (r.includes('SELLER')) onLoginSuccess('seller');
            else if (r.includes('SHIPPER')) onLoginSuccess('shipper');
            else onLoginSuccess('storefront');
          }
        }, 800);
        return;
      }

      setErrorMsg(res?.message || 'Tên đăng nhập hoặc mật khẩu không chính xác.');
    } catch (err) {
      const status = err?.response?.status;
      const serverMessage = err?.response?.data?.message;

      if (status === 400 || status === 401 || status === 403) {
        setErrorMsg(serverMessage || 'Tên đăng nhập hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại!');
        return;
      }
      if (status === 404) {
        setErrorMsg(serverMessage || 'Tài khoản không tồn tại trên hệ thống. Vui lòng kiểm tra lại!');
        return;
      }

      // Local demo fallback khi backend offline và nhập đúng mẫu demo
      const uLower = trimmedUser.toLowerCase();
      const isKnownDemo = (uLower === 'buyer' && trimmedPass === 'buyer123') ||
                          (uLower === 'seller' && trimmedPass === 'seller123') ||
                          (uLower === 'admin' && trimmedPass === 'admin123') ||
                          (uLower === 'shipper' && trimmedPass === 'shipper123');

      if (isKnownDemo) {
        let role = 'ROLE_BUYER';
        let fullName = 'Nguyễn Văn Mua Hàng';
        let demoUserId = 2;
        if (uLower === 'admin') {
          role = 'ROLE_ADMIN';
          fullName = 'Quản Trị Viên QuickShop';
          demoUserId = 99;
        } else if (uLower === 'seller') {
          role = 'ROLE_SELLER';
          fullName = 'Chủ Gian Hàng TechWorld';
          demoUserId = 1;
        } else if (uLower === 'shipper') {
          role = 'ROLE_SHIPPER';
          fullName = 'Tài Xế Giao Hàng Siêu Tốc';
          demoUserId = 5;
        }

        const validSignedToken = generateValidSignedToken({
          userId: demoUserId,
          username: trimmedUser,
          email: `${trimmedUser}@quickshop.vn`,
          fullName,
          role,
        });

        const mockData = {
          userId: demoUserId,
          username: trimmedUser,
          email: `${trimmedUser}@quickshop.vn`,
          fullName,
          role,
          accessToken: validSignedToken,
          refreshToken: validSignedToken,
        };

        login(mockData);
        try {
          await syncCartOnLogin(mockData.userId);
        } catch {}

        setSuccessMsg(`Đăng nhập thành công: ${fullName} [${role}]`);
        setTimeout(() => {
          onClose();
          if (onLoginSuccess) {
            if (role === 'ROLE_ADMIN') onLoginSuccess('admin');
            else if (role === 'ROLE_SELLER') onLoginSuccess('seller');
            else if (role === 'ROLE_SHIPPER') onLoginSuccess('shipper');
            else onLoginSuccess('storefront');
          }
        }, 800);
        return;
      }

      setErrorMsg(serverMessage || 'Lỗi kết nối máy chủ xác thực hoặc thông tin đăng nhập không hợp lệ.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    // Kiểm thực form đăng ký client-side
    const validation = validateRegisterInput({
      fullName: regFullName,
      username: regUsername,
      email: regEmail,
      phoneNumber: regPhone,
      password: regPassword,
      confirmPassword: regConfirmPassword,
      agreeTerms,
    });

    if (!validation.isValid) {
      setFieldErrors(validation.errors);
      setErrorMsg(validation.firstError || 'Vui lòng kiểm tra và hoàn thiện các ô thông tin bị báo đỏ.');
      return;
    }

    setFieldErrors({});
    setLoading(true);

    try {
      const payload = {
        username: regUsername.trim(),
        fullName: regFullName.trim(),
        email: regEmail.trim(),
        phoneNumber: regPhone.trim() || undefined,
        password: regPassword,
      };

      const res = await axiosClient.post('/api/auth/register', payload);

      if (res && (res.success || res.data)) {
        setSuccessMsg('Đăng ký tài khoản thành công! Tài khoản của bạn đã được kích hoạt trực tiếp không cần xác nhận email.');
        
        const registeredUser = regUsername.trim();
        const registeredPass = regPassword;

        // Tự động chuyển sang tab Đăng nhập và điền sẵn thông tin
        setTimeout(() => {
          setActiveTab('login');
          setUsername(registeredUser);
          setPassword(registeredPass);
          setSuccessMsg('Tài khoản đã sẵn sàng! Nhấn "ĐĂNG NHẬP NGAY" để tiếp tục.');
        }, 1500);
        return;
      }

      setErrorMsg(res?.message || 'Đăng ký tài khoản không thành công. Vui lòng thử lại.');
    } catch (err) {
      const serverData = err?.response?.data;
      const serverMsg = serverData?.message;
      const validationMap = serverData?.data;

      if (validationMap && typeof validationMap === 'object' && Object.keys(validationMap).length > 0) {
        setFieldErrors(prev => ({
          ...prev,
          fullName: validationMap.fullName || '',
          username: validationMap.username || '',
          email: validationMap.email || '',
          phoneNumber: validationMap.phoneNumber || '',
          password: validationMap.password || '',
        }));
        const firstErr = Object.values(validationMap)[0];
        setErrorMsg(firstErr || serverMsg || 'Dữ liệu đăng ký không hợp lệ theo quy định máy chủ.');
        return;
      }

      if (serverMsg) {
        const lowerMsg = serverMsg.toLowerCase();
        if (lowerMsg.includes('username')) {
          setFieldErrors(prev => ({ ...prev, username: serverMsg }));
        } else if (lowerMsg.includes('email')) {
          setFieldErrors(prev => ({ ...prev, email: serverMsg }));
        } else if (lowerMsg.includes('số điện thoại') || lowerMsg.includes('phone')) {
          setFieldErrors(prev => ({ ...prev, phoneNumber: serverMsg }));
        }
        setErrorMsg(serverMsg);
        return;
      }

      setErrorMsg('Lỗi kết nối máy chủ xác thực hoặc máy chủ đang bận. Vui lòng thử lại sau!');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AnimatedPresence show={isOpen} mode="modal" dataLenisPrevent={true}>
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
        {/* Header Modal */}
        <div className="p-5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 text-white flex justify-between items-center">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center">
              {activeTab === 'login' ? <User className="w-5 h-5 text-white" /> : <KeyRound className="w-5 h-5 text-white" />}
            </div>
            <div>
              <h3 className="font-bold text-sm">
                {activeTab === 'login' ? 'Đăng nhập QuickShop' : 'Đăng ký tài khoản mới'}
              </h3>
              <p className="text-[11px] text-white/80">Hệ thống thương mại điện tử đa phân hệ</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-white/20 transition cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Toggle: Đăng nhập / Đăng ký */}
        <div className="grid grid-cols-2 border-b border-slate-100 text-center text-xs font-bold">
          <button
            type="button"
            onClick={() => handleTabSwitch('login')}
            className={`py-3 transition flex items-center justify-center gap-1.5 ${
              activeTab === 'login'
                ? 'text-orange-600 border-b-2 border-orange-600 bg-orange-50/40'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <User className="w-3.5 h-3.5" />
            <span>Đăng Nhập</span>
          </button>

          <button
            type="button"
            onClick={() => handleTabSwitch('register')}
            className={`py-3 transition flex items-center justify-center gap-1.5 ${
              activeTab === 'register'
                ? 'text-orange-600 border-b-2 border-orange-600 bg-orange-50/40'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <KeyRound className="w-3.5 h-3.5" />
            <span>Đăng Ký Mới</span>
          </button>
        </div>

        <div className="p-5 sm:p-6 space-y-4 text-xs max-h-[80vh] overflow-y-auto" data-lenis-prevent>
          {/* TAB 1: FORM ĐĂNG NHẬP */}
          {activeTab === 'login' && (
            <form onSubmit={handleLoginSubmit} className="space-y-3.5" noValidate>
              <div>
                <label htmlFor="auth-modal-login-username" className="font-bold text-slate-700 block mb-1">
                  Tên đăng nhập hoặc Email: <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-login-username"
                    type="text"
                    value={username}
                    onChange={(e) => { 
                      setUsername(e.target.value); 
                      setFieldErrors(prev => ({ ...prev, usernameOrEmail: '' }));
                      setErrorMsg(''); 
                    }}
                    placeholder="Nhập tên đăng nhập hoặc email..."
                    className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.usernameOrEmail
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <User className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.usernameOrEmail ? 'text-red-500' : 'text-slate-400'}`} />
                </div>
                {fieldErrors.usernameOrEmail && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.usernameOrEmail}</span>
                  </div>
                )}
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label htmlFor="auth-modal-login-password" className="font-bold text-slate-700">
                    Mật khẩu: <span className="text-red-500">*</span>
                  </label>
                </div>
                <div className="relative">
                  <input
                    id="auth-modal-login-password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => { 
                      setPassword(e.target.value); 
                      setFieldErrors(prev => ({ ...prev, password: '' }));
                      setErrorMsg(''); 
                    }}
                    placeholder="Nhập mật khẩu..."
                    className={`w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.password
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <Lock className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.password ? 'text-red-500' : 'text-slate-400'}`} />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-600 transition"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {fieldErrors.password && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.password}</span>
                  </div>
                )}
              </div>

              {/* THÔNG BÁO LỖI ĐỎ NỔI BẬT */}
              {errorMsg && (
                <div className="p-3 bg-red-50 border-2 border-red-500 text-red-900 rounded-xl text-xs flex items-start gap-2 shadow-sm animate-in fade-in">
                  <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <div className="font-bold text-red-900">Thông báo lỗi:</div>
                    <div className="text-red-700 mt-0.5 font-medium leading-relaxed">{errorMsg}</div>
                  </div>
                </div>
              )}

              {successMsg && (
                <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-[11px] flex items-center gap-1.5 animate-in fade-in">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>{successMsg}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="btn-shimmer btn-primary-cta w-full py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/30 transition text-xs active:scale-95 disabled:opacity-50 cursor-pointer"
              >
                {loading ? 'Đang xác thực thông tin...' : 'ĐĂNG NHẬP NGAY'}
              </button>
            </form>
          )}

          {/* TAB 2: FORM ĐĂNG KÝ MỚI */}
          {activeTab === 'register' && (
            <form onSubmit={handleRegisterSubmit} className="space-y-3" noValidate>
              {/* Banner kích hoạt ngay không cần mail */}
              <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-xl text-[11px] flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>Kích hoạt tài khoản trực tiếp, <strong>không cần xác nhận email</strong>.</span>
              </div>

              {/* Họ và tên */}
              <div>
                <label htmlFor="auth-modal-reg-fullname" className="font-bold text-slate-700 block mb-1">
                  Họ và tên: <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-reg-fullname"
                    type="text"
                    value={regFullName}
                    onChange={(e) => {
                      setRegFullName(e.target.value);
                      setFieldErrors(prev => ({ ...prev, fullName: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="Ví dụ: Nguyễn Văn Mua"
                    className={`w-full pl-9 pr-3.5 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.fullName
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <User className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.fullName ? 'text-red-500' : 'text-slate-400'}`} />
                </div>
                {fieldErrors.fullName && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.fullName}</span>
                  </div>
                )}
              </div>

              {/* Tên đăng nhập */}
              <div>
                <label htmlFor="auth-modal-reg-username" className="font-bold text-slate-700 block mb-1">
                  Tên đăng nhập (Username): <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-reg-username"
                    type="text"
                    value={regUsername}
                    onChange={(e) => {
                      setRegUsername(e.target.value);
                      setFieldErrors(prev => ({ ...prev, username: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="Từ 3-30 ký tự, viết liền không dấu..."
                    className={`w-full pl-9 pr-3.5 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.username
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <User className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.username ? 'text-red-500' : 'text-slate-400'}`} />
                </div>
                {fieldErrors.username && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.username}</span>
                  </div>
                )}
              </div>

              {/* Email */}
              <div>
                <label htmlFor="auth-modal-reg-email" className="font-bold text-slate-700 block mb-1">
                  Email: <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-reg-email"
                    type="email"
                    value={regEmail}
                    onChange={(e) => {
                      setRegEmail(e.target.value);
                      setFieldErrors(prev => ({ ...prev, email: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="buyer@example.com"
                    className={`w-full pl-9 pr-3.5 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.email
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <Mail className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.email ? 'text-red-500' : 'text-slate-400'}`} />
                </div>
                {fieldErrors.email && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.email}</span>
                  </div>
                )}
              </div>

              {/* Số điện thoại (tùy chọn) */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label htmlFor="auth-modal-reg-phone" className="font-bold text-slate-700">Số điện thoại:</label>
                  <span className="text-[10px] text-slate-400">(Tùy chọn)</span>
                </div>
                <div className="relative">
                  <input
                    id="auth-modal-reg-phone"
                    type="tel"
                    value={regPhone}
                    onChange={(e) => {
                      setRegPhone(e.target.value);
                      setFieldErrors(prev => ({ ...prev, phoneNumber: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="0912345678"
                    className={`w-full pl-9 pr-3.5 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.phoneNumber
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <Phone className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.phoneNumber ? 'text-red-500' : 'text-slate-400'}`} />
                </div>
                {fieldErrors.phoneNumber && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.phoneNumber}</span>
                  </div>
                )}
              </div>

              {/* Mật khẩu */}
              <div>
                <label htmlFor="auth-modal-reg-password" className="font-bold text-slate-700 block mb-1">
                  Mật khẩu: <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-reg-password"
                    type={showRegPassword ? 'text' : 'password'}
                    value={regPassword}
                    onChange={(e) => {
                      setRegPassword(e.target.value);
                      setFieldErrors(prev => ({ ...prev, password: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="Tối thiểu 6 ký tự..."
                    className={`w-full pl-9 pr-9 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.password
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <Lock className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.password ? 'text-red-500' : 'text-slate-400'}`} />
                  <button
                    type="button"
                    onClick={() => setShowRegPassword(!showRegPassword)}
                    className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 transition"
                  >
                    {showRegPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                  </button>
                </div>
                {fieldErrors.password && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.password}</span>
                  </div>
                )}
              </div>

              {/* Xác nhận mật khẩu */}
              <div>
                <label htmlFor="auth-modal-reg-confirm-password" className="font-bold text-slate-700 block mb-1">
                  Xác nhận mật khẩu: <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    id="auth-modal-reg-confirm-password"
                    type={showRegConfirmPassword ? 'text' : 'password'}
                    value={regConfirmPassword}
                    onChange={(e) => {
                      setRegConfirmPassword(e.target.value);
                      setFieldErrors(prev => ({ ...prev, confirmPassword: '' }));
                      setErrorMsg('');
                    }}
                    placeholder="Nhập lại mật khẩu vừa tạo..."
                    className={`w-full pl-9 pr-9 py-2 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                      fieldErrors.confirmPassword
                        ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                        : 'bg-slate-50 border border-slate-300 focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
                    }`}
                  />
                  <ShieldCheck className={`w-3.5 h-3.5 absolute left-3 top-2.5 ${fieldErrors.confirmPassword ? 'text-red-500' : 'text-slate-400'}`} />
                  <button
                    type="button"
                    onClick={() => setShowRegConfirmPassword(!showRegConfirmPassword)}
                    className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600 transition"
                  >
                    {showRegConfirmPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                  </button>
                </div>
                {fieldErrors.confirmPassword && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.confirmPassword}</span>
                  </div>
                )}
              </div>

              {/* Checkbox điều khoản */}
              <div className="pt-0.5">
                <label className="flex items-start gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(e) => {
                      setAgreeTerms(e.target.checked);
                      setFieldErrors(prev => ({ ...prev, agreeTerms: '' }));
                      setErrorMsg('');
                    }}
                    className="mt-0.5 rounded border-slate-300 text-orange-600 focus:ring-orange-500 cursor-pointer"
                  />
                  <span className="text-[11px] text-slate-600 leading-snug">
                    Tôi đồng ý với <span className="text-orange-600 font-semibold hover:underline">Điều khoản dịch vụ</span> & <span className="text-orange-600 font-semibold hover:underline">Chính sách bảo mật</span> của QuickShop.
                  </span>
                </label>
                {fieldErrors.agreeTerms && (
                  <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                    <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                    <span>{fieldErrors.agreeTerms}</span>
                  </div>
                )}
              </div>

              {/* THÔNG BÁO LỖI ĐỎ NỔI BẬT */}
              {errorMsg && (
                <div className="p-3 bg-red-50 border-2 border-red-500 text-red-900 rounded-xl text-xs flex items-start gap-2 shadow-sm animate-in fade-in">
                  <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <div className="font-bold text-red-900">Lỗi đăng ký:</div>
                    <div className="text-red-700 mt-0.5 font-medium leading-relaxed">{errorMsg}</div>
                  </div>
                </div>
              )}

              {/* THÔNG BÁO THÀNH CÔNG */}
              {successMsg && (
                <div className="p-2.5 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-xl text-[11px] flex items-center gap-1.5 animate-in fade-in">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                  <span className="font-medium">{successMsg}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="btn-shimmer btn-primary-cta w-full py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/30 transition text-xs active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
              >
                {loading ? (
                  <>
                    <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang đăng ký tài khoản...</span>
                  </>
                ) : (
                  <span>ĐĂNG KÝ TÀI KHOẢN NGAY</span>
                )}
              </button>
            </form>
          )}

          {/* Partner Onboarding Links */}
          <div className="pt-3 border-t border-slate-100 space-y-2">
            <div className="text-center text-[10px] font-bold text-slate-400 uppercase tracking-wider">
              Hợp tác cùng QuickShop
            </div>
            <div className="grid grid-cols-2 gap-2 text-center text-xs">
              <button
                type="button"
                onClick={() => {
                  onClose();
                  if (onNavigateToRegisterSeller) onNavigateToRegisterSeller();
                }}
                className="p-2 rounded-xl border border-slate-200 hover:border-orange-500 hover:bg-orange-50/50 text-slate-700 hover:text-orange-600 transition flex items-center justify-center gap-1.5 font-semibold cursor-pointer"
              >
                <Store className="w-3.5 h-3.5 text-orange-600" />
                <span>Đăng ký Bán hàng</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  onClose();
                  if (onNavigateToRegisterShipper) onNavigateToRegisterShipper();
                }}
                className="p-2 rounded-xl border border-slate-200 hover:border-blue-500 hover:bg-blue-50/50 text-slate-700 hover:text-blue-600 transition flex items-center justify-center gap-1.5 font-semibold cursor-pointer"
              >
                <Truck className="w-3.5 h-3.5 text-blue-600" />
                <span>Đăng ký Shipper</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </AnimatedPresence>
  );
}
