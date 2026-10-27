package com.quickshop.order.dto.checkout;

import com.quickshop.order.entity.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest implements Serializable {

    /**
     * Khóa Idempotency để chống lặp đơn hàng khi client gửi lại request.
     * Có thể truyền tại đây hoặc qua HTTP header X-Idempotency-Key.
     */
    private String requestId;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String shippingAddress;

    private String recipientName;

    private String recipientPhone;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    /**
     * Mã voucher áp dụng toàn sàn (shop_id = null)
     */
    private String voucherCode;

    /**
     * Map các voucher riêng theo từng shop: Map<shopId, voucherCode>
     */
    private Map<Long, String> shopVoucherCodes;

    /**
     * Danh sách mặt hàng được tick chọn để checkout.
     * Nếu để trống, hệ thống sẽ checkout toàn bộ mặt hàng hiện có trong giỏ hàng.
     */
    @Valid
    private List<CheckoutItemRequest> items;
}
