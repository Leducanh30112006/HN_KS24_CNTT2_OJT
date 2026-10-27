package com.quickshop.order.service;

import com.quickshop.order.dto.voucher.ApplyVoucherResult;
import com.quickshop.order.dto.voucher.VoucherResponse;

import java.math.BigDecimal;
import java.util.Collection;

public interface VoucherService {

    /**
     * Xác thực và áp dụng voucher cho Shop (Voucher riêng của Shop).
     * Kiểm tra thời hạn, trạng thái, shop_id, min_order_value và trừ lượt sử dụng nguyên tử (atomic).
     *
     * @param code         Mã voucher của shop
     * @param shopId       ID shop mà voucher phải thuộc về
     * @param shopSubtotal Tổng tiền hàng của Shop
     * @return ApplyVoucherResult chứa mức giảm và trạng thái áp dụng
     */
    ApplyVoucherResult applyShopVoucher(String code, Long shopId, BigDecimal shopSubtotal);

    /**
     * Xác thực và áp dụng voucher toàn sàn (Platform Voucher, shop_id = null).
     * Kiểm tra thời hạn, trạng thái, shop_id == null, min_order_value và trừ lượt sử dụng nguyên tử (atomic).
     *
     * @param code                   Mã voucher toàn sàn
     * @param totalMerchandiseAmount Tổng tiền hàng toàn bộ các shop
     * @return ApplyVoucherResult chứa mức giảm và trạng thái áp dụng
     */
    ApplyVoucherResult applyPlatformVoucher(String code, BigDecimal totalMerchandiseAmount);

    /**
     * Hoàn tác lượt sử dụng của 1 voucher (Dùng trong Compensating Transaction khi Saga thất bại hoặc hủy đơn).
     *
     * @param code Mã voucher cần hoàn lại lượt sử dụng
     */
    void releaseVoucherUsage(String code);

    /**
     * Hoàn tác danh sách các voucher đã dùng (Voucher sàn + Voucher các shop) khi Saga rollback.
     *
     * @param platformVoucherCode Mã voucher sàn (nếu có)
     * @param shopVoucherCodes    Tập hợp mã voucher các shop (nếu có)
     */
    void releaseVouchers(String platformVoucherCode, Collection<String> shopVoucherCodes);

    /**
     * Tra cứu chi tiết thông tin voucher theo mã code.
     *
     * @param code Mã voucher
     * @return VoucherResponse
     */
    VoucherResponse getVoucherByCode(String code);

    /**
     * Xem trước (Preview) mức giảm giá của voucher mà không trừ lượt sử dụng.
     * Dùng cho giỏ hàng và màn hình Checkout trước khi bấm Đặt hàng.
     *
     * @param code        Mã voucher
     * @param shopId      ID shop (nếu là voucher shop, null nếu là voucher sàn)
     * @param orderAmount Tiền hàng áp dụng
     * @return ApplyVoucherResult
     */
    ApplyVoucherResult previewVoucherDiscount(String code, Long shopId, BigDecimal orderAmount);
}
