import React, { useState } from 'react';
import { X, User, Key, ShieldCheck, Store, ShoppingBag, Truck, CheckCircle2, AlertCircle } from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import { useCartStore } from '../stores/useCartStore';
import { generateValidSignedToken } from '../utils/tokenHelper';
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
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleLoginSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    const trimmedUser = username.trim();
    const trimmedPass = password.trim();

    if (!trimmedUser) {
      setErrorMsg('Vui lòng nhập tên đăng nhập hoặc email.');
      return;
    }
    if (!trimmedPass) {
      setErrorMsg('Vui lòng nhập mật khẩu.');
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
          if (role !== 'ROLE_SHIPPER') {
            await syncCartOnLogin(mockData.userId, { dedupeRecent: true });
          }
        } catch {}
        setSuccessMsg(`Đăng nhập thành công: ${fullName} [${role}]`);
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

      setErrorMsg(serverMessage || 'Lỗi kết nối tới máy chủ xác thực.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AnimatedPresence show={isOpen} mode="modal" dataLenisPrevent={true}>
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
        <div className="p-5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 text-white flex justify-between items-center">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center">
              <User className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="font-bold text-sm">Đăng nhập QuickShop</h3>
              <p className="text-[11px] text-white/80">Hệ thống thương mại điện tử đa phân hệ</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-white/20 transition cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4 text-xs">
          <form onSubmit={handleLoginSubmit} className="space-y-3.5">
            <div>
              <label className="font-bold text-slate-700 block mb-1">Tên đăng nhập hoặc Email:</label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => { setUsername(e.target.value); setErrorMsg(''); }}
                placeholder="Nhập tên đăng nhập hoặc email..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white transition"
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="font-bold text-slate-700">Mật khẩu:</label>
              </div>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => { setPassword(e.target.value); setErrorMsg(''); }}
                placeholder="Nhập mật khẩu..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white transition"
              />
            </div>

            {errorMsg && (
              <div className="p-3 bg-rose-50 border border-rose-300 text-rose-800 rounded-xl text-xs flex items-start gap-2 shadow-xs animate-in fade-in">
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <div className="font-bold text-rose-900">Thông báo lỗi:</div>
                  <div className="text-rose-700 mt-0.5">{errorMsg}</div>
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
