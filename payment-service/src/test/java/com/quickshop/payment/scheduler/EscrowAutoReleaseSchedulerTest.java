package com.quickshop.payment.scheduler;

import com.quickshop.payment.constant.LedgerAccount;
import com.quickshop.payment.dto.EscrowResponse;
import com.quickshop.payment.dto.ReleaseEscrowRequest;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.enums.EscrowStatus;
import com.quickshop.payment.entity.enums.LedgerTransactionType;
import com.quickshop.payment.exception.DisputedEscrowException;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.service.DoubleEntryLedgerService;
import com.quickshop.payment.service.impl.EscrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EscrowAutoReleaseScheduler Tests - Tự động giải phóng Escrow lúc 00:00 & Bỏ qua đơn tranh chấp")
class EscrowAutoReleaseSchedulerTest {

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private DoubleEntryLedgerService doubleEntryLedgerService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private EscrowServiceImpl escrowService;
    private EscrowAutoReleaseScheduler scheduler;

    @BeforeEach
    void setUp() {
        escrowService = new EscrowServiceImpl(
                escrowAccountRepository,
                walletRepository,
                doubleEntryLedgerService,
                stringRedisTemplate
        );
        ReflectionTestUtils.setField(escrowService, "commissionRate", new BigDecimal("0.03")); // 3%
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        scheduler = new EscrowAutoReleaseScheduler(escrowService);
    }

    @Test
    @DisplayName("Tác vụ 2: Đơn DELIVERED quá 3 ngày không khiếu nại => Tự động giải phóng Escrow, khấu trừ 3% hoa hồng và cộng ví Seller")
    void testScheduleAutoRelease_DeliveredOver3Days_Success() {
        LocalDateTime fourDaysAgo = LocalDateTime.now().minusDays(4);

        EscrowAccount eligibleEscrow = EscrowAccount.builder()
                .id(1L)
                .subOrderId(501L)
                .masterOrderId(101L)
                .shopId(10L)
                .amount(new BigDecimal("1000000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .deliveredAt(fourDaysAgo)
                .build();

        Wallet shopWallet = Wallet.builder()
                .id(10L)
                .shopId(10L)
                .balance(new BigDecimal("500000"))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(escrowAccountRepository.findEligibleForAutoRelease(any(LocalDateTime.class)))
                .thenReturn(List.of(eligibleEscrow));
        when(escrowAccountRepository.findBySubOrderIdForUpdate(501L))
                .thenReturn(Optional.of(eligibleEscrow));
        when(walletRepository.findByShopIdForUpdate(10L))
                .thenReturn(Optional.of(shopWallet));

        scheduler.scheduleAutoRelease();

        // 1. Kiểm tra trạng thái chuyển sang RELEASED
        assertEquals(EscrowStatus.RELEASED, eligibleEscrow.getStatus());
        assertEquals(new BigDecimal("30000"), eligibleEscrow.getCommissionFee()); // 3% của 1.000.000 = 30.000
        assertNotNull(eligibleEscrow.getReleasedAt());
        verify(escrowAccountRepository).save(eligibleEscrow);

        // 2. Kiểm tra ví Seller nhận số tiền thực nhận (1.000.000 - 30.000 = 970.000)
        // Số dư mới = 500.000 + 970.000 = 1.470.000
        assertEquals(new BigDecimal("1470000"), shopWallet.getBalance());
        verify(walletRepository).save(shopWallet);

        // 3. Kiểm tra ghi sổ cái kép Double-Entry chính xác 2 bút toán
        verify(doubleEntryLedgerService).recordEntry(
                anyString(),
                eq(LedgerTransactionType.ESCROW_RELEASE),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq("SELLER_WALLET_10"),
                eq(new BigDecimal("970000")),
                anyString()
        );
        verify(doubleEntryLedgerService).recordEntry(
                anyString(),
                eq(LedgerTransactionType.COMMISSION_FEE),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq(LedgerAccount.PLATFORM_REVENUE),
                eq(new BigDecimal("30000")),
                anyString()
        );
    }

    @Test
    @DisplayName("Tác vụ 2: Đơn đang có khiếu nại / tranh chấp (isDisputed = true) => BỊ ĐÓNG BĂNG, KHÔNG GIẢI PHÓNG")
    void testScheduleAutoRelease_DisputedEscrow_SkippedAndProtected() {
        LocalDateTime fourDaysAgo = LocalDateTime.now().minusDays(4);

        // findEligibleForAutoRelease trên database lọc is_disputed = false nên danh sách trả về rỗng
        when(escrowAccountRepository.findEligibleForAutoRelease(any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.scheduleAutoRelease();

        // Không có đơn nào được giải phóng
        verify(escrowAccountRepository, never()).findBySubOrderIdForUpdate(anyLong());
        verify(walletRepository, never()).findByShopIdForUpdate(anyLong());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tác vụ 2: Nếu có đơn tranh chấp bị gọi giải phóng => Ném DisputedEscrowException và không cộng tiền ví")
    void testReleaseEscrow_DisputedDirect_ThrowsDisputedEscrowException() {
        EscrowAccount disputedEscrow = EscrowAccount.builder()
                .id(2L)
                .subOrderId(502L)
                .shopId(10L)
                .amount(new BigDecimal("1000000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(true)
                .disputeReason("Hàng bị vỡ khi nhận")
                .deliveredAt(LocalDateTime.now().minusDays(4))
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(502L))
                .thenReturn(Optional.of(disputedEscrow));

        DisputedEscrowException ex = assertThrows(DisputedEscrowException.class, () ->
                escrowService.releaseEscrow(ReleaseEscrowRequest.builder()
                        .subOrderId(502L)
                        .trigger("MANUAL_TEST")
                        .build()));

        assertTrue(ex.getMessage().contains("đang có khiếu nại/tranh chấp"));
        assertEquals(EscrowStatus.HOLDING, disputedEscrow.getStatus());
        verify(walletRepository, never()).findByShopIdForUpdate(anyLong());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tác vụ 2: Đơn đã giải phóng trước đó (RELEASED) => Bỏ qua an toàn, không giải phóng hai lần")
    void testReleaseEscrow_AlreadyReleased_IdempotentSkip() {
        EscrowAccount alreadyReleased = EscrowAccount.builder()
                .id(3L)
                .subOrderId(503L)
                .shopId(10L)
                .amount(new BigDecimal("1000000"))
                .status(EscrowStatus.RELEASED)
                .commissionFee(new BigDecimal("30000"))
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(503L))
                .thenReturn(Optional.of(alreadyReleased));

        EscrowResponse response = escrowService.releaseEscrow(ReleaseEscrowRequest.builder()
                .subOrderId(503L)
                .trigger("MANUAL_TEST")
                .build());

        assertNotNull(response);
        assertEquals(EscrowStatus.RELEASED, response.getStatus());

        // Không ghi sổ cái kép lần 2, không cộng thêm tiền vào ví
        verify(walletRepository, never()).findByShopIdForUpdate(anyLong());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }
}
