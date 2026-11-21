import { defaultProducts } from '../data/sampleProducts';

/**
 * QuickShop Flash Sale Store & Utilities
 * Quản lý cấu hình Flash Sale toàn sàn: Tự chọn sản phẩm hoặc AI tự động chọn
 */

const FLASH_SALE_STORAGE_KEY = 'quickshop_flash_sale_config';

export const DEFAULT_FLASH_SALE_CONFIG = {
  isEnabled: true,
  mode: 'AI', // 'AI' | 'MANUAL'
  discountPercent: 30,
  slotHours: [9, 12, 15, 18, 21, 24],
  selectedProductIds: [], // Dùng khi mode === 'MANUAL'
  aiSelectedProductIds: [], // Dùng khi mode === 'AI'
  lastAiAnalyzedAt: null,
};

/**
 * Tạo kho sản phẩm hợp nhất (server + catalog mẫu) để Flash Sale luôn dồi dào ít nhất 50+ deal
 */
function getUnifiedCatalogPool(allProducts = []) {
  const serverList = Array.isArray(allProducts) ? allProducts : [];
  const serverIds = new Set(serverList.map((p) => String(p.id)));
  const additional = (defaultProducts || []).filter((dp) => !serverIds.has(String(dp.id)));
  return [...serverList, ...additional];
}

/**
 * Lấy cấu hình Flash Sale hiện tại từ localStorage
 */
export function getFlashSaleConfig() {
  try {
    const raw = localStorage.getItem(FLASH_SALE_STORAGE_KEY);
    if (!raw) return { ...DEFAULT_FLASH_SALE_CONFIG };
    const parsed = JSON.parse(raw);
    return { ...DEFAULT_FLASH_SALE_CONFIG, ...parsed };
  } catch {
    return { ...DEFAULT_FLASH_SALE_CONFIG };
  }
}

/**
 * Lưu cấu hình Flash Sale và phát sự kiện đồng bộ toàn hệ thống
 */
export function saveFlashSaleConfig(newConfig) {
  const current = getFlashSaleConfig();
  const updated = { ...current, ...newConfig };
  try {
    localStorage.setItem(FLASH_SALE_STORAGE_KEY, JSON.stringify(updated));
    window.dispatchEvent(new CustomEvent('quickshop_flash_sale_updated', { detail: updated }));
  } catch (e) {
    console.warn('Không thể lưu cấu hình Flash Sale vào localStorage:', e);
  }
  return updated;
}

/**
 * Thuật toán AI phân tích và đề xuất sản phẩm tham gia Flash Sale
 * Dựa trên: Điểm đánh giá (>= 4.2), tồn kho dồi dào, mức độ quan tâm
 */
export function analyzeAndSelectFlashSaleWithAI(allProducts = []) {
  const pool = getUnifiedCatalogPool(allProducts);
  if (pool.length === 0) return [];

  // Lọc sản phẩm hợp lệ đang mở bán (ACTIVE, APPROVED hoặc không có status)
  const candidates = pool.filter((p) => {
    const status = String(p.status || '').toUpperCase();
    const isLive = status === 'ACTIVE' || status === 'APPROVED' || !p.status;
    const isNotDraft = status !== 'DRAFT' && status !== 'PENDING' && status !== 'REJECTED';
    const hasName = p.name && p.name.trim().length > 3;
    return isLive && isNotDraft && hasName;
  });

  // Chấm điểm tiềm năng Flash Sale của từng sản phẩm
  const scored = candidates.map((p) => {
    let score = 50;
    const rating = Number(p.rating || p.averageRating || 4.5);
    const sold = Number(p.sold || p.totalSold || p.soldCount || 10);
    const price = Number(p.minPrice || p.price || 0);

    // Ưu tiên sản phẩm rating cao
    if (rating >= 4.8) score += 30;
    else if (rating >= 4.5) score += 20;

    // Ưu tiên sản phẩm bán chạy
    if (sold > 50) score += 25;
    else if (sold > 10) score += 15;

    // Ưu tiên sản phẩm thương mại thật có thương hiệu hoặc giá trị hợp lý
    if (p.brand) score += 10;
    if (price >= 50000) score += 15;

    // Đề xuất mức giảm giá thông minh từ 15% đến 40% tùy theo phân khúc giá
    let recommendedDiscount = 25;
    if (price > 10000000) recommendedDiscount = 15; // Hàng giá trị cao giảm nhẹ
    else if (price < 500000) recommendedDiscount = 35; // Hàng phổ thông giảm mạnh kéo traffic
    else recommendedDiscount = 25;

    return {
      product: p,
      score,
      recommendedDiscount,
      aiReason: `Đánh giá ${rating}★ • ${sold} đã bán • Chiết khấu tối ưu ${recommendedDiscount}% để tối đa hóa doanh số`,
    };
  });

  // Sắp xếp theo điểm tiềm năng cao nhất
  scored.sort((a, b) => b.score - a.score);

  // Chọn top 10 - 15 sản phẩm hàng đầu cho khung giờ vàng
  return scored.slice(0, 15);
}

