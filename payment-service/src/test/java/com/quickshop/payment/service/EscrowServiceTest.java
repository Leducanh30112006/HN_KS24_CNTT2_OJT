package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.constant.LedgerAccount;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.enums.EscrowStatus;
import com.quickshop.payment.entity.enums.LedgerTransactionType;
import com.quickshop.payment.exception.DisputedEscrowException;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.service.impl.EscrowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

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

    @InjectMocks
    private EscrowServiceImpl escrowService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(escrowService, "commissionRate", new BigDecimal("0.03")); // 3%
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    @DisplayName("1. Giữ tiền Escrow thành công: Trạng thái HOLDING và ghi bút toán ESCROW_HOLD")
    void testHoldEscrow_Success() {
        HoldEscrowRequest request = HoldEscrowRequest.builder()
                .subOrderId(101L)
                .masterOrderId(1001L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .build();

        when(escrowAccountRepository.findBySubOrderId(101L)).thenReturn(Optional.empty());
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        EscrowResponse response = escrowService.holdEscrow(request);

        assertNotNull(response);
        assertEquals(101L, response.getSubOrderId());
        assertEquals(1001L, response.getMasterOrderId());
        assertEquals(5L, response.getShopId());
        assertEquals(new BigDecimal("500000"), response.getAmount());
        assertEquals(new BigDecimal("15000"), response.getCommissionFee()); // 3% của 500k
        assertEquals(EscrowStatus.HOLDING, response.getStatus());
        assertFalse(response.isDisputed());

        // Kiểm tra ghi sổ cái kép
        verify(doubleEntryLedgerService).recordEntry(
                eq("ESCROW_HOLD_SUB_101"),
                eq(LedgerTransactionType.ESCROW_HOLD),
                eq(LedgerAccount.CUSTOMER_PAYMENT),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq(new BigDecimal("500000")),
                anyString()
        );
    }

    @Test
    @DisplayName("2. Thao tác giữ tiền Escrow lặp (Idempotent): Trả về Escrow hiện tại nếu đã HOLDING cùng số tiền")
    void testHoldEscrow_DuplicateHold_Idempotent() {
        HoldEscrowRequest request = HoldEscrowRequest.builder()
                .subOrderId(101L)
                .masterOrderId(1001L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .build();

        EscrowAccount existing = EscrowAccount.builder()
                .id(1L)
                .subOrderId(101L)
                .masterOrderId(1001L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .build();

        when(escrowAccountRepository.findBySubOrderId(101L)).thenReturn(Optional.of(existing));

        EscrowResponse response = escrowService.holdEscrow(request);

        assertNotNull(response);
        assertEquals(101L, response.getSubOrderId());
        assertEquals(EscrowStatus.HOLDING, response.getStatus());

        // Đảm bảo không lưu mới hay ghi lại sổ cái
        verify(escrowAccountRepository, never()).save(any());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("3. Giải phóng Escrow cho Seller: Khấu trừ 3% hoa hồng, cộng tiền vào ví và ghi 2 bút toán Sổ cái")
    void testReleaseEscrow_Success() {
        Long subOrderId = 101L;
        Long shopId = 5L;
        BigDecimal amount = new BigDecimal("1000000"); // 1,000,000 VND

        EscrowAccount escrow = EscrowAccount.builder()
                .id(1L)
                .subOrderId(subOrderId)
                .masterOrderId(1001L)
                .shopId(shopId)
                .amount(amount)
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = Wallet.builder()
                .shopId(shopId)
                .balance(new BigDecimal("200000"))
                .frozenBalance(BigDecimal.ZERO)
                .build();
        when(walletRepository.findByShopIdForUpdate(shopId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        ReleaseEscrowRequest request = ReleaseEscrowRequest.builder()
                .subOrderId(subOrderId)
                .trigger("BUYER_CONFIRMED")
                .build();

        EscrowResponse response = escrowService.releaseEscrow(request);

        assertNotNull(response);
        assertEquals(EscrowStatus.RELEASED, response.getStatus());
        assertEquals(new BigDecimal("30000"), response.getCommissionFee()); // 3% = 30,000 VND
        assertEquals(new BigDecimal("970000"), response.getNetSellerAmount()); // Thực nhận 970,000 VND

        // Ví shop được cộng tiền: 200,000 + 970,000 = 1,170,000 VND
        assertEquals(new BigDecimal("1170000"), wallet.getBalance());
        verify(walletRepository).save(wallet);

        // Ghi 2 bút toán: Tiền về ví seller và hoa hồng sàn
        verify(doubleEntryLedgerService).recordEntry(
                eq("ESCROW_RELEASE_SUB_101"),
                eq(LedgerTransactionType.ESCROW_RELEASE),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq(LedgerAccount.sellerWallet(shopId)),
                eq(new BigDecimal("970000")),
                anyString()
        );

        verify(doubleEntryLedgerService).recordEntry(
                eq("COMMISSION_FEE_SUB_101"),
                eq(LedgerTransactionType.COMMISSION_FEE),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq(LedgerAccount.PLATFORM_REVENUE),
                eq(new BigDecimal("30000")),
                anyString()
        );
    }

    @Test
    @DisplayName("4. Đóng băng giải phóng Escrow khi có tranh chấp (Disputed): Ném DisputedEscrowException")
    void testReleaseEscrow_Disputed_ThrowsDisputedEscrowException() {
        Long subOrderId = 101L;

        EscrowAccount disputedEscrow = EscrowAccount.builder()
                .subOrderId(subOrderId)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(true) // Đang có tranh chấp!
                .disputeReason("Hàng vỡ hỏng trong quá trình vận chuyển")
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(disputedEscrow));

        ReleaseEscrowRequest request = ReleaseEscrowRequest.builder()
                .subOrderId(subOrderId)
                .trigger("BUYER_CONFIRMED")
                .build();

        assertThrows(DisputedEscrowException.class, () -> escrowService.releaseEscrow(request));

        // Không cộng tiền vào ví hay ghi sổ cái
        verify(walletRepository, never()).save(any());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("5. Thao tác giải phóng Escrow lặp (Idempotent): Trả về kết quả mà không cộng ví hay ghi sổ cái lần 2")
    void testReleaseEscrow_DuplicateRelease_Idempotent() {
        Long subOrderId = 101L;

        EscrowAccount alreadyReleased = EscrowAccount.builder()
                .subOrderId(subOrderId)
                .amount(new BigDecimal("500000"))
                .commissionFee(new BigDecimal("15000"))
                .status(EscrowStatus.RELEASED)
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(alreadyReleased));

        ReleaseEscrowRequest request = ReleaseEscrowRequest.builder()
                .subOrderId(subOrderId)
                .trigger("BUYER_CONFIRMED")
                .build();

        EscrowResponse response = escrowService.releaseEscrow(request);

        assertNotNull(response);
        assertEquals(EscrowStatus.RELEASED, response.getStatus());

        verify(walletRepository, never()).save(any());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("6. Hoàn tiền Escrow lại cho Buyer: Trạng thái REFUNDED và ghi bút toán REFUND")
    void testRefundEscrow_Success() {
        Long subOrderId = 101L;
        BigDecimal amount = new BigDecimal("500000");

        EscrowAccount escrow = EscrowAccount.builder()
                .subOrderId(subOrderId)
                .amount(amount)
                .status(EscrowStatus.HOLDING)
                .isDisputed(true)
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundEscrowRequest request = RefundEscrowRequest.builder()
                .subOrderId(subOrderId)
                .refundAmount(amount)
                .reason("Chấp thuận hoàn tiền do hàng lỗi kỹ thuật")
                .build();

        EscrowResponse response = escrowService.refundEscrow(request);

        assertNotNull(response);
        assertEquals(EscrowStatus.REFUNDED, response.getStatus());
        assertFalse(response.isDisputed()); // Tranh chấp đã được xử lý xong
        assertNotNull(response.getRefundedAt());

        verify(doubleEntryLedgerService).recordEntry(
                eq("REFUND_SUB_101"),
                eq(LedgerTransactionType.REFUND),
                eq(LedgerAccount.SYSTEM_ESCROW),
                eq(LedgerAccount.CUSTOMER_REFUND),
                eq(amount),
                anyString()
        );
    }

    @Test
    @DisplayName("7. Hoàn tiền thất bại nếu số tiền hoàn lớn hơn số tiền đang tạm giữ")
    void testRefundEscrow_ExceedsHoldingAmount_ThrowsException() {
        Long subOrderId = 101L;

        EscrowAccount escrow = EscrowAccount.builder()
                .subOrderId(subOrderId)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(escrow));

        RefundEscrowRequest request = RefundEscrowRequest.builder()
                .subOrderId(subOrderId)
                .refundAmount(new BigDecimal("600000")) // Vượt quá 500k
                .reason("Hoàn tiền")
                .build();

        assertThrows(BadRequestException.class, () -> escrowService.refundEscrow(request));
    }

    @Test
    @DisplayName("8. Đóng băng và mở đóng băng Escrow khi có khiếu nại tranh chấp")
    void testFreezeAndUnfreezeEscrow() {
        Long subOrderId = 101L;

        EscrowAccount escrow = EscrowAccount.builder()
                .subOrderId(subOrderId)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .build();

        when(escrowAccountRepository.findBySubOrderIdForUpdate(subOrderId)).thenReturn(Optional.of(escrow));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Đóng băng
        EscrowResponse frozenResponse = escrowService.freezeEscrowForDispute(subOrderId, "Khiếu nại chưa nhận được hàng");
        assertTrue(frozenResponse.isDisputed());
        assertEquals("Khiếu nại chưa nhận được hàng", frozenResponse.getDisputeReason());

        // 2. Mở đóng băng (khi Admin phân xử bác bỏ khiếu nại)
        EscrowResponse unfrozenResponse = escrowService.unfreezeEscrow(subOrderId);
        assertFalse(unfrozenResponse.isDisputed());
        assertNull(unfrozenResponse.getDisputeReason());
    }

    @Test
    @DisplayName("9. Quét tự động các Escrow giao thành công (DELIVERED) quá 3 ngày không khiếu nại")
    void testAutoReleaseEligibleEscrows() {
        EscrowAccount eligible1 = EscrowAccount.builder()
                .subOrderId(101L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .isDisputed(false)
                .deliveredAt(LocalDateTime.now().minusDays(4))
                .build();

        when(escrowAccountRepository.findEligibleForAutoRelease(any(LocalDateTime.class)))
                .thenReturn(List.of(eligible1));

        when(escrowAccountRepository.findBySubOrderIdForUpdate(101L)).thenReturn(Optional.of(eligible1));
        when(escrowAccountRepository.save(any(EscrowAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = Wallet.builder().shopId(5L).balance(BigDecimal.ZERO).build();
        when(walletRepository.findByShopIdForUpdate(5L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        int releasedCount = escrowService.autoReleaseEligibleEscrows();

        assertEquals(1, releasedCount);
        assertEquals(EscrowStatus.RELEASED, eligible1.getStatus());
        verify(doubleEntryLedgerService, times(2)).recordEntry(any(), any(), any(), any(), any(), any());
    }
}
