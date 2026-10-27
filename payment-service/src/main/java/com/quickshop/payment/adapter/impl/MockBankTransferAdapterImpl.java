package com.quickshop.payment.adapter.impl;

import com.quickshop.payment.adapter.BankTransferAdapter;
import com.quickshop.payment.adapter.dto.BankTransferRequest;
import com.quickshop.payment.adapter.dto.BankTransferResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * =========================================================================================
 * [MÔ PHỎNG / SIMULATOR] MOCK BANK TRANSFER ADAPTER
 * -----------------------------------------------------------------------------------------
 * Do hệ thống hiện tại đang trong giai đoạn phát triển và chưa ký hợp đồng kết nối API
 * chi hộ (Payout API) với ngân hàng đối tác thực tế (Vietcombank, MB, Napas 247...),
 * Adapter này đóng vai trò GIẢ LẬP kết quả chuyển khoản ngân hàng phục vụ kiểm thử luồng.
 *
 * Khi dự án tích hợp đối tác ngân hàng thật, chỉ cần thay thế Bean triển khai interface
 * BankTransferAdapter mà không ảnh hưởng tới toàn bộ nghiệp vụ Sổ cái hay Quản lý Ví.
 * =========================================================================================
 */
@Slf4j
@Component
public class MockBankTransferAdapterImpl implements BankTransferAdapter {

    @Override
    public BankTransferResult transfer(BankTransferRequest request) {
        log.info("[MOCK BANK TRANSFER SIMULATOR] Bắt đầu mô phỏng lệnh chuyển tiền tất toán:");
        log.info("  -> Lệnh rút tiền ID: {}", request.getWithdrawalId());
        log.info("  -> Shop ID: {}", request.getShopId());
        log.info("  -> Số tiền: {} VND", request.getAmount());
        log.info("  -> Ngân hàng thụ hưởng: {}", request.getBankCode());
        log.info("  -> Số tài khoản: {}", request.getBankAccountNumber());
        log.info("  -> Tên chủ tài khoản: {}", request.getBankAccountName());

        // Sinh mã tham chiếu giao dịch ngân hàng giả lập Napas 24/7
        String mockBankRef = "MOCK_NAPAS247_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[MOCK BANK TRANSFER SIMULATOR] Chuyển tiền THÀNH CÔNG! Mã giao dịch liên ngân hàng: {}", mockBankRef);

        return BankTransferResult.builder()
                .success(true)
                .bankTransactionRef(mockBankRef)
                .responseCode("00")
                .message("Chuyển tiền liên ngân hàng 24/7 thành công (Mô phỏng Napas)")
                .transferredAt(LocalDateTime.now())
                .build();
    }
}
