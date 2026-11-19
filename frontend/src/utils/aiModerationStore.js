import { stripHtml } from './sanitizeHtml';

const AI_MODERATION_STORAGE_KEY = 'quickshop_ai_moderated_products';
const SELLER_PRODUCTS_STORAGE_KEY = 'quickshop_seller_products';
const AI_AUTO_APPROVE_ENABLED_KEY = 'quickshop_ai_auto_approve_enabled';

/**
 * Kiểm tra xem chế độ AI tự động duyệt sản phẩm có đang bật hay không
 * Mặc định: true (Bật AI tự động duyệt)
 */
export function isAiAutoApproveEnabled() {
  try {
    const val = localStorage.getItem(AI_AUTO_APPROVE_ENABLED_KEY);
    if (val === null) return true; // Mặc định BẬT
    return val === 'true';
  } catch {
    return true;
  }
}

/**
 * Bật/tắt chế độ AI tự động duyệt sản phẩm từ Admin
 */
export function setAiAutoApproveEnabled(enabled) {
  try {
    localStorage.setItem(AI_AUTO_APPROVE_ENABLED_KEY, String(Boolean(enabled)));
    window.dispatchEvent(new CustomEvent('quickshop_ai_auto_approve_toggled', { detail: Boolean(enabled) }));
  } catch (e) {
    console.warn('Không thể lưu trạng thái AI Auto Approve vào localStorage:', e);
  }
  return Boolean(enabled);
}

const BANNED_KEYWORDS = [
  'hàng giả',
  'hàng nhái',
  'siêu fake',
  'rep 1:1',
  'vũ khí',
  'súng',
  'đạn',
  'ma túy',
  'cờ bạc',
  'cá độ',
  'lừa đảo',
  'pháo nổ',
  'thuốc lá điện tử',
  'rượu lậu',
  '18+',
  'khiêu dâm',
];

/**
 * Bộ máy Kiểm duyệt Sản phẩm Tự động bằng AI (AI Auto-Moderation Engine)
 * Chấm điểm chuẩn sàn TMĐT (thang 100) dựa trên 5 tiêu chí:
 * 1. Kiểm duyệt từ khóa cấm / chính sách an toàn
 * 2. Tiêu đề chuẩn SEO & rõ ràng
 * 3. Giá bán & Tồn kho SKU hợp lệ
 * 4. Hình ảnh sản phẩm hợp lệ
 * 5. Mô tả chi tiết đầy đủ thông tin
 */
export function evaluateProductWithAI(product) {
  const name = String(product?.name || '').trim();
  const plainDesc = stripHtml(product?.description || '').trim();
  const combined = `${name} ${plainDesc}`.toLowerCase();
  const mainImage = String(product?.mainImage || '').trim();
  const variants = Array.isArray(product?.variants) ? product.variants : [];
  const firstPrice = Number(variants[0]?.price ?? product?.minPrice ?? 0);
  const totalStock = variants.reduce((sum, v) => sum + (Number(v.stock) || 0), 0);

  const checks = [];
  const flaggedReasons = [];
  let score = 0;

  // 1. Chính sách & Từ khóa cấm (30 điểm)
  const matchedBanned = BANNED_KEYWORDS.filter((kw) => combined.includes(kw));
  if (matchedBanned.length === 0) {
    score += 30;
    checks.push({
      label: 'Chính sách & Từ khóa cấm',
      passed: true,
      note: 'An toàn — không phát hiện nội dung cấm hoặc hàng giả',
    });
  } else {
    const reason = `Phát hiện từ khóa vi phạm chính sách sàn: "${matchedBanned.join(', ')}"`;
    flaggedReasons.push(reason);
    checks.push({
      label: 'Chính sách & Từ khóa cấm',
      passed: false,
      note: reason,
    });
  }

  // 2. Chất lượng tên sản phẩm (20 điểm)
  if (name.length >= 10) {
    score += 20;
    checks.push({
      label: 'Tiêu đề sản phẩm chuẩn SEO',
      passed: true,
      note: `Tiêu đề rõ ràng (${name.length} ký tự)`,
    });
  } else if (name.length >= 5) {
    score += 14;
    checks.push({
      label: 'Tiêu đề sản phẩm chuẩn SEO',
      passed: true,
      note: 'Tiêu đề đạt mức tối thiểu, có thể bổ sung thêm thông số',
    });
  } else {
    const reason = 'Tên sản phẩm quá ngắn (< 5 ký tự), chưa đạt chuẩn hiển thị';
    flaggedReasons.push(reason);
    checks.push({
      label: 'Tiêu đề sản phẩm chuẩn SEO',
      passed: false,
      note: reason,
    });
  }

  // 3. Giá bán & Tồn kho SKU (20 điểm)
  if (firstPrice >= 1000 && totalStock >= 1) {
    score += 20;
    checks.push({
      label: 'Giá bán & Tồn kho SKU',
      passed: true,
      note: `Giá ${firstPrice.toLocaleString('vi-VN')}₫ • Tồn kho ${totalStock} SP`,
    });
  } else {
    const reason =
      firstPrice < 1000
        ? 'Giá bán bất thường (< 1.000₫) — cần Admin kiểm tra'
        : 'Tồn kho SKU đang bằng 0';
    flaggedReasons.push(reason);
    checks.push({
      label: 'Giá bán & Tồn kho SKU',
      passed: false,
      note: reason,
    });
  }

  // 4. Hình ảnh đại diện (15 điểm)
  if (mainImage.length > 0 && (mainImage.startsWith('http') || mainImage.startsWith('data:image/') || mainImage.startsWith('/api/'))) {
    score += 15;
    checks.push({
      label: 'Hình ảnh đại diện sản phẩm',
      passed: true,
      note: mainImage.startsWith('data:image/') || mainImage.includes('/api/catalog/media/')
        ? 'Ảnh tải lên từ thiết bị hợp lệ'
        : 'Ảnh URL hợp lệ, rõ nét',
    });
  } else {
    score += 8;
    checks.push({
      label: 'Hình ảnh đại diện sản phẩm',
      passed: true,
      note: 'Sử dụng ảnh mặc định — khuyến nghị tải ảnh thật từ máy',
    });
  }

  // 5. Mô tả chi tiết sản phẩm (15 điểm)
  if (plainDesc.length >= 25) {
    score += 15;
    checks.push({
      label: 'Nội dung mô tả chi tiết',
      passed: true,
      note: `Mô tả đầy đủ thông tin (${plainDesc.length} ký tự)`,
    });
  } else {
    score += 10;
    checks.push({
      label: 'Nội dung mô tả chi tiết',
      passed: true,
      note: 'Mô tả ngắn — khuyến nghị dùng AI Gemini bổ sung thông số',
    });
  }

  const isApproved = matchedBanned.length === 0 && flaggedReasons.length === 0 && score >= 75;

  return {
    decision: isApproved ? 'AI_APPROVED' : 'AI_FLAGGED',
    productStatus: isApproved ? 'APPROVED' : 'PENDING',
    score: Math.min(100, score),
    checkedAt: new Date().toISOString(),
    checkedBy: 'QuickShop AI Moderator v2.5',
    summary: isApproved
      ? `AI đã duyệt tự động lên sàn (${Math.min(100, score)}/100 điểm) — Đã lưu nhật ký sang Admin để giám sát.`
      : `AI gắn cờ cảnh báo (${score}/100 điểm) — Chuyển hàng đợi Admin kiểm duyệt thủ công.`,
    checks,
    flaggedReasons,
    adminReviewed: false,
    adminActionNote: null,
  };
}

