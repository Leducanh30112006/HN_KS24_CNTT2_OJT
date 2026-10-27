package com.quickshop.payment.adapter;

import com.quickshop.payment.adapter.dto.BankTransferRequest;
import com.quickshop.payment.adapter.dto.BankTransferResult;

/**
 * Adapter tích hợp cổng chuyển tiền chi hộ ngân hàng (Napas 247, VietQR, Baokim, VNPay Payout).
 */
public interface BankTransferAdapter {

    /**
     * Thực hiện lệnh chuyển tiền tất toán tới tài khoản ngân hàng của Seller
     */
    BankTransferResult transfer(BankTransferRequest request);
}