/**
 * Lấy danh sách sản phẩm Flash Sale hiệu lực dựa trên cấu hình (Thủ công hoặc AI)
 * Đảm bảo luôn trả về ít nhất 8 - 12 sản phẩm KHÔNG TRÙNG LẶP
 */
export function getEffectiveFlashSaleProducts(allProducts = []) {
  const pool = getUnifiedCatalogPool(allProducts);
  if (pool.length === 0) return [];
  const config = getFlashSaleConfig();

  if (!config.isEnabled) {
    return [];
  }

  // Dùng Map theo ID sản phẩm để đảm bảo TUYỆT ĐỐI KHÔNG TRÙNG LẶP SẢN PHẨM
  const resultMap = new Map();

  // 1. Chế độ Admin chọn thủ công (MANUAL): Ưu tiên hiển thị trước các sản phẩm Admin chỉ định
  if (config.mode === 'MANUAL' && Array.isArray(config.selectedProductIds) && config.selectedProductIds.length > 0) {
    const selectedSet = new Set(config.selectedProductIds.map(String));
    pool.forEach((p) => {
      const pid = String(p.id);
      if (selectedSet.has(pid) && !resultMap.has(pid)) {
        resultMap.set(pid, {
          ...p,
          discountPercent: config.discountPercent || 30,
          isFlashSale: true,
        });
      }
    });
  }

  // 2. Chế độ AI (hoặc tự động backfill bổ sung nếu danh sách thủ công có dưới 8 sản phẩm)
  if (resultMap.size < 8) {
    const aiResults = analyzeAndSelectFlashSaleWithAI(pool);
    for (const item of aiResults) {
      const pid = String(item.product.id);
      if (!resultMap.has(pid)) {
        resultMap.set(pid, {
          ...item.product,
          discountPercent: item.recommendedDiscount || config.discountPercent || 30,
          isFlashSale: true,
          aiReason: item.aiReason,
        });
        if (resultMap.size >= 12) break;
      }
    }
  }

  // 3. Fallback dự phòng cuối cùng nếu vẫn thiếu sản phẩm (lấy tiếp từ pool sản phẩm hợp nhất)
  if (resultMap.size < 8) {
    for (const p of pool) {
      const pid = String(p.id);
      if (!resultMap.has(pid)) {
        resultMap.set(pid, {
          ...p,
          discountPercent: config.discountPercent || 25,
          isFlashSale: true,
        });
        if (resultMap.size >= 12) break;
      }
    }
  }

  return Array.from(resultMap.values());
}

/**
 * Tính toán thời gian còn lại chuẩn xác theo giây đến khung giờ vàng tiếp theo
 */
export function calculateSlotRemainingTime(slotHours = [9, 12, 15, 18, 21, 24]) {
  const now = new Date();
  const curHour = now.getHours();
  const nextHour = slotHours.find((h) => h > curHour) || 24;

  const target = new Date(now);
  if (nextHour === 24) {
    target.setHours(23, 59, 59, 999);
  } else {
    target.setHours(nextHour, 0, 0, 0);
  }

  const diffMs = Math.max(0, target.getTime() - now.getTime());
  const totalSecs = Math.floor(diffMs / 1000);

  const hours = Math.floor(totalSecs / 3600);
  const minutes = Math.floor((totalSecs % 3600) / 60);
  const seconds = totalSecs % 60;

  return { hours, minutes, seconds, totalSecs };
}
