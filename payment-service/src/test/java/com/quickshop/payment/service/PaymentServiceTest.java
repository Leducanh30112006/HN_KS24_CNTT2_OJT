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
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private VNPayConfig vnPayConfig;

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
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
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
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class)); // 1 for shop release, 1 for commission
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
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
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
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
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
        verify(ledgerEntryRepository, times(1)).save(any(LedgerEntry.class));
    }
}
