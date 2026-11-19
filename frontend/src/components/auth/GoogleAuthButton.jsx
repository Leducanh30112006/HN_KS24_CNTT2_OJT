import React, { useState } from 'react';
import { triggerGoogleAuth } from '../../utils/googleAuth';

/**
 * Standard, beautiful Google Authentication CTA button
 * Supports LOGIN, REGISTER, and FORGOT_PASSWORD actions.
 */
export default function GoogleAuthButton({
  action = 'LOGIN',
  newPassword = '',
  onSuccess,
  onError,
  className = '',
  buttonText,
}) {
  const [loading, setLoading] = useState(false);

  const defaultText =
    action === 'FORGOT_PASSWORD'
      ? 'Khôi phục tài khoản bằng Google'
      : action === 'REGISTER'
      ? 'Đăng ký nhanh bằng Google'
      : 'Tiếp tục với Google';

  const label = buttonText || defaultText;

  const handleClick = async () => {
    if (loading) return;
    setLoading(true);

    try {
      const result = await triggerGoogleAuth({ action, newPassword });
      if (result && result.success) {
        if (onSuccess) onSuccess(result.data, result.message);
      } else {
        throw new Error(result?.message || 'Xác thực Google không thành công');
      }
    } catch (err) {
      console.error('Google Auth error:', err);
      const msg = err?.message || 'Đăng nhập Google thất bại hoặc đã bị đóng.';
      if (onError) onError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={loading}
      aria-label={label}
      className={`w-full py-2.5 px-4 bg-white hover:bg-slate-50/90 active:bg-slate-100 text-slate-700 font-semibold text-xs rounded-xl border border-slate-300/90 shadow-sm hover:shadow transition-all duration-200 flex items-center justify-center gap-2.5 cursor-pointer disabled:opacity-60 active:scale-[0.98] select-none ${className}`}
    >
      {loading ? (
        <div className="w-4 h-4 border-2 border-slate-400 border-t-orange-500 rounded-full animate-spin flex-shrink-0" />
      ) : (
        <svg className="w-4 h-4 flex-shrink-0" viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="#4285F4"
            d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.17z"
          />
          <path
            fill="#34A853"
            d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.34 24 12 24z"
          />
          <path
            fill="#FBBC05"
            d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
          />
          <path
            fill="#EA4335"
            d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.34 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
          />
        </svg>
      )}
      <span className="truncate">{loading ? 'Đang kết nối Google...' : label}</span>
    </button>
  );
}
