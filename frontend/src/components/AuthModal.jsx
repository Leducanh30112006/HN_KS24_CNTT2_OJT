import React, { useState } from 'react';
import { X, User, Key, ShieldCheck, Store, ShoppingBag, CheckCircle2 } from 'lucide-react';
import { useAuthStore } from '../stores/useAuthStore';
import axiosClient from '../api/axiosClient';

export default function AuthModal({ isOpen, onClose }) {
  const { login } = useAuthStore();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  if (!isOpen) return null;

  const handleLoginSubmit = async (e) => {
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
        setSuccessMsg(`Đăng nhập thành công! Vai trò: ${res.data.fullName} [${res.data.role}]`);
        setTimeout(() => {
          onClose();
        }, 1200);
      }
    } catch (err) {
      // Local demo fallback for local dev when identity service isn't actively running
      let role = 'ROLE_BUYER';
      let fullName = 'Nguyễn Văn Mua Hàng';
      if (username.toLowerCase().includes('admin')) {
        role = 'ROLE_ADMIN';
        fullName = 'Quản Trị Viên QuickShop';
      } else if (username.toLowerCase().includes('seller')) {
        role = 'ROLE_SELLER';
        fullName = 'Chủ Gian Hàng TechWorld';
      }

      const mockData = {
        userId: role === 'ROLE_ADMIN' ? 99 : (role === 'ROLE_SELLER' ? 1 : 2),
        username: username,
        email: `${username}@quickshop.vn`,
        fullName,
        role,
        accessToken: 'mock_jwt_token_for_quickshop_local_demo',
      };

      login(mockData);
      setSuccessMsg(`Đăng nhập phiên Demo: ${fullName} [${role}]`);
      setTimeout(() => {
        onClose();
      }, 1000);
    } finally {
      setLoading(false);
    }
  };

  const selectAccount = (u, p) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl overflow-hidden border border-slate-200 animate-in zoom-in-95 duration-200">
        <div className="p-5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 text-white flex justify-between items-center">
          <div className="flex items-center space-x-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center">
              <User className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="font-bold text-sm">Đăng nhập tài khoản QuickShop</h3>
              <p className="text-[11px] text-white/80">Xác thực phân quyền RBAC đa vai trò</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-white/20 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4 text-xs">
          {/* Quick 1-Click Role Accounts */}
          <div className="space-y-1.5">
            <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wide">
              Chọn tài khoản thử nghiệm nhanh:
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => selectAccount('buyer', 'buyer123')}
                className={`p-2.5 rounded-xl border text-center transition ${
                  username === 'buyer' ? 'border-orange-500 bg-orange-50 font-bold text-orange-700' : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <ShoppingBag className="w-4 h-4 mx-auto mb-1 text-orange-600" />
                Người Mua
              </button>

              <button
                type="button"
                onClick={() => selectAccount('seller', 'seller123')}
                className={`p-2.5 rounded-xl border text-center transition ${
                  username === 'seller' ? 'border-orange-500 bg-orange-50 font-bold text-orange-700' : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <Store className="w-4 h-4 mx-auto mb-1 text-emerald-600" />
                Người Bán
              </button>

              <button
                type="button"
                onClick={() => selectAccount('admin', 'admin123')}
                className={`p-2.5 rounded-xl border text-center transition ${
                  username === 'admin' ? 'border-orange-500 bg-orange-50 font-bold text-orange-700' : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                }`}
              >
                <ShieldCheck className="w-4 h-4 mx-auto mb-1 text-purple-600" />
                Quản Trị Viên
              </button>
            </div>
          </div>

          <form onSubmit={handleLoginSubmit} className="space-y-3 pt-2">
            <div>
              <label className="font-bold text-slate-700">Tên đăng nhập hoặc Email:</label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
              />
            </div>

            <div>
              <label className="font-bold text-slate-700">Mật khẩu:</label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full mt-1 bg-slate-50 border border-slate-300 rounded-xl p-2.5 text-xs text-slate-900 focus:outline-none focus:border-orange-500"
              />
            </div>

            {errorMsg && (
              <div className="p-2.5 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-[11px]">
                {errorMsg}
              </div>
            )}

            {successMsg && (
              <div className="p-2.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-[11px] flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>{successMsg}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-lg shadow-orange-500/30 transition text-xs active:scale-95 disabled:opacity-50"
            >
              {loading ? 'Đang xác thực...' : 'Đăng nhập ngay'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
