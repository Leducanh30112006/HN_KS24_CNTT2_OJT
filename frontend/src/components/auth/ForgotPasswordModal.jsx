import React, { useState } from 'react';
import { X, KeyRound, Mail, Lock, ShieldCheck, CheckCircle2, AlertCircle, Eye, EyeOff, Sparkles, ArrowLeft } from 'lucide-react';
import GoogleAuthButton from './GoogleAuthButton';
import axiosClient from '../../api/axiosClient';
import { AnimatedPresence } from '../motion/MotionPrimitives';
import { useAuthStore } from '../../stores/useAuthStore';

export default function ForgotPasswordModal({ isOpen, onClose, onRecoverySuccess }) {
  const { login } = useAuthStore();

  const [mode, setMode] = useState('google'); // 'google' | 'otp'
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // OTP flow states
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  if (!isOpen) return null;

  const handleGoogleSuccess = (userData, message) => {
    setErrorMsg('');
    setSuccessMsg(message || 'Xác thực tài khoản Google thành công!');

    if (userData) {
      const userId = userData.userId ?? userData.id ?? 1001;
      const role = userData.role || 'ROLE_BUYER';
      login({ ...userData, userId, role });
    }

    setTimeout(() => {
      onClose();
      if (onRecoverySuccess) onRecoverySuccess(userData);
    }, 1000);
  };

  const handleGoogleError = (errorText) => {
    setErrorMsg(errorText || 'Không thể khôi phục tài khoản qua Google. Vui lòng thử lại!');
    setSuccessMsg('');
  };

  const handleSendOtp = async (e) => {
    if (e) e.preventDefault();
    if (!email.trim()) {
      setErrorMsg('Vui lòng nhập địa chỉ email đã đăng ký.');
      return;
    }
    setErrorMsg('');
    setLoading(true);

    try {
      const res = await axiosClient.post('/api/auth/forgot-password', { email: email.trim() }, { timeout: 10000 });
      if (res && (res.success || res.data)) {
        setOtpSent(true);
        setSuccessMsg('Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư!');
      } else {
        throw new Error(res?.message || 'Không thể gửi mã OTP.');
      }
    } catch (err) {
      setErrorMsg(err?.response?.data?.message || err?.message || 'Lỗi gửi yêu cầu khôi phục mật khẩu.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetWithOtp = async (e) => {
    if (e) e.preventDefault();
    if (!otp.trim()) {
      setErrorMsg('Vui lòng nhập mã OTP.');
      return;
    }
    if (!newPassword || newPassword.length < 6) {
      setErrorMsg('Mật khẩu mới phải có tối thiểu 6 ký tự.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setErrorMsg('Mật khẩu xác nhận không khớp.');
      return;
    }

    setErrorMsg('');
    setLoading(true);

    try {
      const res = await axiosClient.post(
        '/api/auth/reset-password',
        {
          email: email.trim(),
          otp: otp.trim(),
          newPassword: newPassword.trim(),
        },
        { timeout: 10000 }
      );

      if (res && (res.success || res.data)) {
        setSuccessMsg('Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.');
        setTimeout(() => {
          onClose();
          if (onRecoverySuccess) onRecoverySuccess();
        }, 1200);
      } else {
        throw new Error(res?.message || 'Đặt lại mật khẩu không thành công.');
      }
    } catch (err) {
      setErrorMsg(err?.response?.data?.message || err?.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <AnimatedPresence show={isOpen}>
      <div
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-sm"
        role="dialog"
        aria-modal="true"
        aria-labelledby="forgot-password-title"
      >
        <div className="bg-white rounded-3xl shadow-2xl border border-slate-200/90 w-full max-w-md overflow-hidden relative animate-in fade-in zoom-in-95 duration-200">
          {/* Header */}
          <div className="px-6 pt-6 pb-4 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-2xl bg-orange-100 text-orange-600 flex items-center justify-center flex-shrink-0">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <h3 id="forgot-password-title" className="text-sm font-bold text-slate-900">
                  Khôi Phục Mật Khẩu
                </h3>
                <p className="text-[11px] text-slate-500">Xác thực danh tính an toàn với Google hoặc Email OTP</p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng cửa sổ"
              className="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 flex items-center justify-center transition cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="p-6 space-y-4">
            {/* Mode switch tabs */}
            <div className="grid grid-cols-2 p-1 bg-slate-100 rounded-2xl text-xs font-semibold">
              <button
                type="button"
                onClick={() => {
                  setMode('google');
                  setErrorMsg('');
                  setSuccessMsg('');
                }}
                className={`py-2 rounded-xl transition flex items-center justify-center gap-1.5 cursor-pointer ${
                  mode === 'google'
                    ? 'bg-white text-orange-600 shadow-sm font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Sparkles className="w-3.5 h-3.5 text-orange-500" />
                <span>Qua Google (Nhanh)</span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('otp');
                  setErrorMsg('');
                  setSuccessMsg('');
                }}
                className={`py-2 rounded-xl transition flex items-center justify-center gap-1.5 cursor-pointer ${
                  mode === 'otp'
                    ? 'bg-white text-orange-600 shadow-sm font-bold'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                <span>Qua Email OTP</span>
              </button>
            </div>

            {/* ERROR & SUCCESS ALERTS */}
            {errorMsg && (
              <div className="p-3 bg-red-50 border border-red-200 text-red-900 rounded-2xl text-xs flex items-start gap-2 animate-in fade-in">
                <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                <span className="font-medium">{errorMsg}</span>
              </div>
            )}
            {successMsg && (
              <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-2xl text-xs flex items-start gap-2 animate-in fade-in">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                <span className="font-medium">{successMsg}</span>
              </div>
            )}

            {/* METHOD 1: GOOGLE ONE-CLICK RECOVERY */}
            {mode === 'google' && (
              <div className="space-y-4 pt-1">
                <div className="p-3.5 bg-orange-50/70 border border-orange-200/80 rounded-2xl text-[11px] text-orange-900 leading-relaxed space-y-1">
                  <div className="font-bold flex items-center gap-1.5 text-orange-800">
                    <ShieldCheck className="w-4 h-4 text-orange-600" />
                    Khôi phục trực tiếp không cần nhớ mật khẩu
                  </div>
                  <p>
                    Đăng nhập bằng tài khoản Google liên kết với email của bạn. Hệ thống sẽ lập tức cấp lại quyền truy cập an toàn.
                  </p>
                </div>

                <div className="space-y-2">
                  <label className="block text-xs font-bold text-slate-700">
                    Mật khẩu mới (Tùy chọn):
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Nhập nếu bạn muốn đổi mật khẩu mới..."
                      className="w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 bg-slate-50 border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 focus:outline-none"
                    />
                    <Lock className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-600"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  <span className="text-[10px] text-slate-500">
                    Để trống nếu bạn chỉ muốn xác thực và đăng nhập ngay bằng Google.
                  </span>
                </div>

                <div className="pt-2">
                  <GoogleAuthButton
                    action="FORGOT_PASSWORD"
                    newPassword={newPassword}
                    onSuccess={handleGoogleSuccess}
                    onError={handleGoogleError}
                    buttonText="Xác minh Google & Đăng nhập ngay"
                    className="py-3 font-bold border-orange-200 hover:border-orange-500 text-slate-800 shadow-sm"
                  />
                </div>
              </div>
            )}

            {/* METHOD 2: TRADITIONAL EMAIL OTP RECOVERY */}
            {mode === 'otp' && (
              <div className="space-y-3.5 pt-1">
                {!otpSent ? (
                  <form onSubmit={handleSendOtp} className="space-y-3">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Địa chỉ Email đăng ký:
                      </label>
                      <div className="relative">
                        <input
                          type="email"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder="nhập email của bạn..."
                          required
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl text-xs text-slate-900 bg-slate-50 border border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 focus:outline-none"
                        />
                        <Mail className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full py-2.5 bg-slate-900 hover:bg-black text-white font-bold rounded-xl text-xs transition cursor-pointer disabled:opacity-50"
                    >
                      {loading ? 'Đang gửi mã...' : 'Gửi mã xác thực OTP'}
                    </button>
                  </form>
                ) : (
                  <form onSubmit={handleResetWithOtp} className="space-y-3">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Mã xác thực OTP (6 chữ số):
                      </label>
                      <input
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value)}
                        placeholder="Nhập mã OTP..."
                        required
                        className="w-full px-4 py-2.5 rounded-xl text-xs text-slate-900 bg-slate-50 border border-slate-300 focus:border-orange-500 focus:outline-none font-mono font-bold"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Mật khẩu mới:
                      </label>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                          placeholder="Tối thiểu 6 ký tự..."
                          required
                          className="w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 bg-slate-50 border border-slate-300 focus:border-orange-500 focus:outline-none"
                        />
                        <Lock className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3.5 top-3 text-slate-400"
                        >
                          {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">
                        Xác nhận mật khẩu mới:
                      </label>
                      <div className="relative">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder="Nhập lại mật khẩu mới..."
                          required
                          className="w-full pl-10 pr-10 py-2.5 rounded-xl text-xs text-slate-900 bg-slate-50 border border-slate-300 focus:border-orange-500 focus:outline-none"
                        />
                        <ShieldCheck className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" />
                        <button
                          type="button"
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          className="absolute right-3.5 top-3 text-slate-400"
                        >
                          {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>

                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white font-bold rounded-xl text-xs transition cursor-pointer disabled:opacity-50"
                    >
                      {loading ? 'Đang cập nhật mật khẩu...' : 'Xác nhận & Cập nhật mật khẩu'}
                    </button>

                    <button
                      type="button"
                      onClick={() => setOtpSent(false)}
                      className="w-full text-center text-[11px] text-slate-500 hover:text-slate-800 transition"
                    >
                      Quay lại nhập lại email
                    </button>
                  </form>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </AnimatedPresence>
  );
}
