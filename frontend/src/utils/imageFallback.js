// Ảnh dự phòng nội bộ dạng inline SVG (không phụ thuộc mạng ngoài, không bao giờ lỗi 404/ERR_FAILED)
export const FALLBACK_PRODUCT_IMAGE =
  "data:image/svg+xml;charset=utf-8," +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="600" viewBox="0 0 600 600">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#fff7ed"/>
          <stop offset="50%" stop-color="#ffedd5"/>
          <stop offset="100%" stop-color="#fed7aa"/>
        </linearGradient>
      </defs>
      <rect width="600" height="600" fill="url(#bg)"/>
      <g fill="none" stroke="#ea580c" stroke-width="14" stroke-linecap="round" stroke-linejoin="round" opacity="0.85">
        <path d="M210 240h180l-16 185a20 20 0 0 1-20 18H246a20 20 0 0 1-20-18L210 240z"/>
        <path d="M255 240v-32a45 45 0 0 1 90 0v32"/>
      </g>
      <text x="300" y="495" text-anchor="middle" font-family="system-ui,-apple-system,sans-serif" font-size="28" font-weight="700" fill="#9a3412">QuickShop Mall</text>
    </svg>`
  );

export const handleImageError = (e) => {
  const img = e?.currentTarget;
  if (
    !img ||
    img.dataset.fallbackApplied === '1' ||
    img.classList?.contains('leaflet-tile') ||
    img.closest?.('.leaflet-pane') ||
    img.closest?.('.leaflet-container')
  ) return;
  img.dataset.fallbackApplied = '1';
  img.src = FALLBACK_PRODUCT_IMAGE;
};
