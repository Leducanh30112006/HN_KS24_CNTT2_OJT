package com.quickshop.payment.constant;

public final class LedgerAccount {

    private LedgerAccount() {}

    // Tài khoản Khách hàng / Cổng thanh toán
    public static final String CUSTOMER_PAYMENT = "CUSTOMER_PAYMENT";
    public static final String CUSTOMER_REFUND = "CUSTOMER_REFUND";

    // Tài khoản Trung gian Tạm giữ của Sàn (Escrow)
    public static final String SYSTEM_ESCROW = "SYSTEM_ESCROW";

    // Tài khoản Doanh thu Sàn (Hoa hồng)
    public static final String PLATFORM_REVENUE = "PLATFORM_REVENUE";

    // Tài khoản Tiền chờ rút của Seller
    public static final String WITHDRAWAL_PENDING = "WITHDRAWAL_PENDING";

    // Tài khoản Ngân hàng tất toán
    public static final String BANK_SETTLEMENT = "BANK_SETTLEMENT";

    // Tiền tố tài khoản Ví Seller
    public static final String SELLER_WALLET_PREFIX = "SELLER_WALLET_";

    public static String sellerWallet(Long shopId) {
        return SELLER_WALLET_PREFIX + shopId;
    }
}
