package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private vn.payos.PayOS payOS;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Wallet sampleWallet;
    private EscrowAccount sampleEscrow;

    @BeforeEach
    void setUp() {
        sampleWallet = Wallet.builder()
                .id(1L)
                .shopId(501L)
                .balance(BigDecimal.valueOf(1000000))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        sampleEscrow = EscrowAccount.builder()
                .id(1L)
                .subOrderId(2001L)
                .amount(BigDecimal.valueOf(500000))
                .commissionFee(BigDecimal.valueOf(25000))
                .status("HOLDING")
                .build();
    }

    @Test
    @DisplayName("Ký quỹ escrow thành công và đóng băng tiền trong ví shop, ghi sổ cái")
    void testHoldEscrow_Success() {
        HoldEscrowRequest req = HoldEscrowRequest.builder()
                .subOrderId(2001L)
                .shopId(501L)
                .amount(BigDecimal.valueOf(500000))
                .commissionFee(BigDecimal.valueOf(25000))
                .build();

        when(escrowAccountRepository.findBySubOrderId(2001L)).thenReturn(Optional.empty());
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));

        EscrowAccount result = paymentService.holdEscrow(req);

        assertNotNull(result);
        assertEquals("HOLDING", result.getStatus());
        assertEquals(BigDecimal.valueOf(500000), result.getAmount());
        assertEquals(BigDecimal.valueOf(475000), sampleWallet.getFrozenBalance());
        verify(ledgerService, times(1)).recordDoubleEntry(
                eq("ESCROW-HOLD-2001"), eq("ESCROW_HOLD"), eq("CUSTOMER_PAYMENT"), eq("ESCROW_HOLDING"), eq(BigDecimal.valueOf(500000)));
    }

    @Test
    @DisplayName("Giải phóng ký quỹ escrow chuyển vào số dư ví shop và ghi hoa hồng sàn")
    void testReleaseEscrow_Success() {
        sampleWallet.setFrozenBalance(BigDecimal.valueOf(475000));
        when(escrowAccountRepository.findBySubOrderId(2001L)).thenReturn(Optional.of(sampleEscrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));

        ReleaseEscrowRequest req = ReleaseEscrowRequest.builder()
                .subOrderId(2001L)
                .shopId(501L)
                .build();

        EscrowAccount released = paymentService.releaseEscrow(req);

        assertEquals("RELEASED", released.getStatus());
        assertEquals(BigDecimal.ZERO, sampleWallet.getFrozenBalance());
        assertEquals(BigDecimal.valueOf(1475000), sampleWallet.getBalance()); // 1,000,000 + 475,000
        verify(ledgerService, times(1)).recordSplitCreditEntry(
                eq("ESCROW-REL-2001"), eq("ESCROW_RELEASE"), eq("ESCROW_HOLDING"), anyMap());
    }

    @Test
    @DisplayName("Hoàn tiền ký quỹ escrow cho người mua khi đơn bị hủy")
    void testRefundEscrow_Success() {
        sampleWallet.setFrozenBalance(BigDecimal.valueOf(475000));
        when(escrowAccountRepository.findBySubOrderId(2001L)).thenReturn(Optional.of(sampleEscrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));

        RefundEscrowRequest req = RefundEscrowRequest.builder()
                .subOrderId(2001L)
                .shopId(501L)
                .reason("Khách hủy đơn")
                .build();

        EscrowAccount refunded = paymentService.refundEscrow(req);

        assertEquals("REFUNDED", refunded.getStatus());
        assertEquals(BigDecimal.ZERO, sampleWallet.getFrozenBalance());
        verify(ledgerService, times(1)).recordDoubleEntry(
                eq("ESCROW-REFUND-2001"), eq("ESCROW_REFUND"), eq("ESCROW_HOLDING"), eq("CUSTOMER_REFUND"), eq(BigDecimal.valueOf(500000)));
    }

    @Test
    @DisplayName("Tạo yêu cầu rút tiền thành công khi số dư ví khả dụng")
    void testRequestWithdrawal_Success() {
        WithdrawalRequestDto req = WithdrawalRequestDto.builder()
                .shopId(501L)
                .amount(BigDecimal.valueOf(300000))
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("SHOP HOA HONG")
                .build();

        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> {
            Withdrawal w = invocation.getArgument(0);
            w.setId(99L);
            return w;
        });

        Withdrawal withdrawal = paymentService.requestWithdrawal(req);

        assertNotNull(withdrawal);
        assertEquals(BigDecimal.valueOf(700000), sampleWallet.getBalance());
        assertEquals(BigDecimal.valueOf(300000), sampleWallet.getFrozenBalance());
        assertEquals("PENDING", withdrawal.getStatus());
        verify(ledgerService, times(1)).recordDoubleEntry(
                eq("WITHDRAWAL-REQ-99"), eq("WITHDRAWAL_REQUEST"), eq("SHOP_WALLET_501"), eq("WITHDRAWAL_PENDING"), eq(BigDecimal.valueOf(300000)));
    }

    @Test
    @DisplayName("Tạo yêu cầu rút tiền thất bại khi số dư không đủ")
    void testRequestWithdrawal_InsufficientBalance() {
        WithdrawalRequestDto req = WithdrawalRequestDto.builder()
                .shopId(501L)
                .amount(BigDecimal.valueOf(2000000)) // Greater than 1,000,000
                .build();

        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));

        assertThrows(BadRequestException.class, () -> paymentService.requestWithdrawal(req));
    }

    @Test
    @DisplayName("Admin duyệt hoàn tất yêu cầu rút tiền")
    void testProcessWithdrawal_Approve() {
        Withdrawal pending = Withdrawal.builder()
                .id(99L)
                .shopId(501L)
                .amount(BigDecimal.valueOf(300000))
                .status("PENDING")
                .build();

        sampleWallet.setBalance(BigDecimal.valueOf(700000));
        sampleWallet.setFrozenBalance(BigDecimal.valueOf(300000));

        when(withdrawalRepository.findById(99L)).thenReturn(Optional.of(pending));
        when(walletRepository.findByShopId(501L)).thenReturn(Optional.of(sampleWallet));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WithdrawalActionRequest actionReq = WithdrawalActionRequest.builder()
                .action("APPROVE")
                .adminId(1L)
                .build();

        Withdrawal processed = paymentService.processWithdrawal(99L, actionReq);

        assertEquals("COMPLETED", processed.getStatus());
        assertEquals(BigDecimal.ZERO, sampleWallet.getFrozenBalance());
        assertEquals(BigDecimal.valueOf(700000), sampleWallet.getBalance());
        verify(ledgerService, times(1)).recordDoubleEntry(
                eq("WITHDRAWAL-CMP-99"), eq("WITHDRAWAL_COMPLETED"), eq("WITHDRAWAL_PENDING"), eq("BANK_SETTLEMENT"), eq(BigDecimal.valueOf(300000)));
    }

    @Test
    @DisplayName("PayOS: Tạo yêu cầu thanh toán thành công và lưu PaymentTransaction")
    void testCreatePayOsPayment_Success() {
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-999999"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePayOsPaymentRequest req = CreatePayOsPaymentRequest.builder()
                .orderId("QS-999999")
                .amount(BigDecimal.valueOf(250000))
                .userId(1001L)
                .build();

        CreatePayOsPaymentResponse resp = paymentService.createPayOsPayment(req);

        assertNotNull(resp);
        assertEquals("QS-999999", resp.getOrderId());
        assertEquals(BigDecimal.valueOf(250000), resp.getAmount());
        assertEquals("0346746521", resp.getAccountNumber());
        assertEquals("NGUYEN VAN KIEN", resp.getAccountName());
        assertNotNull(resp.getQrCode());
        verify(paymentTransactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("PayOS: Xử lý Webhook thanh toán MB Bank thành công và Idempotent")
    void testProcessPayOsWebhook_SuccessAndIdempotent() {
        when(paymentTransactionRepository.findByTxnRef("123456")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("123456")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        java.util.Map<String, Object> webhookData = new java.util.HashMap<>();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("orderCode", 123456L);
        data.put("amount", 250000L);
        data.put("description", "QS DH123456");
        data.put("reference", "MB_REF_001");
        data.put("code", "00");
        webhookData.put("data", data);

        // Lần 1: Xử lý thành công
        java.util.Map<String, Object> res1 = paymentService.processPayOsWebhook(webhookData);
        assertEquals(0, res1.get("error"));
        verify(ledgerService, times(1)).recordDoubleEntry(
                anyString(), eq("PAYMENT_PAYOS_MBBANK_SUCCESS"), eq("MBBANK_PAYOS_GATEWAY"), eq("ESCROW_HOLDING"), any());

        // Lần 2: Giả lập gọi lại cùng dữ liệu (Idempotent test)
        com.quickshop.payment.entity.PaymentTransaction existingSuccess = com.quickshop.payment.entity.PaymentTransaction.builder()
                .orderId("123456")
                .txnRef("123456")
                .amount(BigDecimal.valueOf(250000))
                .status(com.quickshop.payment.entity.PaymentStatus.SUCCESS)
                .build();
        when(paymentTransactionRepository.findByTxnRef("123456")).thenReturn(Optional.of(existingSuccess));

        java.util.Map<String, Object> res2 = paymentService.processPayOsWebhook(webhookData);
        assertEquals(0, res2.get("error"));
        assertTrue(String.valueOf(res2.get("message")).contains("Idempotent"));
        // Đảm bảo không ghi sổ kép
        verify(ledgerService, times(1)).recordDoubleEntry(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PayOS: Kiểm tra trạng thái thanh toán checkPaymentStatus")
    void testCheckPaymentStatus_Success() {
        com.quickshop.payment.entity.PaymentTransaction existingSuccess = com.quickshop.payment.entity.PaymentTransaction.builder()
                .orderId("QS-888888")
                .txnRef("888888")
                .amount(BigDecimal.valueOf(150000))
                .status(com.quickshop.payment.entity.PaymentStatus.SUCCESS)
                .vnpTransactionNo("MB_FAST_888888")
                .bankCode("MB")
                .build();
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-888888"))
                .thenReturn(Optional.of(existingSuccess));

        java.util.Map<String, Object> status = paymentService.checkPaymentStatus("QS-888888");
        assertTrue((Boolean) status.get("paid"));
        assertEquals("SUCCESS", status.get("status"));
        assertEquals("MB", status.get("bankCode"));
        assertEquals("MB_FAST_888888", status.get("transactionNo"));
    }
}
