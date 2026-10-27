package com.quickshop.payment.e2e;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.config.TestPaymentIntegrationConfig;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.*;
import com.quickshop.payment.entity.enums.*;
import com.quickshop.payment.repository.*;
import com.quickshop.payment.scheduler.EscrowAutoReleaseScheduler;
import com.quickshop.payment.service.DoubleEntryLedgerService;
import com.quickshop.payment.service.EscrowService;
import com.quickshop.payment.service.VNPayService;
import com.quickshop.payment.service.WalletService;
import com.quickshop.payment.util.VNPayUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestPaymentIntegrationConfig.class)
@DisplayName("E2E Integration Tests - Payment & Wallet Service (Flows: 6, 7, 8, 10, 12)")
class PaymentServiceE2ETest {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private DoubleEntryLedgerService doubleEntryLedgerService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private EscrowAutoReleaseScheduler escrowAutoReleaseScheduler;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private EscrowAccountRepository escrowAccountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private net.javacrumbs.shedlock.core.LockProvider lockProvider;

    private static final String TEST_SECRET = "TEST_VNPAY_SECRET_KEY_2026_HMACSHA512";
    private static final String TEST_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final Long TEST_SHOP_ID = 101L;
    private static final Long TEST_USER_ID = 202L;

    @BeforeEach
    void setUp() {
        TestPaymentIntegrationConfig.clearAll();
        TestPaymentIntegrationConfig.configureRedisMock(stringRedisTemplate);

        net.javacrumbs.shedlock.core.SimpleLock simpleLock = org.mockito.Mockito.mock(net.javacrumbs.shedlock.core.SimpleLock.class);
        org.mockito.Mockito.when(lockProvider.lock(org.mockito.ArgumentMatchers.any(net.javacrumbs.shedlock.core.LockConfiguration.class)))
                .thenReturn(Optional.of(simpleLock));

        // Mock rabbitTemplate để lưu các message phát ra
        org.mockito.Mockito.doAnswer(invocation -> {
            Object msg = invocation.getArgument(2);
            if (msg != null) {
                TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.add(msg);
            }
            return null;
        }).when(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class)
        );

