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
    private PayOsService payOsService;

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
    @DisplayName("PayOS: Tạo yêu cầu thanh toán chuyển khoản ủy quyền cho PayOsService")
    void testCreatePayOsPayment_DelegatesToPayOsService() {
        CreatePayOsPaymentRequest req = CreatePayOsPaymentRequest.builder()
                .orderId("QS-999999")
                .amount(BigDecimal.valueOf(250000))
                .userId(1001L)
                .build();

        CreatePayOsPaymentResponse expected = CreatePayOsPaymentResponse.builder()
                .orderId("QS-999999")
                .amount(BigDecimal.valueOf(250000))
                .accountNumber("0346746521")
                .accountName("NGUYEN VAN KIEN")
                .qrCode("https://img.vietqr.io/...")
                .build();

        when(payOsService.createPaymentLink(req)).thenReturn(expected);

        CreatePayOsPaymentResponse resp = paymentService.createPayOsPayment(req);
        assertNotNull(resp);
        assertEquals("QS-999999", resp.getOrderId());
        verify(payOsService, times(1)).createPaymentLink(req);
    }

    @Test
    @DisplayName("PayOS: Xử lý Webhook ủy quyền cho PayOsService")
    void testProcessPayOsWebhook_DelegatesToPayOsService() {
        java.util.Map<String, Object> webhookData = java.util.Map.of("code", "00");
        java.util.Map<String, Object> expected = java.util.Map.of("error", 0, "message", "OK");

        when(payOsService.processWebhook(webhookData)).thenReturn(expected);

        java.util.Map<String, Object> res = paymentService.processPayOsWebhook(webhookData);
        assertEquals(0, res.get("error"));
        verify(payOsService, times(1)).processWebhook(webhookData);
    }

    @Test
    @DisplayName("PayOS: Kiểm tra trạng thái checkPaymentStatus ủy quyền cho PayOsService")
    void testCheckPaymentStatus_DelegatesToPayOsService() {
        java.util.Map<String, Object> expected = java.util.Map.of("paid", true, "status", "SUCCESS");

        when(payOsService.checkPaymentStatus("QS-888888")).thenReturn(expected);

        java.util.Map<String, Object> status = paymentService.checkPaymentStatus("QS-888888");
        assertTrue((Boolean) status.get("paid"));
        verify(payOsService, times(1)).checkPaymentStatus("QS-888888");
    }
}
