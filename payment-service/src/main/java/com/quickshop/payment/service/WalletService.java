package com.quickshop.payment.service;

import com.quickshop.payment.dto.WalletResponse;
import com.quickshop.payment.dto.WithdrawalRequestDto;
import com.quickshop.payment.entity.Withdrawal;

import java.util.List;

public interface WalletService {

    /**
     * Lấy thông tin số dư ví của Shop (khởi tạo nếu chưa tồn tại)
     * Kiểm tra quyền: Chỉ Shop sở hữu hoặc Admin mới có quyền xem ví
     */
    WalletResponse getWalletByShopId(Long shopId, Long currentUserId, String currentUserRole);

    /**
     * Seller tạo yêu cầu rút tiền từ Ví khả dụng (đóng băng số dư chờ duyệt)
     * Hỗ trợ chống request lặp và kiểm tra không cho số dư âm
     */
    Withdrawal requestWithdrawal(WithdrawalRequestDto request, Long currentUserId, String currentUserRole);

    /**
     * Admin phê duyệt lệnh rút tiền và thực hiện tất toán qua ngân hàng (chuyển sang COMPLETED)
     */
    Withdrawal approveWithdrawal(Long withdrawalId, Long adminId, String currentUserRole);

    /**
     * Admin từ chối lệnh rút tiền (hoàn lại số dư khả dụng cho Seller)
     */
    Withdrawal rejectWithdrawal(Long withdrawalId, Long adminId, String reason, String currentUserRole);

    /**
     * Lấy danh sách lịch sử rút tiền của Shop
     */
    List<Withdrawal> getWithdrawalsByShopId(Long shopId, Long currentUserId, String currentUserRole);

    /**
     * Tra cứu chi tiết một lệnh rút tiền
     */
    Withdrawal getWithdrawalById(Long withdrawalId, Long currentUserId, String currentUserRole);
}
