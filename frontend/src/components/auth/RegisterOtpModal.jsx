import React, { useState, useEffect, useRef } from 'react';
import { 
  X, Mail, ShieldCheck, CheckCircle2, AlertCircle, ArrowLeft, 
  RefreshCw, Clock, Sparkles, KeyRound 
} from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import { AnimatedPresence } from '../motion/MotionPrimitives';
import { LucianButton } from '../ui/LucianPrimitives';

export default function RegisterOtpModal({
  isOpen,
  onClose,
  email,
  fullName,
  username,
  onVerificationSuccess,
  onBackToEdit,
}) {
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [cooldown, setCooldown] = useState(60);
  const [ttl, setTtl] = useState(300); // 5 minutes TTL
  const [isSuccess, setIsSuccess] = useState(false);

  const inputRefs = useRef([]);

  // Reset state when modal opens with new email
  useEffect(() => {
    if (isOpen) {
      setOtpDigits(['', '', '', '', '', '']);
      setErrorMsg('');
      setSuccessMsg('');
      setIsSuccess(false);
      setCooldown(60);
      setTtl(300);

      // Focus first input box
      setTimeout(() => {
        if (inputRefs.current[0]) {
          inputRefs.current[0].focus();
        }
      }, 150);
    }
  }, [isOpen, email]);

  // Timers for Cooldown (60s) and Expiry TTL (300s)
  useEffect(() => {
    if (!isOpen || isSuccess) return;

    const timer = setInterval(() => {
      setCooldown(prev => (prev > 0 ? prev - 1 : 0));
      setTtl(prev => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(timer);
  }, [isOpen, isSuccess]);

  if (!isOpen) return null;

  // Format seconds into MM:SS
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleDigitChange = (index, value) => {
    // Only accept numeric digit
    const cleaned = value.replace(/\D/g, '');
    if (!cleaned) {
      const next = [...otpDigits];
      next[index] = '';
      setOtpDigits(next);
      return;
    }

    // Handle single character
    const char = cleaned.slice(-1);
    const next = [...otpDigits];
    next[index] = char;
    setOtpDigits(next);
    setErrorMsg('');

    // Auto-advance focus to next digit
    if (index < 5 && char) {
      inputRefs.current[index + 1]?.focus();
    }

    // Auto submit if all 6 digits entered
    const fullOtp = next.join('');
    if (fullOtp.length === 6) {
      verifyOtpCode(fullOtp);
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (!pasted) return;

    const next = ['', '', '', '', '', ''];
    for (let i = 0; i < pasted.length; i++) {
      next[i] = pasted[i];
    }
    setOtpDigits(next);
    setErrorMsg('');

    if (pasted.length === 6) {
      inputRefs.current[5]?.focus();
      verifyOtpCode(pasted);
    } else {
      inputRefs.current[pasted.length]?.focus();
    }
  };

  const verifyOtpCode = async (codeToVerify) => {
    const code = codeToVerify || otpDigits.join('');
    if (code.length !== 6) {
      setErrorMsg('Vui lòng nhập đầy đủ 6 chữ số mã OTP.');
      return;
    }

    if (ttl <= 0) {
      setErrorMsg('Mã OTP đã hết hạn sau 5 phút. Vui lòng nhấn "Gửi lại mã OTP" để nhận mã mới.');
      return;
    }

    setErrorMsg('');
    setLoading(true);

    try {
      const res = await axiosClient.post('/api/auth/verify-otp', {
        email: email.trim(),
        otp: code.trim(),
      }, { timeout: 12000 });

      if (res && (res.success || res.data)) {
        const authData = res.data || {};
        setIsSuccess(true);
        setSuccessMsg('Kích hoạt tài khoản thành công! Đang chuyển hướng...');

        setTimeout(() => {
          if (onVerificationSuccess) {
            onVerificationSuccess(authData);
          }
        }, 1200);
        return;
      }

      setErrorMsg(res?.message || 'Mã OTP không chính xác. Vui lòng kiểm tra lại!');
    } catch (err) {
      const serverMsg = err?.response?.data?.message || err?.message;
      setErrorMsg(serverMsg || 'Mã OTP không chính xác hoặc đã hết hạn.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (cooldown > 0 || resending) return;

    setErrorMsg('');
    setSuccessMsg('');
    setResending(true);

    try {
      const res = await axiosClient.post('/api/auth/resend-otp', {
        email: email.trim(),
      }, { timeout: 10000 });

      if (res && (res.success || res.data)) {
        setCooldown(60);
        setTtl(300);
        setOtpDigits(['', '', '', '', '', '']);
        setSuccessMsg('Mã OTP mới đã được gửi tới email của bạn!');
        inputRefs.current[0]?.focus();
      } else {
        setErrorMsg(res?.message || 'Không thể gửi lại mã OTP. Vui lòng thử lại.');
      }
    } catch (err) {
      const msg = err?.response?.data?.message || err?.message;
      setErrorMsg(msg || 'Lỗi khi gửi lại mã OTP. Vui lòng chờ thêm ít phút.');
    } finally {
      setResending(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  return (
    <AnimatedPresence show={isOpen} onClose={onClose}>
      <div 
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md cursor-pointer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="otp-modal-title"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div 
          className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-slate-200/90 overflow-hidden relative animate-in fade-in zoom-in-95 duration-200 cursor-default"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header Bar */}
          <div className="px-6 pt-6 pb-4 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-orange-500 via-rose-500 to-amber-500 flex items-center justify-center text-white shadow-md shadow-orange-500/20">
                <ShieldCheck className="w-5 h-5 stroke-[2.5]" />
              </div>
              <div>
                <h3 id="otp-modal-title" className="text-base font-extrabold text-slate-900 tracking-tight">
                  Xác thực mã OTP đăng ký
                </h3>
                <p className="text-[11px] text-slate-500 font-medium">
                  Kích hoạt tài khoản bảo mật QuickShop
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              disabled={loading || isSuccess}
              aria-label="Đóng cửa sổ xác thực OTP"
              className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition disabled:opacity-40"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="p-6 sm:p-7 space-y-5">
            {isSuccess ? (
              /* Success State */
              <div className="py-8 text-center space-y-4 animate-in fade-in">
                <div className="w-16 h-16 rounded-3xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-lg shadow-emerald-500/20 animate-in zoom-in-95 duration-300">
                  <CheckCircle2 className="w-8 h-8" />
                </div>
                <div>
                  <h4 className="text-lg font-bold text-slate-900">Xác thực tài khoản thành công!</h4>
                  <p className="text-xs text-slate-500 mt-1">
                    Chào mừng <strong>{fullName || username || email}</strong> đến với QuickShop.
                  </p>
                </div>
                <div className="text-xs text-emerald-600 font-semibold flex items-center justify-center gap-2">
                  <div className="w-3.5 h-3.5 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin" />
                  <span>Đang tự động đăng nhập và tải trang...</span>
                </div>
              </div>
            ) : (
              /* Verification Form */
              <>
                {/* Email Chip Banner */}
                <div className="p-3.5 bg-orange-50/70 border border-orange-200/90 rounded-2xl flex items-start gap-3">
                  <div className="w-8 h-8 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Mail className="w-4 h-4" />
                  </div>
                  <div className="text-xs text-slate-700 flex-1 leading-relaxed">
                    Mã xác thực 6 chữ số đã được gửi tới email:
                    <div className="font-bold text-orange-700 font-mono text-[13px] break-all mt-0.5">
                      {email}
                    </div>
                  </div>
                </div>

                {/* 6-box OTP Input */}
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label className="text-xs font-bold text-slate-700">
                      Nhập mã OTP 6 chữ số:
                    </label>
                    <div className="flex items-center gap-1 text-[11px] font-mono text-slate-500">
                      <Clock className="w-3 h-3 text-orange-500" />
                      <span>Hết hạn: <strong className={ttl < 60 ? 'text-red-500' : 'text-slate-700'}>{formatTime(ttl)}</strong></span>
                    </div>
                  </div>

                  <div className="grid grid-cols-6 gap-2 sm:gap-2.5 on-paste" onPaste={handlePaste}>
                    {otpDigits.map((digit, idx) => (
                      <input
                        key={idx}
                        ref={(el) => (inputRefs.current[idx] = el)}
                        type="text"
                        inputMode="numeric"
                        pattern="[0-9]*"
                        maxLength={1}
                        value={digit}
                        onChange={(e) => handleDigitChange(idx, e.target.value)}
                        onKeyDown={(e) => handleKeyDown(idx, e)}
                        className={`w-full aspect-square text-center font-mono font-bold text-lg sm:text-xl rounded-2xl border transition focus:outline-none ${
                          errorMsg 
                            ? 'bg-red-50/40 border-red-500 text-red-900 focus:ring-4 focus:ring-red-500/10' 
                            : digit 
                              ? 'bg-orange-50/30 border-orange-500 text-slate-900 focus:ring-4 focus:ring-orange-500/10' 
                              : 'bg-slate-50 border-slate-200 hover:border-slate-300 focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 text-slate-900'
                        }`}
                      />
                    ))}
                  </div>
                  <p className="text-[11px] text-slate-400 mt-2 text-center">
                    Gợi ý: Bạn có thể dán (paste) trực tiếp mã OTP 6 số vào ô đầu tiên.
                  </p>
                </div>

                {/* Error Banner */}
                {errorMsg && (
                  <div role="alert" className="p-3 bg-red-50 border border-red-300 text-red-800 rounded-xl text-xs flex items-start gap-2 shadow-sm animate-in fade-in">
                    <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0 mt-0.5" />
                    <span className="font-medium leading-relaxed">{errorMsg}</span>
                  </div>
                )}

                {/* Success Banner */}
                {successMsg && !isSuccess && (
                  <div className="p-3 bg-emerald-50 border border-emerald-300 text-emerald-800 rounded-xl text-xs flex items-start gap-2 shadow-sm animate-in fade-in">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                    <span className="font-medium leading-relaxed">{successMsg}</span>
                  </div>
                )}

                {/* Actions */}
                <div className="space-y-2.5 pt-1">
                  <LucianButton
                    variant="primary"
                    disabled={loading || otpDigits.join('').length !== 6 || ttl <= 0}
                    loading={loading}
                    onClick={() => verifyOtpCode()}
                    className="w-full py-3"
                  >
                    XÁC THỰC & KÍCH HOẠT TÀI KHOẢN
                  </LucianButton>

                  <div className="flex items-center justify-between pt-1">
                    <button
                      type="button"
                      onClick={onBackToEdit}
                      disabled={loading}
                      className="text-xs text-slate-500 hover:text-slate-800 flex items-center gap-1 font-semibold transition"
                    >
                      <ArrowLeft className="w-3.5 h-3.5" />
                      <span>Sửa thông tin / email</span>
                    </button>

                    <button
                      type="button"
                      onClick={handleResendOtp}
                      disabled={cooldown > 0 || resending || loading}
                      className={`text-xs font-semibold flex items-center gap-1.5 transition ${
                        cooldown > 0 || resending
                          ? 'text-slate-400 cursor-not-allowed'
                          : 'text-orange-600 hover:text-orange-700 hover:underline cursor-pointer'
                      }`}
                    >
                      <RefreshCw className={`w-3.5 h-3.5 ${resending ? 'animate-spin' : ''}`} />
                      <span>
                        {resending 
                          ? 'Đang gửi lại...' 
                          : cooldown > 0 
                            ? `Gửi lại mã sau (${cooldown}s)` 
                            : 'Gửi lại mã OTP'}
                      </span>
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </AnimatedPresence>
  );
}
