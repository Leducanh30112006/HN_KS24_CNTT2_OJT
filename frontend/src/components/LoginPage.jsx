import React, { useState } from 'react';
import { 
  ShoppingBag, Store, ShieldCheck, Truck, User, Lock, Mail, Phone, 
  ArrowLeft, CheckCircle2, AlertCircle, Eye, EyeOff, Sparkles, KeyRound,
  ChevronRight, ArrowRight, Check
} from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { generateValidSignedToken } from '../utils/tokenHelper';
import { validateRegisterInput, validateLoginInput } from '../utils/authValidation';
import axiosClient from '../api/axiosClient';

export default function LoginPage({ 
  onBackToHome, 
  onLoginSuccess,
  onNavigateToRegisterSeller,
  onNavigateToRegisterShipper
}) {
  const { login } = useAuthStore();
  const { syncCartOnLogin } = useCartStore();
  
  const [activeTab, setActiveTab] = useState('login'); // 'login' | 'register'
  
  // Login form states
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Register form states & validations
  const [regUsername, setRegUsername] = useState('');
  const [regFullName, setRegFullName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPhone, setRegPhone] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regConfirmPassword, setRegConfirmPassword] = useState('');
  const [showRegPassword, setShowRegPassword] = useState(false);
  const [showRegConfirmPassword, setShowRegConfirmPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(true);
  const [fieldErrors, setFieldErrors] = useState({});

  const handleLoginSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');
    setFieldErrors({});

    const loginValidation = validateLoginInput({
      usernameOrEmail: loginUsername,
      password: loginPassword,
    });

    if (!loginValidation.isValid) {
      setFieldErrors(loginValidation.errors);
      setErrorMsg(loginValidation.firstError);
      return;
    }

    const trimmedUser = loginUsername.trim();
    const trimmedPass = loginPassword.trim();

    if (!trimmedUser) {
      setErrorMsg('Vui lòng nhập tên đăng nhập hoặc địa chỉ email.');
      return;
    }
    if (!trimmedPass) {
      setErrorMsg('Vui lòng nhập mật khẩu đăng nhập.');
      return;
    }

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

        // Tự động đồng bộ / hợp nhất giỏ hàng sau khi xác thực thành công
        try {
          const syncResult = await syncCartOnLogin(userId, { dedupeRecent: true });
          if (syncResult?.warnings?.length > 0) {
            console.info('Cảnh báo tồn kho khi đồng bộ giỏ hàng:', syncResult.warnings);
          }
        } catch (syncErr) {
          console.warn('Lỗi khi đồng bộ giỏ hàng:', syncErr);
        }

        setSuccessMsg(`Đăng nhập thành công! Chào mừng ${data.fullName || data.username || trimmedUser}`);
        
        setTimeout(() => {
          if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
            onLoginSuccess('admin');
          } else if (role === 'ROLE_SELLER' || role === 'SELLER') {
            onLoginSuccess('seller');
          } else if (role === 'ROLE_SHIPPER' || role === 'SHIPPER') {
            onLoginSuccess('shipper');
          } else {
            onLoginSuccess('storefront');
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
        setErrorMsg(serverMessage || 'Tài khoản không tồn tại trên hệ thống. Vui lòng kiểm tra lại hoặc Đăng ký mới!');
        return;
      }

      // Fallback demo duy nhất khi backend hoàn toàn offline và tài khoản nhập chuẩn theo mẫu demo
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
          if (role !== 'ROLE_SHIPPER') {
            await syncCartOnLogin(mockData.userId, { dedupeRecent: true });
          }
        } catch (e) {}

        setSuccessMsg(`Đăng nhập thành công: ${fullName} [${role}]`);
        
        setTimeout(() => {
          if (role === 'ROLE_ADMIN') {
            onLoginSuccess('admin');
          } else if (role === 'ROLE_SELLER') {
            onLoginSuccess('seller');
          } else if (role === 'ROLE_SHIPPER') {
            onLoginSuccess('shipper');
          } else {
            onLoginSuccess('storefront');
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

    // 1. Kiểm thực toàn bộ form Đăng ký bằng bộ rules chuẩn
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
      setErrorMsg(validation.firstError || 'Vui lòng kiểm tra và sửa các thông tin chưa hợp lệ (báo đỏ bên dưới).');
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
        setSuccessMsg('Đăng ký tài khoản thành công! Tài khoản của bạn đã được kích hoạt trực tiếp, không cần xác nhận qua email.');
        
        const registeredUser = regUsername.trim();
        const registeredPass = regPassword;

        // Tự động chuyển về Tab Đăng nhập và điền sẵn thông tin
        setTimeout(() => {
          setActiveTab('login');
          setLoginUsername(registeredUser);
          setLoginPassword(registeredPass);
          setSuccessMsg('Tài khoản đã sẵn sàng! Nhấn "ĐĂNG NHẬP NGAY" để bắt đầu trải nghiệm mua sắm.');
        }, 1500);
        return;
      }

      setErrorMsg(res?.message || 'Đăng ký tài khoản không thành công. Vui lòng thử lại.');
    } catch (err) {
      const serverData = err?.response?.data;
      const serverMsg = serverData?.message;
      const validationMap = serverData?.data; // Map field errors từ Spring Boot Validation

      // Xử lý lỗi validate chi tiết theo từng ô input từ server
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

      // Xử lý các lỗi nghiệp vụ trùng lặp username / email
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
              Trải nghiệm mua sắm <span className="text-orange-600 font-extrabold">thông minh & an toàn</span> cùng QuickShop
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
                    setFieldErrors({});
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
                    setFieldErrors({});
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
                  <form onSubmit={handleLoginSubmit} className="space-y-4 pt-1">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1.5">
                        Tên đăng nhập hoặc Email: <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type="text"
                          value={loginUsername}
                          onChange={(e) => { 
                            setLoginUsername(e.target.value); 
                            setFieldErrors(prev => ({ ...prev, usernameOrEmail: '' }));
                            setErrorMsg(''); 
                          }}
                          placeholder="Nhập username hoặc email..."
                          className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.usernameOrEmail
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
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
                      <div className="flex justify-between items-center mb-1.5">
                        <label className="block text-xs font-bold text-slate-700">
                          Mật khẩu: <span className="text-red-500">*</span>
                        </label>
                        <span className="text-[11px] text-orange-600 hover:underline cursor-pointer">
                          Quên mật khẩu?
                        </span>
                      </div>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={loginPassword}
                          onChange={(e) => { 
                            setLoginPassword(e.target.value); 
                            setFieldErrors(prev => ({ ...prev, password: '' }));
                            setErrorMsg(''); 
                          }}
                          placeholder="Nhập mật khẩu..."
                          className={`w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.password
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
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
                      <div className="p-3.5 bg-red-50 border-2 border-red-500 text-red-900 rounded-2xl text-xs flex items-start gap-2.5 shadow-sm animate-in fade-in">
                        <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <div className="font-bold text-red-900">Đăng nhập không thành công:</div>
                          <div className="mt-0.5 text-red-700 leading-relaxed font-medium">{errorMsg}</div>
                        </div>
                      </div>
                    )}

                    {successMsg && (
                      <div className="p-3.5 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-2xl text-xs flex items-start gap-2.5 shadow-sm animate-in fade-in">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <div className="font-bold text-emerald-900">Thành công:</div>
                          <div className="mt-0.5 text-emerald-700 leading-relaxed font-medium">{successMsg}</div>
                        </div>
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full py-3 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 hover:from-orange-600 hover:to-rose-600 text-white font-bold rounded-xl shadow-lg shadow-orange-500/25 transition text-xs active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
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
                )}

                {/* TAB 2: REGISTER FORM - ĐẦY ĐỦ VALIDATE & THÔNG BÁO ĐỎ & BỎ XÁC NHẬN MAIL */}
                {activeTab === 'register' && (
                  <form onSubmit={handleRegisterSubmit} className="space-y-3.5 pt-1" noValidate>
                    {/* Banner thông báo trực quan: Đăng ký kích hoạt ngay, bỏ xác nhận email */}
                    <div className="p-3 bg-emerald-50/90 border border-emerald-200 text-emerald-900 rounded-xl text-xs flex items-center gap-2.5">
                      <ShieldCheck className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      <div className="text-[11px] leading-relaxed">
                        Tài khoản được <strong>kích hoạt trực tiếp</strong> ngay sau khi đăng ký, không cần chờ xác thực qua email.
                      </div>
                    </div>

                    {/* 1. Họ và tên */}
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Họ và tên: <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type="text"
                          value={regFullName}
                          onChange={(e) => {
                            setRegFullName(e.target.value);
                            setFieldErrors(prev => ({ ...prev, fullName: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="Ví dụ: Nguyễn Văn Mua"
                          className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.fullName
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <User className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.fullName ? 'text-red-500' : 'text-slate-400'}`} />
                      </div>
                      {fieldErrors.fullName && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.fullName}</span>
                        </div>
                      )}
                    </div>

                    {/* 2. Tên đăng nhập */}
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Tên đăng nhập (Username): <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type="text"
                          value={regUsername}
                          onChange={(e) => {
                            setRegUsername(e.target.value);
                            setFieldErrors(prev => ({ ...prev, username: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="Từ 3-30 ký tự, viết liền không dấu..."
                          className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.username
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <User className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.username ? 'text-red-500' : 'text-slate-400'}`} />
                      </div>
                      {fieldErrors.username && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.username}</span>
                        </div>
                      )}
                    </div>

                    {/* 3. Email */}
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Email: <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type="email"
                          value={regEmail}
                          onChange={(e) => {
                            setRegEmail(e.target.value);
                            setFieldErrors(prev => ({ ...prev, email: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="buyer@example.com"
                          className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.email
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <Mail className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.email ? 'text-red-500' : 'text-slate-400'}`} />
                      </div>
                      {fieldErrors.email && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.email}</span>
                        </div>
                      )}
                    </div>

                    {/* 4. Số điện thoại (tùy chọn) */}
                    <div>
                      <div className="flex justify-between items-center mb-1">
                        <label className="block text-xs font-bold text-slate-700">
                          Số điện thoại:
                        </label>
                        <span className="text-[10px] text-slate-400">(Tùy chọn)</span>
                      </div>
                      <div className="relative">
                        <input
                          type="tel"
                          value={regPhone}
                          onChange={(e) => {
                            setRegPhone(e.target.value);
                            setFieldErrors(prev => ({ ...prev, phoneNumber: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="0912345678"
                          className={`w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.phoneNumber
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <Phone className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.phoneNumber ? 'text-red-500' : 'text-slate-400'}`} />
                      </div>
                      {fieldErrors.phoneNumber && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.phoneNumber}</span>
                        </div>
                      )}
                    </div>

                    {/* 5. Mật khẩu */}
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Mật khẩu: <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type={showRegPassword ? 'text' : 'password'}
                          value={regPassword}
                          onChange={(e) => {
                            setRegPassword(e.target.value);
                            setFieldErrors(prev => ({ ...prev, password: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="Tối thiểu 6 ký tự..."
                          className={`w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.password
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <Lock className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.password ? 'text-red-500' : 'text-slate-400'}`} />
                        <button
                          type="button"
                          onClick={() => setShowRegPassword(!showRegPassword)}
                          className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-600 transition"
                        >
                          {showRegPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                      {fieldErrors.password && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.password}</span>
                        </div>
                      )}
                    </div>

                    {/* 6. Xác nhận mật khẩu */}
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Xác nhận mật khẩu: <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <input
                          type={showRegConfirmPassword ? 'text' : 'password'}
                          value={regConfirmPassword}
                          onChange={(e) => {
                            setRegConfirmPassword(e.target.value);
                            setFieldErrors(prev => ({ ...prev, confirmPassword: '' }));
                            setErrorMsg('');
                          }}
                          placeholder="Nhập lại mật khẩu vừa tạo..."
                          className={`w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 transition focus:outline-none ${
                            fieldErrors.confirmPassword
                              ? 'bg-red-50/30 border-2 border-red-500 text-red-900 focus:border-red-600 focus:ring-4 focus:ring-red-500/10'
                              : 'bg-slate-50 hover:bg-white focus:bg-white border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10'
                          }`}
                        />
                        <ShieldCheck className={`w-4 h-4 absolute left-3.5 top-3 ${fieldErrors.confirmPassword ? 'text-red-500' : 'text-slate-400'}`} />
                        <button
                          type="button"
                          onClick={() => setShowRegConfirmPassword(!showRegConfirmPassword)}
                          className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-600 transition"
                        >
                          {showRegConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                      {fieldErrors.confirmPassword && (
                        <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-red-600 animate-in fade-in">
                          <AlertCircle className="w-3.5 h-3.5 text-red-500 flex-shrink-0" />
                          <span>{fieldErrors.confirmPassword}</span>
                        </div>
                      )}
                    </div>

                    {/* 7. Đồng ý điều khoản */}
                    <div className="pt-1">
                      <label className="flex items-start gap-2.5 cursor-pointer">
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
                        <span className="text-[11px] text-slate-600 leading-relaxed">
                          Tôi đồng ý với{' '}
                          <span className="text-orange-600 font-semibold hover:underline">
                            Điều khoản dịch vụ
                          </span>{' '}
                          &{' '}
                          <span className="text-orange-600 font-semibold hover:underline">
                            Chính sách bảo mật
                          </span>{' '}
                          của sàn QuickShop.
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
                      <div className="p-3.5 bg-red-50 border-2 border-red-500 text-red-900 rounded-2xl text-xs flex items-start gap-2.5 shadow-sm animate-in fade-in">
                        <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <div className="font-bold text-red-900">Lỗi đăng ký / Thông tin chưa hợp lệ:</div>
                          <div className="mt-0.5 text-red-700 leading-relaxed font-medium">{errorMsg}</div>
                        </div>
                      </div>
                    )}

                    {/* THÔNG BÁO THÀNH CÔNG XANH LÁ */}
                    {successMsg && (
                      <div className="p-3.5 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-2xl text-xs flex items-start gap-2.5 shadow-sm animate-in fade-in">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <div className="font-bold text-emerald-900">Đăng ký thành công!</div>
                          <div className="mt-0.5 text-emerald-700 leading-relaxed font-medium">{successMsg}</div>
                        </div>
                      </div>
                    )}

                    <button
                      type="submit"
                      disabled={loading}
                      className="btn-shimmer btn-primary-cta w-full py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/25 transition text-xs active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
                    >
                      {loading ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                          <span>Đang đăng ký tài khoản...</span>
                        </>
                      ) : (
                        <span>ĐĂNG KÝ TÀI KHOẢN NGAY</span>
                      )}
                    </button>
                  </form>
                )}

                {/* PARTNER REGISTRATION ONBOARDING SECTION */}
                <div className="pt-5 border-t border-slate-200/90 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5 text-orange-500" />
                      Hợp tác cùng QuickShop
                    </span>
                    <span className="text-[10px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                      0đ Phí mở
                    </span>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                    {/* Bán hàng */}
                    <div
                      role="button"
                      tabIndex={0}
                      onClick={onNavigateToRegisterSeller}
                      className="group p-3 rounded-2xl border border-slate-200/90 hover:border-orange-500 hover:bg-orange-50/40 transition-all cursor-pointer flex items-center gap-3 bg-white shadow-xs select-none"
                    >
                      <div className="w-9 h-9 rounded-xl bg-orange-100 flex items-center justify-center text-orange-600 group-hover:scale-105 group-hover:bg-orange-500 group-hover:text-white transition flex-shrink-0">
                        <Store className="w-4 h-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <h4 className="text-xs font-bold text-slate-900 group-hover:text-orange-600 transition truncate">
                            Đối tác Bán hàng
                          </h4>
                          <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:translate-x-0.5 group-hover:text-orange-600 transition flex-shrink-0" />
                        </div>
                        <p className="text-[10px] text-slate-500 truncate mt-0.5">
                          Mở shop, tiếp cận 5M+ khách
                        </p>
                      </div>
                    </div>

                    {/* Vận chuyển */}
                    <div
                      role="button"
                      tabIndex={0}
                      onClick={onNavigateToRegisterShipper}
                      className="group p-3 rounded-2xl border border-slate-200/90 hover:border-blue-500 hover:bg-blue-50/40 transition-all cursor-pointer flex items-center gap-3 bg-white shadow-xs select-none"
                    >
                      <div className="w-9 h-9 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600 group-hover:scale-105 group-hover:bg-blue-600 group-hover:text-white transition flex-shrink-0">
                        <Truck className="w-4 h-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <h4 className="text-xs font-bold text-slate-900 group-hover:text-blue-600 transition truncate">
                            Đối tác Vận chuyển
                          </h4>
                          <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:translate-x-0.5 group-hover:text-blue-600 transition flex-shrink-0" />
                        </div>
                        <p className="text-[10px] text-slate-500 truncate mt-0.5">
                          Tài xế giao hàng, 15-20tr/tháng
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
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

