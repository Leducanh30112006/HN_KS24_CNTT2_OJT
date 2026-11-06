import React from 'react';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

/**
 * Reusable Pagination Component for QuickShop
 * Hỗ trợ chuyển trang số, nút Trước/Sau, nhảy đầu/cuối trang và tùy chọn số lượng sản phẩm/trang
 */
export default function Pagination({
  currentPage = 0,
  totalPages = 1,
  totalElements = 0,
  pageSize = 12,
  onPageChange,
  onPageSizeChange,
}) {
  if (totalPages <= 1 && totalElements <= pageSize) {
    return null;
  }

  const startItem = totalElements === 0 ? 0 : currentPage * pageSize + 1;
  const endItem = Math.min((currentPage + 1) * pageSize, totalElements);

  // Thuật toán hiển thị các số trang thông minh kèm dấu chấm lửng (...)
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible + 2) {
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(0); // Luôn hiện trang đầu

      let start = Math.max(1, currentPage - 1);
      let end = Math.min(totalPages - 2, currentPage + 1);

      if (currentPage <= 2) {
        start = 1;
        end = 3;
      } else if (currentPage >= totalPages - 3) {
        start = totalPages - 4;
        end = totalPages - 2;
      }

      if (start > 1) {
        pages.push('dots-1');
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (end < totalPages - 2) {
        pages.push('dots-2');
      }

      pages.push(totalPages - 1); // Luôn hiện trang cuối
    }

    return pages;
  };

  const pages = getPageNumbers();

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 p-3 sm:px-5 sm:py-3.5 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
      {/* Thông tin số lượng hiển thị */}
      <div className="text-slate-500 font-medium text-center sm:text-left flex items-center gap-2">
        <span>
          Hiển thị <strong className="text-slate-800 font-bold">{startItem} - {endItem}</strong> trong tổng số{' '}
          <strong className="text-orange-600 font-bold">{totalElements.toLocaleString('vi-VN')}</strong> sản phẩm
        </span>

        {onPageSizeChange && (
          <div className="hidden md:flex items-center gap-1.5 ml-3 pl-3 border-l border-slate-200">
            <span className="text-slate-400">Mỗi trang:</span>
            <select
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              className="bg-slate-50 border border-slate-200 text-slate-700 rounded-lg px-2 py-1 text-xs font-semibold focus:outline-none focus:ring-1 focus:ring-orange-500"
            >
              <option value={12}>12</option>
              <option value={24}>24</option>
              <option value={36}>36</option>
              <option value={48}>48</option>
            </select>
          </div>
        )}
      </div>

      {/* Điều hướng các nút phân trang */}
      <div className="flex items-center space-x-1 sm:space-x-1.5">
        {/* Nút về trang đầu tiên */}
        <button
          onClick={() => onPageChange(0)}
          disabled={currentPage === 0}
          className="p-1.5 sm:p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          title="Về trang đầu"
        >
          <ChevronsLeft className="w-3.5 h-3.5" />
        </button>

        {/* Nút lùi 1 trang */}
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="p-1.5 sm:p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          title="Trang trước"
        >
          <ChevronLeft className="w-3.5 h-3.5" />
        </button>

        {/* Danh sách các số trang */}
        {pages.map((p, idx) => {
          if (typeof p === 'string') {
            return (
              <span key={`dots-${idx}`} className="px-2 py-1 text-slate-400 select-none">
                ...
              </span>
            );
          }

          const isActive = p === currentPage;
          return (
            <button
              key={p}
              onClick={() => onPageChange(p)}
              className={`min-w-[32px] sm:min-w-[36px] h-8 sm:h-9 px-2 rounded-xl text-xs font-bold transition ${
                isActive
                  ? 'bg-orange-600 text-white shadow-md shadow-orange-600/30'
                  : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-orange-300'
              }`}
            >
              {p + 1}
            </button>
          );
        })}

        {/* Nút sang 1 trang */}
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="p-1.5 sm:p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          title="Trang sau"
        >
          <ChevronRight className="w-3.5 h-3.5" />
        </button>

        {/* Nút tới trang cuối cùng */}
        <button
          onClick={() => onPageChange(totalPages - 1)}
          disabled={currentPage >= totalPages - 1}
          className="p-1.5 sm:p-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition"
          title="Tới trang cuối"
        >
          <ChevronsRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
