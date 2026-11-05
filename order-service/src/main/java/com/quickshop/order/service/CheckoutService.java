package com.quickshop.order.service;

import com.quickshop.order.dto.checkout.CheckoutRequest;
import com.quickshop.order.dto.checkout.CheckoutResponse;

/**
 * Dịch vụ xử lý quy trình thanh toán và tạo đơn hàng (Checkout Service).
 * Hỗ trợ mô hình sàn thương mại điện tử Multi-Vendor:
 * - Tách giỏ hàng thành MasterOrder và các SubOrder theo từng Shop
 * - Lưu vết Snapshot (tên sản phẩm, biến thể, giá bán, mã SKU) tại thời điểm mua
 * - Tự động dọn dẹp mặt hàng đã thanh toán khỏi Redis Cart
 */
public interface CheckoutService {

    /**
     * Thực hiện thanh toán và tạo đơn hàng hoàn chỉnh (MasterOrder + SubOrders + OrderItems).
     *
     * @param userId  ID người mua đã xác thực
     * @param request Thông tin thanh toán, địa chỉ giao hàng và danh sách mặt hàng
     * @return CheckoutResponse thông tin đơn hàng đã tạo
     */
    CheckoutResponse checkout(Long userId, CheckoutRequest request);

    /**
     * Xem trước thông tin checkout (tính toán chi phí, phí ship theo từng shop và tổng tiền cần trả)
     * trước khi người dùng xác nhận đặt hàng.
     *
     * @param userId  ID người mua
     * @param request Thông tin mặt hàng và mã voucher
     * @return CheckoutResponse xem trước chi tiết
     */
    CheckoutResponse previewCheckout(Long userId, CheckoutRequest request);
}

