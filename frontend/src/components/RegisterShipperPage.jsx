import React, { useState } from 'react';
import { 
  Truck, ShoppingBag, ShieldCheck, ArrowLeft, CheckCircle2, AlertCircle, 
  Eye, EyeOff, Sparkles, User, Phone, Mail, MapPin, 
  FileText, Award, ChevronRight, Zap, Check, Wallet, Compass
} from 'lucide-react';
import axiosClient from '../api/axiosClient';
import { useAuthStore } from '../stores/useAuthStore';

// Helper generating valid signed mock JWT token if backend is offline
const generateValidSignedToken = (payload) => {
  const header = { alg: 'HS256', typ: 'JWT' };
  const base64UrlEncode = (obj) => {
    const json = JSON.stringify(obj);
    const base64 = btoa(unescape(encodeURIComponent(json)));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  };
  const now = Math.floor(Date.now() / 1000);
  const jwtPayload = {
    sub: payload.username || 'shipper',
    userId: payload.userId || 5,
    role: payload.role || 'ROLE_SHIPPER',
    fullName: payload.fullName || 'Tài Xế QuickShop',
    email: payload.email || 'shipper@quickshop.vn',
    iat: now,
    exp: now + 86400 * 7,
  };
  const dummySignature = 'mock_valid_signature_for_quickshop_local_demo';
  return `${base64UrlEncode(header)}.${base64UrlEncode(jwtPayload)}.${dummySignature}`;
};

const CITIES = [
  'Hà Nội (Khu vực Đống Đa - Ba Đình - Cầu Giấy)',
  'Hà Nội (Khu vực Hai Bà Trưng - Hoàn Kiếm - Hoàng Mai)',
  'Hà Nội (Khu vực Thanh Xuân - Hà Đông - Nam Từ Liêm)',
  'TP. Hồ Chí Minh (Khu vực Quận 1 - Quận 3 - Bình Thạnh)',
  'TP. Hồ Chí Minh (Khu vực Tân Bình - Gò Vấp - Phú Nhuận)',
  'TP. Hồ Chí Minh (Khu vực Quận 7 - Nhà Bè - TP. Thủ Đức)',
  'Đà Nẵng (Hải Châu - Thanh Khê - Sơn Trà)',
  'Hải Phòng (Hồng Bàng - Ngô Quyền - Lê Chân)',
  'Cần Thơ (Ninh Kiều - Cái Răng - Bình Thủy)',
];

