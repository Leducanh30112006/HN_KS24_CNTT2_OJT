package com.quickshop.order.dto.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Yêu cầu thanh toán và tạo đơn hàng (Checkout Request).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest implements Serializable {

    /**
     * Phương thức thanh toán: COD, VNPAY, MOMO, CREDIT_CARD
     */
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    /**
     * Tên người nhận hàng
     */
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    /**
     * Số điện thoại người nhận hàng
     */
    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    private String recipientPhone;

    /**
     * Địa chỉ giao hàng chi tiết (số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành)
     */
    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    /**
     * Ghi chú đơn hàng của người mua
     */
    private String note;

    /**
     * Mã voucher giảm giá toàn sàn (tùy chọn)
     */
    private String voucherCode;

    /**
     * Mã voucher riêng cho từng Shop: Map<shopId, voucherCode> (tùy chọn)
     */
    @Builder.Default
    private Map<Long, String> shopVouchers = new HashMap<>();

    /**
     * Danh sách mặt hàng được chọn checkout.
     * Nếu để trống, hệ thống sẽ tự động lấy toàn bộ các mặt hàng đang có trong User Cart trên Redis.
     */
    @Valid
    @Builder.Default
    private List<CheckoutItemDto> items = new ArrayList<>();
}

