package com.quickshop.order.service;

import com.quickshop.order.dto.voucher.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service xử lý toàn diện logic nghiệp vụ Voucher:
 * - Kiểm tra điều kiện hợp lệ (trạng thái, hạn dùng, lượt dùng, giá trị đơn tối thiểu, shop áp dụng).
 * - Tính toán mức giảm giá chi tiết (theo % có trần hoặc số tiền cố định).
 * - Hỗ trợ cơ chế Voucher kép (Voucher sàn + Voucher từng Shop).
 * - Ghi nhận lịch sử sử dụng và quản lý lượt dùng.
 */
public interface VoucherService {

    /**
     * Xác thực và tính toán số tiền giảm giá của một voucher cụ thể (Platform hoặc Shop voucher).
     *
     * @param code Mã voucher
     * @param shopId ID shop áp dụng (để null nếu là voucher toàn sàn)
     * @param orderSubtotal Tiền hàng cần tính chiết khấu
     * @return Kết quả xác thực và số tiền giảm
     */
    VoucherValidationResponse validateAndCalculateVoucher(String code, Long shopId, BigDecimal orderSubtotal);

    /**
     * Áp dụng và tính toán giảm giá cho cả combo Voucher sàn + các Voucher Shop.
     *
     * @param request Yêu cầu áp dụng voucher kép
     * @return Kết quả tính toán chi tiết từng voucher và tổng giảm giá
     */
    VoucherApplyResponse calculateDiscounts(VoucherApplyRequest request);

    /**
     * Ghi nhận việc sử dụng voucher vào hệ thống (tạo bản ghi VoucherUsage và cập nhật usedCount).
     *
     * @param voucherCode Mã voucher
     * @param userId ID người dùng
     * @param masterOrderId ID đơn hàng tổng
     * @param subOrderId ID đơn hàng con (null nếu là voucher sàn)
     * @param discountAmount Số tiền đã giảm
     */
    void recordUsage(String voucherCode, Long userId, Long masterOrderId, Long subOrderId, BigDecimal discountAmount);

    /**
     * Lấy danh sách các voucher hợp lệ áp dụng được cho giỏ hàng hoặc shop.
     *
     * @param shopId ID shop (null để lấy voucher sàn)
     * @param subtotal Tiền hàng tối thiểu để lọc
     * @return Danh sách voucher hợp lệ
     */
    List<VoucherResponse> getApplicableVouchers(Long shopId, BigDecimal subtotal);

    /**
     * Lấy thông tin chi tiết một voucher theo mã.
     *
     * @param code Mã voucher
     * @return Thông tin voucher
     */
    VoucherResponse getVoucherByCode(String code);
}
