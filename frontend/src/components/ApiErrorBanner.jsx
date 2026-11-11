import React from 'react';
import { AlertTriangle, RefreshCw, X } from 'lucide-react';

/**
 * ApiErrorBanner Component
 * Hiển thị cảnh báo thân thiện khi API Catalog gặp sự cố kết nối,
 * thông báo chế độ Fallback và cho phép người dùng bấm "Thử lại ngay".
 */
export default function ApiErrorBanner({
  error,
  isRetrying = false,
  onRetry,
  onDismiss,
  fallbackActive = true,
  title = 'Sự cố kết nối máy chủ Catalog API',
  fallbackBadge = 'Chế độ dữ liệu dự phòng',
  fallbackHint = 'Hệ thống đang hiển thị dữ liệu dự phòng để bạn tiếp tục trải nghiệm mua sắm.',
}) {
  if (!error) return null;

  return (
    <div className="bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200/90 rounded-2xl p-4 shadow-sm text-xs text-amber-900 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-in fade-in duration-200">
      <div className="flex items-start gap-3">
        <div className="p-2 bg-amber-100 text-amber-700 rounded-xl shrink-0 mt-0.5 sm:mt-0">
          <AlertTriangle className="w-4 h-4" />
        </div>
        <div className="space-y-0.5">
          <div className="font-bold text-amber-950 flex items-center gap-2">
            <span>{title}</span>
            {fallbackActive && (
              <span className="bg-amber-200/80 text-amber-800 text-[10px] font-bold px-2 py-0.5 rounded-full">
                {fallbackBadge}
              </span>
            )}
          </div>
          <p className="text-amber-800/90 leading-relaxed">
            {typeof error === 'string' ? error : error?.message || 'Không thể đồng bộ dữ liệu thời gian thực từ máy chủ.'}
            {fallbackActive && fallbackHint ? ` ${fallbackHint}` : ''}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
        {onRetry && (
          <button
            onClick={onRetry}
            disabled={isRetrying}
            className="px-3.5 py-1.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl font-semibold shadow-sm transition flex items-center gap-1.5 disabled:opacity-50 active:scale-95"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRetrying ? 'animate-spin' : ''}`} />
            <span>{isRetrying ? 'Đang thử lại...' : 'Thử lại ngay'}</span>
          </button>
        )}

        {onDismiss && (
          <button
            onClick={onDismiss}
            className="p-1.5 text-amber-700 hover:text-amber-900 hover:bg-amber-100 rounded-lg transition"
            title="Đóng thông báo"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}