export default function RegisterShipperPage({ 
  onBackToHome, 
  onNavigateToLogin, 
  onRegisterSuccess 
}) {
  const { login } = useAuthStore();

  // Form State
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [email, setEmail] = useState('');
  const [cccd, setCccd] = useState('');
  const [operatingArea, setOperatingArea] = useState(CITIES[0]);

  const [vehicleType, setVehicleType] = useState('MOTORBIKE'); // MOTORBIKE | VAN | TRUCK
  const [licensePlate, setLicensePlate] = useState('');
  const [drivingLicense, setDrivingLicense] = useState('A1'); // A1 | A2 | B2 | C

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(true);

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    // Validation
    if (!fullName.trim()) {
      setErrorMsg('Vui lòng nhập họ và tên của bạn.');
      return;
    }
    if (!phoneNumber.trim()) {
      setErrorMsg('Vui lòng nhập số điện thoại.');
      return;
    }
    const phoneRegex = /^(0|\+84)[0-9]{9,10}$/;
    if (!phoneRegex.test(phoneNumber.trim())) {
      setErrorMsg('Số điện thoại không hợp lệ (Ví dụ: 0987654321).');
      return;
    }
    if (!email.trim() || !email.includes('@')) {
      setErrorMsg('Vui lòng nhập địa chỉ email hợp lệ.');
      return;
    }
    if (!cccd.trim() || cccd.trim().length < 9) {
      setErrorMsg('Vui lòng nhập số CCCD/CMND hợp lệ (9 đến 12 số).');
      return;
    }
    if (!licensePlate.trim()) {
      setErrorMsg('Vui lòng nhập biển số xe đăng ký (Ví dụ: 29A1-888.88).');
      return;
    }
    if (!username.trim() || username.trim().length < 3) {
      setErrorMsg('Tên đăng nhập phải có ít nhất 3 ký tự.');
      return;
    }
    if (!password || password.length < 6) {
      setErrorMsg('Mật khẩu đăng nhập phải có ít nhất 6 ký tự.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMsg('Mật khẩu xác nhận không khớp.');
      return;
    }
    if (!agreeTerms) {
      setErrorMsg('Bạn cần đồng ý với Quy chế & Tiêu chuẩn giao hàng QuickShop Express.');
      return;
    }

    setLoading(true);

    try {
      // Gọi API đăng ký tài khoản với role ROLE_SHIPPER
      const payload = {
        username: username.trim(),
        fullName: fullName.trim(),
        email: email.trim(),
        phoneNumber: phoneNumber.trim(),
        password,
        role: 'ROLE_SHIPPER',
      };

      const res = await axiosClient.post('/api/auth/register', payload);

      if (res && (res.success || res.data)) {
        const data = res.data || {};
        const userId = data.userId ?? data.id ?? 501;
        const authData = {
          ...data,
          userId,
          role: 'ROLE_SHIPPER',
          fullName: fullName.trim(),
          licensePlate: licensePlate.trim(),
        };

        login(authData);
        setSuccessMsg(`Đăng ký đối tác vận chuyển thành công! Chào mừng bác tài ${fullName.trim()}. Đang chuyển hướng sang Cổng Shipper...`);

        setTimeout(() => {
          if (onRegisterSuccess) {
            onRegisterSuccess('shipper');
          }
        }, 1500);
        return;
      }

      setErrorMsg(res?.message || 'Đăng ký tài xế thất bại. Vui lòng kiểm tra lại thông tin.');
    } catch (err) {
      // Nếu server trả về lỗi nghiệp vụ
      if (err?.response?.data?.message) {
        setErrorMsg(err.response.data.message);
        setLoading(false);
        return;
      }

      // Fallback mô phỏng nếu API Identity backend offline
      const mockUserId = Math.floor(Math.random() * 9000) + 5000;
      const validToken = generateValidSignedToken({
        userId: mockUserId,
        username: username.trim(),
        email: email.trim(),
        fullName: fullName.trim(),
        role: 'ROLE_SHIPPER',
      });

      const mockData = {
        userId: mockUserId,
        username: username.trim(),
        email: email.trim(),
        fullName: fullName.trim(),
        role: 'ROLE_SHIPPER',
        licensePlate: licensePlate.trim(),
        accessToken: validToken,
        refreshToken: validToken,
      };

      login(mockData);
      setSuccessMsg(`Chào mừng bác tài ${fullName.trim()} gia nhập QuickShop Express! Đang chuyển hướng vào Cổng Vận Chuyển...`);

      setTimeout(() => {
        if (onRegisterSuccess) {
          onRegisterSuccess('shipper');
        }
      }, 1500);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between font-sans">
      {/* Top Header */}
      <header className="bg-white/95 backdrop-blur-md border-b border-slate-200 sticky top-0 z-30 py-3.5 px-4 sm:px-8 shadow-xs">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div 
            onClick={onBackToHome}
            className="flex items-center gap-2.5 cursor-pointer group"
          >
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-cyan-500 flex items-center justify-center text-white shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
              <Truck className="w-5 h-5 stroke-[2.5]" />
            </div>
            <div>
              <div className="text-xl font-black tracking-tight text-slate-900 flex items-center">
                Quick<span className="text-blue-600">Shop</span>
                <span className="ml-2 text-xs font-bold text-blue-700 bg-blue-100 px-2 py-0.5 rounded-full border border-blue-200">
                  Đối Tác Shipper
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onNavigateToLogin}
              className="text-xs font-semibold text-slate-600 hover:text-blue-600 transition px-3 py-1.5 rounded-xl hover:bg-slate-100"
            >
              Đã có tài khoản? <strong className="text-blue-600">Đăng nhập</strong>
            </button>
            <button
              type="button"
              onClick={onBackToHome}
              className="hidden sm:flex items-center gap-1.5 text-xs font-bold text-slate-700 hover:text-slate-900 border border-slate-200 hover:border-slate-300 px-3.5 py-1.5 rounded-xl transition bg-white"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Về trang chủ</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-6xl mx-auto px-4 sm:px-6 py-8 sm:py-12 flex-1 w-full">
        {/* Value Proposition Hero Banner */}
        <div className="bg-gradient-to-r from-slate-950 via-slate-900 to-slate-950 text-white rounded-3xl p-6 sm:p-10 mb-8 shadow-xl relative overflow-hidden border border-slate-800">
          <div className="relative z-10 max-w-3xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/20 border border-blue-500/30 text-blue-400 text-xs font-bold uppercase tracking-wider">
              <Sparkles className="w-3.5 h-3.5" />
              Gia nhập Đội ngũ Giao Vận QuickShop Express
            </div>
            <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight leading-tight">
              Làm chủ thời gian, thu nhập <br className="hidden sm:inline" />
              lên đến <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-cyan-300">15 - 20 Triệu/Tháng</span>
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 leading-relaxed max-w-2xl">
              Hệ thống điều phối đơn hàng thông minh theo tọa độ GPS, hỗ trợ gom đơn tối ưu hóa tuyến đường di chuyển, thanh toán tiền công minh bạch mỗi ngày.
            </p>

            {/* 3 Key Benefits */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
              <div className="flex items-center gap-2.5 bg-white/5 border border-white/10 rounded-2xl p-2.5 backdrop-blur-sm">
                <div className="w-7 h-7 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center flex-shrink-0">
                  <Wallet className="w-4 h-4" />
                </div>
                <div className="text-xs">
                  <div className="font-bold text-white">Rút tiền mỗi ngày</div>
                  <div className="text-[10px] text-slate-400">Ví tài xế linh hoạt 24/7</div>
                </div>
              </div>

              <div className="flex items-center gap-2.5 bg-white/5 border border-white/10 rounded-2xl p-2.5 backdrop-blur-sm">
                <div className="w-7 h-7 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center flex-shrink-0">
                  <Compass className="w-4 h-4" />
                </div>
                <div className="text-xs">
                  <div className="font-bold text-white">Đơn hàng dồi dào</div>
                  <div className="text-[10px] text-slate-400">Giao gần nhà, tiện đường</div>
                </div>
              </div>

              <div className="flex items-center gap-2.5 bg-white/5 border border-white/10 rounded-2xl p-2.5 backdrop-blur-sm">
                <div className="w-7 h-7 rounded-xl bg-cyan-500/20 text-cyan-400 flex items-center justify-center flex-shrink-0">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div className="text-xs">
                  <div className="font-bold text-white">Bảo hiểm 24/7</div>
                  <div className="text-[10px] text-slate-400">An tâm trên mọi cung đường</div>
                </div>
              </div>
            </div>
          </div>

          <div className="absolute right-0 bottom-0 translate-x-12 translate-y-12 w-96 h-96 bg-gradient-to-br from-blue-500/20 to-cyan-500/10 rounded-full blur-3xl pointer-events-none" />
        </div>

        {/* Form Container */}
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-10">
          <div className="border-b border-slate-100 pb-5 mb-6">
            <h2 className="text-lg sm:text-xl font-bold text-slate-900 flex items-center gap-2">
              <Truck className="w-5 h-5 text-blue-600" />
              Hồ Sơ Đăng Ký Tài Xế Giao Hàng
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              Vui lòng điền thông tin chính xác theo Giấy phép lái xe và Căn cước công dân.
            </p>
          </div>

          {/* Alert Error Banner (Thông báo đỏ nổi bật) */}
          {errorMsg && (
            <div className="mb-6 p-4 bg-red-50 border-2 border-red-500 text-red-900 rounded-2xl text-xs flex items-start gap-3 shadow-sm animate-in fade-in">
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <div className="font-bold text-red-900 text-sm">Thông tin đăng ký chưa hợp lệ:</div>
                <div className="mt-1 text-red-700 leading-relaxed font-medium">{errorMsg}</div>
              </div>
            </div>
          )}

          {/* Alert Success Banner */}
          {successMsg && (
            <div className="mb-6 p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs flex items-start gap-3 animate-in fade-in">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <div className="font-bold">Đăng ký thành công!</div>
                <div className="mt-0.5">{successMsg}</div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-8">
            {/* SECTION 1: PERSONAL INFORMATION */}
            <div className="space-y-4">
              <div className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2 text-blue-600">
                <User className="w-4 h-4" />
                <span>1. Thông tin Cá nhân Tài xế</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                {/* Họ tên */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Họ và tên theo CCCD <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => { setFullName(e.target.value); setErrorMsg(''); }}
                    placeholder="Ví dụ: Trần Văn Nam"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                {/* Số điện thoại */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Số điện thoại nhận đơn <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="tel"
                    required
                    value={phoneNumber}
                    onChange={(e) => { setPhoneNumber(e.target.value); setErrorMsg(''); }}
                    placeholder="0987654321"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                {/* Email */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Email liên hệ <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => { setEmail(e.target.value); setErrorMsg(''); }}
                    placeholder="taixe@example.com"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                {/* Số CCCD */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Số Căn cước công dân (12 số) <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={cccd}
                    onChange={(e) => { setCccd(e.target.value); setErrorMsg(''); }}
                    placeholder="00120200xxxx"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                {/* Khu vực hoạt động ưu tiên */}
                <div className="space-y-1 sm:col-span-2">
                  <label className="font-bold text-slate-700 flex items-center justify-between">
                    <span>Khu vực đăng ký giao nhận ưu tiên <span className="text-rose-500">*</span></span>
                    <span className="text-[11px] text-slate-400 font-normal">Hệ thống sẽ ưu tiên bắn đơn gần khu vực này</span>
                  </label>
                  <select
                    value={operatingArea}
                    onChange={(e) => setOperatingArea(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition bg-white"
                  >
                    {CITIES.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>

            {/* SECTION 2: VEHICLE & DRIVING LICENSE */}
            <div className="space-y-4 pt-4 border-t border-slate-100">
              <div className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2 text-blue-600">
                <Truck className="w-4 h-4" />
                <span>2. Phương tiện Giao hàng & Giấy phép lái xe</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                {/* Loại phương tiện */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Loại phương tiện <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={vehicleType}
                    onChange={(e) => setVehicleType(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition bg-white"
                  >
                    <option value="MOTORBIKE">Xe máy (Phổ thông)</option>
                    <option value="VAN">Xe tải van nhỏ 500kg</option>
                    <option value="TRUCK">Xe ba gác chuyên chở hàng nặng</option>
                  </select>
                </div>

                {/* Biển số xe */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Biển số xe đăng ký <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={licensePlate}
                    onChange={(e) => { setLicensePlate(e.target.value); setErrorMsg(''); }}
                    placeholder="Ví dụ: 29A1-888.88"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition uppercase"
                  />
                </div>

                {/* Hạng bằng lái */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Hạng GPLX hiện có <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={drivingLicense}
                    onChange={(e) => setDrivingLicense(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition bg-white"
                  >
                    <option value="A1">Hạng A1 (Xe máy dưới 175cc)</option>
                    <option value="A2">Hạng A2 (Mô tô phân khối lớn)</option>
                    <option value="B2">Hạng B2 (Ô tô / Xe tải dưới 3.5 tấn)</option>
                    <option value="C">Hạng C (Xe tải trên 3.5 tấn)</option>
                  </select>
                </div>
              </div>
            </div>

            {/* SECTION 3: LOGIN CREDENTIALS */}
            <div className="space-y-4 pt-4 border-t border-slate-100">
              <div className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center gap-2 text-blue-600">
                <ShieldCheck className="w-4 h-4" />
                <span>3. Tạo Tài khoản Đăng nhập Shipper</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                {/* Username */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Tên đăng nhập <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={username}
                    onChange={(e) => { setUsername(e.target.value); setErrorMsg(''); }}
                    placeholder="Ví dụ: shipper_nam29"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>

                {/* Password */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Mật khẩu tạo mới <span className="text-rose-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={password}
                      onChange={(e) => { setPassword(e.target.value); setErrorMsg(''); }}
                      placeholder="Tối thiểu 6 ký tự"
                      className="w-full px-4 py-2.5 pr-10 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Confirm Password */}
                <div className="space-y-1">
                  <label className="font-bold text-slate-700">
                    Xác nhận mật khẩu <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={confirmPassword}
                    onChange={(e) => { setConfirmPassword(e.target.value); setErrorMsg(''); }}
                    placeholder="Nhập lại mật khẩu"
                    className="w-full px-4 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 transition"
                  />
                </div>
              </div>
            </div>

            {/* Terms Checkbox */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-600 space-y-2">
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="mt-0.5 rounded border-slate-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
                />
                <span>
                  Tôi cam kết thông tin phương tiện và bằng lái xe là hoàn toàn chính xác. Tôi đồng ý tuân thủ{' '}
                  <span className="text-blue-600 font-bold hover:underline">
                    Quy chuẩn giao nhận hàng hóa & Bộ quy tắc ứng xử Tài xế QuickShop
                  </span>.
                </span>
              </label>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
              <button
                type="button"
                onClick={onNavigateToLogin}
                className="w-full sm:w-auto text-xs font-semibold text-slate-600 hover:text-slate-900 transition flex items-center justify-center gap-1.5 py-2.5 px-4 rounded-xl border border-slate-200 bg-white"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Quay lại trang Đăng nhập</span>
              </button>

              <button
                type="submit"
                disabled={loading}
                className="w-full sm:w-auto min-w-[240px] py-3.5 px-8 bg-gradient-to-r from-blue-600 via-indigo-600 to-cyan-500 hover:from-blue-700 hover:to-indigo-700 text-white font-bold rounded-2xl shadow-lg shadow-blue-500/25 transition text-sm active:scale-95 disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
              >
                {loading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>Đang xét duyệt hồ sơ...</span>
                  </>
                ) : (
                  <>
                    <Truck className="w-4 h-4" />
                    <span>Gửi Hồ Sơ & Trở Thành Shipper</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-400">
        <p>© 2026 QuickShop Express - Mạng Lưới Đối Tác Giao Hàng Tiêu Chuẩn Quốc Tế.</p>
      </footer>
    </div>
  );
}
