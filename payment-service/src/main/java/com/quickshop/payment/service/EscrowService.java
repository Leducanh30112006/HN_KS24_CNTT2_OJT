package com.quickshop.payment.service;

import com.quickshop.payment.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface EscrowService {

    /**
     * Tạm giữ tiền (Escrow Hold) cho một Sub-Order sau khi thanh toán thành công
     */
    EscrowResponse holdEscrow(HoldEscrowRequest request);

    /**
     * Giải phóng tiền Escrow cho Seller (khấu trừ 3% hoa hồng sàn)
     * Điều kiện: Buyer xác nhận nhận hàng HOẶC đơn DELIVERED quá 3 ngày không có khiếu nại
     */
    EscrowResponse releaseEscrow(ReleaseEscrowRequest request);

    /**
     * Hoàn tiền Escrow lại cho Buyer do đơn bị hủy hoặc khiếu nại đổi trả được chấp thuận
     */
    EscrowResponse refundEscrow(RefundEscrowRequest request);

    /**
     * Đóng băng giải phóng Escrow khi phát sinh khiếu nại / tranh chấp từ Buyer
     */
    EscrowResponse freezeEscrowForDispute(Long subOrderId, String reason);

    /**
     * Mở đóng băng Escrow khi khiếu nại tranh chấp được giải quyết bác bỏ
     */
    EscrowResponse unfreezeEscrow(Long subOrderId);

    /**
     * Cập nhật thời điểm đơn hàng giao thành công (DELIVERED) làm mốc tính 3 ngày tự động giải phóng
     */
    EscrowResponse markSubOrderDelivered(Long subOrderId, LocalDateTime deliveredAt);

    /**
     * Quét và tự động giải phóng các Escrow đủ điều kiện (giao thành công quá 3 ngày không khiếu nại)
     */
    int autoReleaseEligibleEscrows();

    /**
     * Tra cứu thông tin Escrow theo subOrderId
     */
    EscrowResponse getEscrowBySubOrderId(Long subOrderId);

    /**
     * Tra cứu danh sách Escrow theo masterOrderId
     */
    List<EscrowResponse> getEscrowsByMasterOrderId(Long masterOrderId);
}
