package com.quickshop.catalog.entity;

/**
 * Trạng thái vòng đời sản phẩm trong Catalog:
 * - DRAFT: Bản nháp do người bán (Seller) tạo, chưa gửi duyệt
 * - PENDING: Đang chờ Quản trị viên (Admin) duyệt
 * - APPROVED: Đã được duyệt, hiển thị công khai trên Storefront
 * - REJECTED: Bị từ chối phê duyệt (kèm lý do)
 *
 * Cung cấp thêm PENDING_APPROVAL và ACTIVE để đảm bảo tính tương thích ngược (Backward compatibility).
 */
public enum ProductStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,

    // Tương thích ngược với seed data và các dịch vụ liên kết
    PENDING_APPROVAL,
    ACTIVE;

    public boolean isApproved() {
        return this == APPROVED || this == ACTIVE;
    }

    public boolean isPending() {
        return this == PENDING || this == PENDING_APPROVAL;
    }
}