        // Dọn dẹp dữ liệu test độc lập
        ledgerEntryRepository.deleteAll();
        withdrawalRepository.deleteAll();
        escrowAccountRepository.deleteAll();
        walletRepository.deleteAll();
        paymentTransactionRepository.deleteAll();
    }

    // =========================================================================
    // LUỒNG 6: VNPAY IPN THÀNH CÔNG, SAI CHỮ KÝ VÀ GỬI LẶP
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 6 - E2E] VNPay IPN: Thành công cập nhật DB và phát event, Sai chữ ký bị từ chối, Gửi lặp an toàn")
    void testVNPayIpn_Success_InvalidSignature_And_DuplicateDelivery() {
        String txnRef = "TXN_E2E_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        BigDecimal amount = new BigDecimal("500000");

        // 1. Tạo bản ghi giao dịch thanh toán PENDING trong database
        PaymentTransaction txn = paymentTransactionRepository.save(PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(9999L)
                .amount(amount)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build());

        // 2. Kịch bản A: Sai chữ ký HMAC-SHA512
        Map<String, String> invalidParams = new HashMap<>();
        invalidParams.put("vnp_TxnRef", txnRef);
        invalidParams.put("vnp_Amount", "50000000"); // 500,000 * 100
        invalidParams.put("vnp_ResponseCode", "00");
        invalidParams.put("vnp_TransactionStatus", "00");
        invalidParams.put("vnp_SecureHash", "INVALID_SIGNATURE_HASH_VALUE_12345");

        VNPayIpnResponse invalidResponse = vnPayService.processIpn(invalidParams);
        assertEquals("97", invalidResponse.getRspCode(), "Sai chữ ký phải trả về mã 97 (Invalid Checksum)");
        PaymentTransaction txnAfterInvalid = paymentTransactionRepository.findById(txn.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, txnAfterInvalid.getStatus(), "Trạng thái giao dịch không được thay đổi khi chữ ký sai");
        assertTrue(TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "Tuyệt đối không phát event khi sai chữ ký");

        // 3. Kịch bản B: IPN Thành công với chữ ký hợp lệ
        Map<String, String> validParams = new HashMap<>();
        validParams.put("vnp_TxnRef", txnRef);
        validParams.put("vnp_Amount", "50000000");
        validParams.put("vnp_BankCode", "NCB");
        validParams.put("vnp_BankTranNo", "VNPAY_BANK_REF_888");
        validParams.put("vnp_CardType", "ATM");
        validParams.put("vnp_OrderInfo", "Thanh toan don hang 9999");
        validParams.put("vnp_PayDate", "20261027120000");
        validParams.put("vnp_ResponseCode", "00");
        validParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        validParams.put("vnp_TransactionNo", "14999888");
        validParams.put("vnp_TransactionStatus", "00");

        String signedUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, validParams, TEST_SECRET);
        String validHash = signedUrl.substring(signedUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        validParams.put("vnp_SecureHash", validHash);

        VNPayIpnResponse successResponse = vnPayService.processIpn(validParams);
        assertEquals("00", successResponse.getRspCode(), "IPN hợp lệ phải phản hồi mã 00 (Confirm Success)");
        assertEquals("Confirm Success", successResponse.getMessage());

        PaymentTransaction txnAfterSuccess = paymentTransactionRepository.findById(txn.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, txnAfterSuccess.getStatus(), "Database phải được cập nhật sang SUCCESS");
        assertEquals("14999888", txnAfterSuccess.getVnpayTransactionNo());
        assertEquals("NCB", txnAfterSuccess.getVnpayBankCode());

        // Kiểm tra RabbitMQ Event được phát
        assertFalse(TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "Phải phát thông điệp PaymentCompletedEvent qua RabbitMQ");
        boolean hasPaymentEvent = TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.stream()
                .anyMatch(msg -> msg instanceof PaymentCompletedEvent && ((PaymentCompletedEvent) msg).getOrderId().equals("9999"));
        assertTrue(hasPaymentEvent, "PaymentCompletedEvent phải mang orderId 9999");

        // 4. Kịch bản C: IPN Gửi Lặp (Duplicate Delivery / Idempotency)
        TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.clear();
        VNPayIpnResponse duplicateResponse = vnPayService.processIpn(validParams);
        assertEquals("02", duplicateResponse.getRspCode(), "IPN lặp phải trả về mã 02 (Order already confirmed)");
        assertTrue(TestPaymentIntegrationConfig.PUBLISHED_MESSAGES.isEmpty(), "IPN lặp không được phát thêm event mới");
    }

    // =========================================================================
    // LUỒNG 7: ESCROW VÀ DOUBLE-ENTRY LEDGER (Tạm giữ, Giải phóng, Đối soát)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 7 - E2E] Escrow & Ledger: Tạm giữ, Giải phóng (3% hoa hồng), Bút toán kép cân đối tổng Nợ == tổng Có")
    void testEscrowAndLedger_Holding_Release_And_Reconcile() {
        Long subOrderId = 5551L;
        Long masterOrderId = 1111L;
        BigDecimal orderAmount = new BigDecimal("1000000"); // 1,000,000 VND

        // 1. Tạm giữ tiền Escrow cho SubOrder đã thanh toán
        HoldEscrowRequest holdRequest = HoldEscrowRequest.builder()
                .subOrderId(subOrderId)
                .masterOrderId(masterOrderId)
                .shopId(TEST_SHOP_ID)
                .amount(orderAmount)
                .build();

        EscrowResponse holdResponse = escrowService.holdEscrow(holdRequest);
        assertNotNull(holdResponse);
        assertEquals(EscrowStatus.HOLDING, holdResponse.getStatus());
        assertEquals(orderAmount, holdResponse.getAmount());

        // Kiểm tra Database: Tài khoản Escrow được lưu ở trạng thái HOLDING
        EscrowAccount escrow = escrowAccountRepository.findBySubOrderId(subOrderId).orElseThrow();
        assertEquals(EscrowStatus.HOLDING, escrow.getStatus());
        assertEquals(TEST_SHOP_ID, escrow.getShopId());

        // 2. Buyer xác nhận nhận hàng -> Giải phóng Escrow
        ReleaseEscrowRequest releaseRequest = ReleaseEscrowRequest.builder()
                .subOrderId(subOrderId)
                .trigger("BUYER_CONFIRMED")
                .build();

        EscrowResponse releaseResponse = escrowService.releaseEscrow(releaseRequest);
        assertEquals(EscrowStatus.RELEASED, releaseResponse.getStatus());
        assertNotNull(releaseResponse.getReleasedAt());

        // Hoa hồng 3% = 30,000 VND. Seller nhận 97% = 970,000 VND
        BigDecimal expectedCommission = new BigDecimal("30000");
        BigDecimal expectedSellerPayout = new BigDecimal("970000");
        assertEquals(expectedCommission, releaseResponse.getCommissionFee());
        assertEquals(expectedSellerPayout, releaseResponse.getNetSellerAmount());

        // Kiểm tra ví Seller nhận đúng 970,000 VND
        Wallet wallet = walletRepository.findByShopId(TEST_SHOP_ID).orElseThrow();
        assertEquals(expectedSellerPayout, wallet.getBalance(), "Ví người bán phải được cộng đúng 970,000 VND");

        // 3. Kiểm tra Sổ cái kép (Double-Entry Ledger)
        List<LedgerEntry> entries = ledgerEntryRepository.findAll();
        assertFalse(entries.isEmpty(), "Sổ cái phải ghi nhận các bút toán tài chính");

        // Tính tổng Nợ và tổng Có qua service đối soát
        ReconciliationReport report = doubleEntryLedgerService.reconcileAll();
        assertNotNull(report);
        assertTrue(report.isDoubleEntryBalanced(), "Tổng Nợ (Debit) và Tổng Có (Credit) trong sổ cái phải tuyệt đối cân đối");
        assertEquals("BALANCED", report.getStatus(), "Trạng thái đối soát phải là BALANCED");
    }

    // =========================================================================
    // LUỒNG 8: SELLER WALLET VÀ WITHDRAWAL (Rút tiền, Đóng băng, Phê duyệt, Từ chối)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 8 - E2E] Seller Wallet: Rút tiền đóng băng số dư, Chống rút vượt số dư, Phê duyệt / Từ chối ghi Ledger")
    void testSellerWallet_Withdrawal_BalanceFreeze_Approve_And_Reject() {
        // 1. Chuẩn bị ví Seller có sẵn 1,000,000 VND khả dụng
        Wallet wallet = walletRepository.save(Wallet.builder()
                .shopId(TEST_SHOP_ID)
                .balance(new BigDecimal("1000000"))
                .frozenBalance(BigDecimal.ZERO)
                .build());

        // 2. Kịch bản A: Rút vượt số dư khả dụng -> Phải bị từ chối
        WithdrawalRequestDto overdrawRequest = WithdrawalRequestDto.builder()
                .shopId(TEST_SHOP_ID)
                .amount(new BigDecimal("1500000")) // Vượt quá 1,000,000
                .bankCode("VCB")
                .bankAccountNumber("001100223344")
                .bankAccountName("CONG TY TNHH TEST")
                .build();

        assertThrows(com.quickshop.payment.exception.InsufficientBalanceException.class, () ->
                walletService.requestWithdrawal(overdrawRequest, TEST_USER_ID, "ROLE_SELLER")
        );

        // 3. Kịch bản B: Rút hợp lệ 400,000 VND -> Đóng băng 400,000 VND
        WithdrawalRequestDto validRequest = WithdrawalRequestDto.builder()
                .shopId(TEST_SHOP_ID)
                .amount(new BigDecimal("400000"))
                .bankCode("VCB")
                .bankAccountNumber("001100223344")
                .bankAccountName("CONG TY TNHH TEST")
                .build();

        Withdrawal withdrawal = walletService.requestWithdrawal(validRequest, TEST_USER_ID, "ROLE_SELLER");
        assertNotNull(withdrawal);
        assertEquals(WithdrawalStatus.PENDING, withdrawal.getStatus());

        // Kiểm tra số dư ví: Khả dụng còn 600,000; Đóng băng 400,000
        Wallet frozenWallet = walletRepository.findByShopId(TEST_SHOP_ID).orElseThrow();
        assertEquals(new BigDecimal("600000"), frozenWallet.getBalance());
        assertEquals(new BigDecimal("400000"), frozenWallet.getFrozenBalance());

        // 4. Kịch bản C: Admin từ chối lệnh rút tiền -> Hoàn trả lại số dư khả dụng
        Withdrawal rejected = walletService.rejectWithdrawal(withdrawal.getId(), 1L, "Sai thông tin tài khoản ngân hàng", "ROLE_ADMIN");
        assertEquals(WithdrawalStatus.REJECTED, rejected.getStatus());

        Wallet unfreezedWallet = walletRepository.findByShopId(TEST_SHOP_ID).orElseThrow();
        assertEquals(new BigDecimal("1000000"), unfreezedWallet.getBalance(), "Từ chối rút tiền phải hoàn lại số dư khả dụng");
        assertEquals(BigDecimal.ZERO, unfreezedWallet.getFrozenBalance(), "Số dư đóng băng phải trở về 0");

        // 5. Kịch bản D: Rút lại 500,000 VND và Admin phê duyệt -> COMPLETED & Ghi Ledger
        Withdrawal withdrawal2 = walletService.requestWithdrawal(
                WithdrawalRequestDto.builder()
                        .shopId(TEST_SHOP_ID)
                        .amount(new BigDecimal("500000"))
                        .bankCode("MB")
                        .bankAccountNumber("999988887777")
                        .bankAccountName("NGUYEN VAN A")
                        .build(),
                TEST_USER_ID,
                "ROLE_SELLER"
        );

        Withdrawal approved = walletService.approveWithdrawal(withdrawal2.getId(), 1L, "ROLE_ADMIN");
        assertEquals(WithdrawalStatus.COMPLETED, approved.getStatus());

        Wallet completedWallet = walletRepository.findByShopId(TEST_SHOP_ID).orElseThrow();
        assertEquals(new BigDecimal("500000"), completedWallet.getBalance());
        assertEquals(BigDecimal.ZERO, completedWallet.getFrozenBalance());

        // Kiểm tra bút toán rút tiền trong Sổ cái
        List<LedgerEntry> withdrawalEntries = ledgerEntryRepository.findByTransactionId("WITHDRAWAL_SETTLE_" + withdrawal2.getId());
        assertFalse(withdrawalEntries.isEmpty(), "Phải ghi nhận bút toán WITHDRAWAL trong sổ cái");
    }

    // =========================================================================
    // LUỒNG 10: REFUND (Hoàn tiền Escrow & Ghi Ledger REFUND)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 10 - E2E] Refund: Hoàn tiền Escrow, ghi bút toán REFUND và ngăn giải phóng tiền cho Seller")
    void testEscrowRefund_UpdatesStatus_RecordsLedger_And_PreventsRelease() {
        Long subOrderId = 7771L;
        BigDecimal amount = new BigDecimal("350000");

        // 1. Tạm giữ tiền Escrow
        escrowService.holdEscrow(HoldEscrowRequest.builder()
                .subOrderId(subOrderId)
                .masterOrderId(2222L)
                .shopId(TEST_SHOP_ID)
                .amount(amount)
                .build());

        // 2. Thực hiện hoàn tiền cho Buyer do hủy đơn
        RefundEscrowRequest refundRequest = RefundEscrowRequest.builder()
                .subOrderId(subOrderId)
                .refundAmount(amount)
                .reason("Đơn hàng bị người mua hủy trước khi giao hàng")
                .build();

        EscrowResponse refundResponse = escrowService.refundEscrow(refundRequest);
        assertEquals(EscrowStatus.REFUNDED, refundResponse.getStatus());
        assertNotNull(refundResponse.getRefundedAt());

        // Kiểm tra Database
        EscrowAccount refundedEscrow = escrowAccountRepository.findBySubOrderId(subOrderId).orElseThrow();
        assertEquals(EscrowStatus.REFUNDED, refundedEscrow.getStatus());

        // 3. Kiểm tra bút toán hoàn tiền trong Sổ cái
        List<LedgerEntry> refundEntries = ledgerEntryRepository.findByTransactionId("REFUND_SUB_" + subOrderId);
        assertFalse(refundEntries.isEmpty(), "Sổ cái phải có bút toán hoàn tiền REFUND");
        assertEquals(LedgerTransactionType.REFUND, refundEntries.get(0).getTransactionType());

        // 4. Đơn đã REFUND thì không thể giải phóng cho Seller
        assertThrows(IllegalStateException.class, () ->
                escrowService.releaseEscrow(ReleaseEscrowRequest.builder().subOrderId(subOrderId).trigger("BUYER_CONFIRMED").build())
        );
    }

    // =========================================================================
    // LUỒNG 12: SCHEDULED TASK GIẢI PHÓNG ESCROW (DELIVERED > 3 ngày & Bỏ qua tranh chấp)
    // =========================================================================
    @Test
    @Transactional
    @DisplayName("[Flow 12 - E2E] Scheduled Task Giải phóng Escrow: DELIVERED > 3 ngày tự động giải phóng, Đơn tranh chấp được bảo vệ")
    void testEscrowAutoReleaseScheduler_ReleasesExpired_SkipsDisputedOrders() {
        // 1. Đơn 1: DELIVERED 4 ngày trước, không tranh chấp -> Phải được tự động giải phóng
        EscrowAccount eligibleEscrow = escrowAccountRepository.save(EscrowAccount.builder()
                .subOrderId(8001L)
                .masterOrderId(3001L)
                .shopId(TEST_SHOP_ID)
                .amount(new BigDecimal("500000"))
                .commissionFee(new BigDecimal("15000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .deliveredAt(LocalDateTime.now().minusDays(4))
                .build());

        // 2. Đơn 2: DELIVERED 5 ngày trước NHƯNG có tranh chấp -> Tuyệt đối KHÔNG được giải phóng
        EscrowAccount disputedEscrow = escrowAccountRepository.save(EscrowAccount.builder()
                .subOrderId(8002L)
                .masterOrderId(3002L)
                .shopId(TEST_SHOP_ID)
                .amount(new BigDecimal("800000"))
                .commissionFee(new BigDecimal("24000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(true) // Đang tranh chấp
                .disputeReason("Hàng bể vỡ khi nhận")
                .deliveredAt(LocalDateTime.now().minusDays(5))
                .build());

        // 3. Đơn 3: Mới DELIVERED 1 ngày trước -> Chưa đủ 3 ngày, chưa giải phóng
        EscrowAccount recentEscrow = escrowAccountRepository.save(EscrowAccount.builder()
                .subOrderId(8003L)
                .masterOrderId(3003L)
                .shopId(TEST_SHOP_ID)
                .amount(new BigDecimal("300000"))
                .commissionFee(new BigDecimal("9000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .deliveredAt(LocalDateTime.now().minusDays(1))
                .build());

        entityManager.flush();
        entityManager.clear();

        // 4. Kích hoạt Scheduler quét giải phóng
        escrowAutoReleaseScheduler.scheduleAutoRelease();

        // 5. Kiểm tra kết quả trong Database
        EscrowAccount checkedEligible = escrowAccountRepository.findBySubOrderId(8001L).orElseThrow();
        assertEquals(EscrowStatus.RELEASED, checkedEligible.getStatus(), "Đơn DELIVERED quá 3 ngày không tranh chấp phải được RELEASED");

        EscrowAccount checkedDisputed = escrowAccountRepository.findBySubOrderId(8002L).orElseThrow();
        assertEquals(EscrowStatus.HOLDING, checkedDisputed.getStatus(), "Đơn đang có tranh chấp (isDisputed = true) phải giữ nguyên HOLDING");

        EscrowAccount checkedRecent = escrowAccountRepository.findBySubOrderId(8003L).orElseThrow();
        assertEquals(EscrowStatus.HOLDING, checkedRecent.getStatus(), "Đơn mới giao 1 ngày (< 3 ngày) phải giữ nguyên HOLDING");

        // 6. Kiểm tra tính Idempotent: Chạy lại scheduler không giải phóng lần 2
        escrowAutoReleaseScheduler.scheduleAutoRelease();
        EscrowAccount checkedAgain = escrowAccountRepository.findBySubOrderId(8001L).orElseThrow();
        assertEquals(EscrowStatus.RELEASED, checkedAgain.getStatus());
    }
}
