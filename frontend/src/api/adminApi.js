import axiosClient from './axiosClient';

/**
 * Chuẩn hóa thông báo lỗi trả về từ Backend (Spring ApiResponse / Validation / Gateway JWT) hoặc lỗi mạng.
 */
export function extractApiErrorMessage(err, fallbackMsg = 'Không thể thực hiện thao tác trên máy chủ API.') {
  if (!err) return fallbackMsg;
  if (typeof err === 'string') return err;

  const status = err?.status || err?.response?.status;
  const payload = err?.response?.data || err;

  if (payload?.errors && typeof payload.errors === 'object') {
    const fieldMessages = Object.values(payload.errors).filter(Boolean);
    if (fieldMessages.length > 0) {
      return fieldMessages.join(' • ');
    }
  }

  if (typeof payload?.message === 'string' && payload.message.trim()) {
    return payload.message.trim();
  }

  if (typeof payload?.error === 'string' && payload.error.trim()) {
    return payload.error.trim();
  }

  if (status === 401) {
    return 'Phiên đăng nhập Quản trị viên đã hết hạn (401). Vui lòng đăng nhập lại.';
  }
  if (status === 403) {
    return 'Tài khoản hiện tại không có quyền Quản trị viên ROLE_ADMIN (403 Forbidden).';
  }
  if (status === 404) {
    return 'Không tìm thấy endpoint hoặc bản ghi tương ứng trên máy chủ (404).';
  }
  if (status >= 500) {
    return `Máy chủ gặp sự cố nội bộ (${status}). Vui lòng thử lại sau.`;
  }
  if (err?.code === 'ERR_NETWORK' || err?.message === 'Network Error') {
    return 'Không thể kết nối tới API Gateway (mất kết nối hoặc dịch vụ chưa khởi chạy).';
  }

  return err?.message || fallbackMsg;
}

/**
 * Chuẩn hóa dữ liệu gian hàng từ Identity Service (ShopResponse) sang cấu trúc hiển thị nhất quán cho Admin UI.
 */
export function normalizeShopDto(raw) {
  if (!raw) return null;

  const bankParts = [raw.bankCode, raw.bankAccountName].filter(Boolean).join(' - ');
  const resolvedBankAccount =
    raw.bankAccount ||
    (raw.bankAccountNumber
      ? `${raw.bankAccountNumber}${bankParts ? ` (${bankParts})` : ''}`
      : 'Chưa liên kết tài khoản');

  return {
    ...raw,
    id: raw.id,
    ownerId: raw.ownerId,
    name: raw.name || raw.shopName || `Gian hàng #${raw.id}`,
    shopName: raw.shopName || raw.name || `Gian hàng #${raw.id}`,
    ownerName: raw.ownerName || raw.bankAccountName || (raw.ownerId ? `Chủ shop #${raw.ownerId}` : 'Chưa cập nhật'),
    ownerEmail: raw.ownerEmail || (raw.ownerId ? `User ID #${raw.ownerId}` : 'Chưa cập nhật'),
    ownerPhone: raw.ownerPhone || raw.phoneNumber || 'Chưa cập nhật',
    idCardNumber: raw.idCardNumber || raw.citizenId || 'Chưa cung cấp',
    taxCode: raw.taxCode || 'Hộ kinh doanh cá thể',
    bankAccount: resolvedBankAccount,
    warehouseAddress: raw.warehouseAddress || 'Chưa cập nhật địa chỉ kho',
    businessType: raw.businessType || 'INDIVIDUAL',
    status: raw.status || 'PENDING_APPROVAL',
    rejectionReason: raw.rejectionReason || null,
    isMall: Boolean(raw.isMall || raw.businessType === 'MALL' || raw.businessType === 'OFFICIAL'),
    productCount: raw.productCount ?? null,
    totalRevenue: raw.totalRevenue ?? null,
    rating: raw.rating ?? null,
    registeredAt: raw.registeredAt || (raw.createdAt ? String(raw.createdAt).slice(0, 10) : ''),
  };
}

function assertApiSuccess(res, defaultMsg) {
  if (!res || res.success === false) {
    throw new Error(res?.message || defaultMsg);
  }
  return res;
}

/**
 * Lấy danh sách sản phẩm phục vụ Admin kiểm duyệt ở mọi trạng thái (không dùng Redis storefront cache).
 */