/**
 * Đọc danh sách sản phẩm đã qua AI Auto-Moderation (dùng chung giữa Seller & Admin)
 */
export function getAiModeratedProducts() {
  try {
    const raw = localStorage.getItem(AI_MODERATION_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/**
 * Lưu sản phẩm mới kèm hồ sơ chấm điểm AI để Admin Portal theo dõi & quản lý ngay lập tức
 */
export function saveAiModeratedProduct(productWithAi) {
  const existing = getAiModeratedProducts();
  const filtered = existing.filter((p) => String(p.id) !== String(productWithAi.id));
  const next = [productWithAi, ...filtered];
  try {
    localStorage.setItem(AI_MODERATION_STORAGE_KEY, JSON.stringify(next));
    window.dispatchEvent(new CustomEvent('quickshop_ai_moderation_updated', { detail: productWithAi }));
  } catch (e) {
    console.warn('Không thể lưu nhật ký AI Moderation vào localStorage:', e);
  }
  return next;
}

/**
 * Khi Admin hậu kiểm (Phê duyệt / Thu hồi gỡ khỏi sàn / Từ chối), đồng bộ 2 chiều sang cả Admin log & Seller Portal
 */
export function syncAdminDecisionToAiStore(productId, newStatus, adminNote = '') {
  const now = new Date().toISOString();
  const aiList = getAiModeratedProducts();
  const nextAiList = aiList.map((item) => {
    if (String(item.id) !== String(productId)) return item;
    return {
      ...item,
      status: newStatus,
      rejectionReason: newStatus === 'REJECTED' ? adminNote : null,
      aiModeration: {
        ...(item.aiModeration || {}),
        adminReviewed: true,
        adminReviewedAt: now,
        adminDecision: newStatus,
        adminActionNote: adminNote || (newStatus === 'APPROVED' ? 'Admin đã xác nhận hợp lệ' : 'Admin đã thu hồi / từ chối'),
      },
    };
  });

  try {
    localStorage.setItem(AI_MODERATION_STORAGE_KEY, JSON.stringify(nextAiList));
  } catch {}

  // Đồng bộ ngược sang danh sách sản phẩm của Seller Portal
  try {
    const rawSeller = localStorage.getItem(SELLER_PRODUCTS_STORAGE_KEY);
    if (rawSeller) {
      const sellerList = JSON.parse(rawSeller);
      if (Array.isArray(sellerList)) {
        const updatedSeller = sellerList.map((p) =>
          String(p.id) === String(productId)
            ? {
                ...p,
                status: newStatus,
                rejectionReason: newStatus === 'REJECTED' ? adminNote : null,
                aiModeration: {
                  ...(p.aiModeration || {}),
                  adminReviewed: true,
                  adminReviewedAt: now,
                  adminDecision: newStatus,
                  adminActionNote: adminNote,
                },
              }
            : p
        );
        localStorage.setItem(SELLER_PRODUCTS_STORAGE_KEY, JSON.stringify(updatedSeller));
      }
    }
    window.dispatchEvent(new CustomEvent('quickshop_ai_moderation_updated', { detail: { productId, newStatus } }));
  } catch {}

  return nextAiList;
}

/**
 * Xóa sản phẩm khỏi AI Moderation store khi Admin hoặc Seller xóa vĩnh viễn
 */
export function removeAiModeratedProduct(productId) {
  const next = getAiModeratedProducts().filter((p) => String(p.id) !== String(productId));
  try {
    localStorage.setItem(AI_MODERATION_STORAGE_KEY, JSON.stringify(next));
  } catch {}
  return next;
}