export async function fetchAdminProducts({ status, keyword, categoryId, page = 0, size = 50 } = {}) {
  const params = { page, size };
  if (status && status !== 'ALL') params.status = status;
  if (keyword?.trim()) params.keyword = keyword.trim();
  if (categoryId && categoryId !== 'ALL') params.categoryId = categoryId;

  const res = assertApiSuccess(
    await axiosClient.get('/api/catalog/products/admin', { params }),
    'Không thể tải danh sách kiểm duyệt sản phẩm.'
  );

  const pageData = res.data || {};
  return {
    items: Array.isArray(pageData.content) ? pageData.content : [],
    pageInfo: {
      page: pageData.page ?? 0,
      size: pageData.size ?? size,
      totalElements: pageData.totalElements ?? 0,
      totalPages: pageData.totalPages ?? 1,
    },
  };
}

/**
 * Admin phê duyệt hoặc từ chối sản phẩm (APPROVED / REJECTED kèm lý do).
 */
export async function updateAdminProductStatus(productId, { status, reason }) {
  const res = assertApiSuccess(
    await axiosClient.patch(`/api/catalog/products/${productId}/status`, { status, reason }),
    'Cập nhật trạng thái kiểm duyệt sản phẩm thất bại.'
  );
  return res.data;
}

/**
 * Admin xóa vĩnh viễn sản phẩm vi phạm khỏi Catalog Service.
 */
export async function deleteAdminProduct(productId) {
  const res = assertApiSuccess(
    await axiosClient.delete(`/api/catalog/products/${productId}`),
    'Xóa sản phẩm thất bại.'
  );
  return res.data;
}

/**
 * Lấy danh sách gian hàng từ Identity Service (toàn bộ hoặc lọc theo trạng thái KYC).
 */
export async function fetchAdminShops(statusFilter) {
  const statusMap = {
    PENDING: 'PENDING_APPROVAL',
    PENDING_APPROVAL: 'PENDING_APPROVAL',
    ACTIVE: 'ACTIVE',
    SUSPENDED: 'SUSPENDED',
    REJECTED: 'REJECTED',
  };

  const mappedStatus = statusFilter && statusFilter !== 'ALL' ? statusMap[statusFilter] : null;
  const endpoint = mappedStatus ? `/api/shops/status/${mappedStatus}` : '/api/shops';

  const res = assertApiSuccess(
    await axiosClient.get(endpoint),
    'Không thể tải danh sách gian hàng & hồ sơ KYC.'
  );

  const list = Array.isArray(res.data) ? res.data : [];
  return list.map(normalizeShopDto);
}

/**
 * Admin phê duyệt hồ sơ mở gian hàng KYC -> ACTIVE và cấp ROLE_SELLER cho chủ shop.
 */
export async function approveAdminShop(shopId) {
  const res = assertApiSuccess(
    await axiosClient.put(`/api/shops/${shopId}/approve`),
    'Phê duyệt gian hàng KYC thất bại.'
  );
  return normalizeShopDto(res.data);
}

/**
 * Admin từ chối hồ sơ mở gian hàng KYC kèm lý do phản hồi.
 */
export async function rejectAdminShop(shopId, reason) {
  const res = assertApiSuccess(
    await axiosClient.put(`/api/shops/${shopId}/reject`, { reason }),
    'Từ chối hồ sơ gian hàng KYC thất bại.'
  );
  return normalizeShopDto(res.data);
}

/**
 * Admin tạm khóa gian hàng đang hoạt động (SUSPENDED).
 */
export async function suspendAdminShop(shopId, reason) {
  const res = assertApiSuccess(
    await axiosClient.put(`/api/shops/${shopId}/suspend`, { reason }),
    'Tạm khóa gian hàng thất bại.'
  );
  return normalizeShopDto(res.data);
}

/**
 * Admin mở khóa hoạt động trở lại cho gian hàng bị tạm khóa/từ chối (ACTIVE).
 */
export async function activateAdminShop(shopId) {
  const res = assertApiSuccess(
    await axiosClient.put(`/api/shops/${shopId}/activate`),
    'Mở khóa gian hàng thất bại.'
  );
  return normalizeShopDto(res.data);
}
