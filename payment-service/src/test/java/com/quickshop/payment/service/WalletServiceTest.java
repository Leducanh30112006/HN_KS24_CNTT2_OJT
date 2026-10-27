package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.adapter.BankTransferAdapter;
import com.quickshop.payment.adapter.dto.BankTransferRequest;
import com.quickshop.payment.adapter.dto.BankTransferResult;
import com.quickshop.payment.constant.LedgerAccount;
import com.quickshop.payment.dto.WalletResponse;
import com.quickshop.payment.dto.WithdrawalRequestDto;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.entity.enums.LedgerTransactionType;
import com.quickshop.payment.entity.enums.WithdrawalStatus;
import com.quickshop.payment.exception.ForbiddenException;
import com.quickshop.payment.exception.InsufficientBalanceException;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private DoubleEntryLedgerService doubleEntryLedgerService;

    @Mock
    private BankTransferAdapter bankTransferAdapter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    @DisplayName("1. Lấy thông tin ví: Tự động khởi tạo nếu Shop chưa có ví")
    void testGetWalletByShopId_AutoCreate() {
        Long shopId = 10L;
        when(walletRepository.findByShopId(shopId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.getWalletByShopId(shopId, 1L, "ROLE_SELLER");

        assertNotNull(response);
        assertEquals(shopId, response.getShopId());
        assertEquals(BigDecimal.ZERO, response.getBalance());
        assertEquals(BigDecimal.ZERO, response.getFrozenBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("2. Tạo yêu cầu rút tiền thành công: Trừ balance, tăng frozenBalance và ghi sổ cái")
    void testRequestWithdrawal_Success() {
        Long shopId = 10L;
        BigDecimal withdrawAmount = new BigDecimal("200000"); // 200,000 VND

        Wallet wallet = Wallet.builder()
                .id(1L)
                .shopId(shopId)
                .balance(new BigDecimal("500000"))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByShopIdForUpdate(shopId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> {
            Withdrawal w = inv.getArgument(0);
            w.setId(501L);
            return w;
        });

        WithdrawalRequestDto request = WithdrawalRequestDto.builder()
                .shopId(shopId)
                .amount(withdrawAmount)
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("NGUYEN VAN KIEN")
                .idempotencyKey("IDEMP_KEY_001")
                .build();

        Withdrawal result = walletService.requestWithdrawal(request, 1L, "ROLE_SELLER");

        assertNotNull(result);
        assertEquals(501L, result.getId());
        assertEquals(WithdrawalStatus.PENDING, result.getStatus());

        // Kiểm tra biến động số dư ví: balance 500k -> 300k, frozen 0 -> 200k
        assertEquals(new BigDecimal("300000"), wallet.getBalance());
        assertEquals(new BigDecimal("200000"), wallet.getFrozenBalance());

        // Ghi bút toán kép: Nợ SELLER_WALLET | Có WITHDRAWAL_PENDING
        verify(doubleEntryLedgerService).recordEntry(
                eq("WITHDRAWAL_REQ_501"),
                eq(LedgerTransactionType.WITHDRAWAL),
                eq(LedgerAccount.sellerWallet(shopId)),
                eq(LedgerAccount.WITHDRAWAL_PENDING),
                eq(withdrawAmount),
                anyString()
        );
    }

    @Test
    @DisplayName("3. Chặn rút vượt số dư (Không cho số dư âm): Ném InsufficientBalanceException")
    void testRequestWithdrawal_InsufficientBalance_ThrowsException() {
        Long shopId = 10L;

        Wallet wallet = Wallet.builder()
                .shopId(shopId)
                .balance(new BigDecimal("100000")) // Chỉ có 100,000 VND
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByShopIdForUpdate(shopId)).thenReturn(Optional.of(wallet));

        WithdrawalRequestDto request = WithdrawalRequestDto.builder()
                .shopId(shopId)
                .amount(new BigDecimal("200000")) // Rút 200,000 VND
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("NGUYEN VAN KIEN")
                .build();

        assertThrows(InsufficientBalanceException.class,
                () -> walletService.requestWithdrawal(request, 1L, "ROLE_SELLER"));

        // Số dư không bị thay đổi
        assertEquals(new BigDecimal("100000"), wallet.getBalance());
        verify(withdrawalRepository, never()).save(any());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("4. Xử lý request rút tiền lặp (Idempotent): Trả về lệnh đã tạo nếu trùng idempotencyKey mà không trừ tiền lần 2")
    void testRequestWithdrawal_DuplicateRequest_Idempotent() {
        Long shopId = 10L;
        String idempKey = "IDEMP_KEY_DUPLICATE_001";

        Withdrawal existing = Withdrawal.builder()
                .id(501L)
                .shopId(shopId)
                .amount(new BigDecimal("200000"))
                .status(WithdrawalStatus.PENDING)
                .idempotencyKey(idempKey)
                .build();

        when(withdrawalRepository.findByIdempotencyKey(idempKey)).thenReturn(Optional.of(existing));

        WithdrawalRequestDto request = WithdrawalRequestDto.builder()
                .shopId(shopId)
                .amount(new BigDecimal("200000"))
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("NGUYEN VAN KIEN")
                .idempotencyKey(idempKey)
                .build();

        Withdrawal result = walletService.requestWithdrawal(request, 1L, "ROLE_SELLER");

        assertNotNull(result);
        assertEquals(501L, result.getId());
        verify(walletRepository, never()).findByShopIdForUpdate(any());
        verify(doubleEntryLedgerService, never()).recordEntry(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("5. Kiểm tra quyền sở hữu ví: BUYER cố tạo lệnh rút tiền ném ForbiddenException")
    void testRequestWithdrawal_BuyerForbidden() {
        WithdrawalRequestDto request = WithdrawalRequestDto.builder()
                .shopId(10L)
                .amount(new BigDecimal("100000"))
                .build();

        assertThrows(ForbiddenException.class,
                () -> walletService.requestWithdrawal(request, 99L, "ROLE_BUYER"));
    }

    @Test
    @DisplayName("6. Admin phê duyệt lệnh rút: Gọi Mock Bank Adapter, chuyển sang COMPLETED, trừ frozenBalance và ghi sổ cái")
    void testApproveWithdrawal_Success() {
        Long withdrawalId = 501L;
        Long shopId = 10L;
        BigDecimal amount = new BigDecimal("200000");

        Withdrawal withdrawal = Withdrawal.builder()
                .id(withdrawalId)
                .shopId(shopId)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .bankCode("VCB")
                .bankAccountNumber("0123456789")
                .bankAccountName("NGUYEN VAN KIEN")
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = Wallet.builder()
                .shopId(shopId)
                .balance(new BigDecimal("300000"))
                .frozenBalance(new BigDecimal("200000"))
                .build();

        when(walletRepository.findByShopIdForUpdate(shopId)).thenReturn(Optional.of(wallet));

        // Mock kết quả chuyển tiền ngân hàng
        BankTransferResult mockBankResult = BankTransferResult.builder()
                .success(true)
                .bankTransactionRef("MOCK_NAPAS247_99998888")
                .responseCode("00")
                .build();
        when(bankTransferAdapter.transfer(any(BankTransferRequest.class))).thenReturn(mockBankResult);

        Withdrawal result = walletService.approveWithdrawal(withdrawalId, 1L, "ROLE_ADMIN");

        assertEquals(WithdrawalStatus.COMPLETED, result.getStatus());
        assertEquals(1L, result.getAdminId());
        assertEquals("MOCK_NAPAS247_99998888", result.getBankTransactionRef());
        assertNotNull(result.getProcessedAt());

        // Số dư đóng băng bị trừ: 200k -> 0
        assertEquals(BigDecimal.ZERO, wallet.getFrozenBalance());
        assertEquals(new BigDecimal("300000"), wallet.getBalance());

        // Bút toán tất toán: Nợ WITHDRAWAL_PENDING | Có BANK_SETTLEMENT
        verify(doubleEntryLedgerService).recordEntry(
                eq("WITHDRAWAL_SETTLE_501"),
                eq(LedgerTransactionType.WITHDRAWAL),
                eq(LedgerAccount.WITHDRAWAL_PENDING),
                eq(LedgerAccount.BANK_SETTLEMENT),
                eq(amount),
                anyString()
        );
    }

    @Test
    @DisplayName("7. Kiểm tra quyền Admin khi duyệt: User không phải ADMIN ném ForbiddenException")
    void testApproveWithdrawal_NonAdmin_ThrowsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> walletService.approveWithdrawal(501L, 2L, "ROLE_SELLER"));
    }

    @Test
    @DisplayName("8. Admin từ chối lệnh rút: Trừ frozenBalance, hoàn tiền về balance, chuyển sang REJECTED và ghi bút toán đảo")
    void testRejectWithdrawal_Success() {
        Long withdrawalId = 501L;
        Long shopId = 10L;
        BigDecimal amount = new BigDecimal("200000");

        Withdrawal withdrawal = Withdrawal.builder()
                .id(withdrawalId)
                .shopId(shopId)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .build();

        when(withdrawalRepository.findById(withdrawalId)).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet wallet = Wallet.builder()
                .shopId(shopId)
                .balance(new BigDecimal("300000"))
                .frozenBalance(new BigDecimal("200000"))
                .build();

        when(walletRepository.findByShopIdForUpdate(shopId)).thenReturn(Optional.of(wallet));

        Withdrawal result = walletService.rejectWithdrawal(withdrawalId, 1L, "Tên tài khoản không khớp CCCD", "ROLE_ADMIN");

        assertEquals(WithdrawalStatus.REJECTED, result.getStatus());
        assertEquals("Tên tài khoản không khớp CCCD", result.getRejectionReason());

        // Hoàn lại tiền vào số dư khả dụng: 300k + 200k = 500k, frozen = 0
        assertEquals(new BigDecimal("500000"), wallet.getBalance());
        assertEquals(BigDecimal.ZERO, wallet.getFrozenBalance());

        // Bút toán đảo: Nợ WITHDRAWAL_PENDING | Có SELLER_WALLET
        verify(doubleEntryLedgerService).recordEntry(
                eq("WITHDRAWAL_REJECT_501"),
                eq(LedgerTransactionType.WITHDRAWAL),
                eq(LedgerAccount.WITHDRAWAL_PENDING),
                eq(LedgerAccount.sellerWallet(shopId)),
                eq(amount),
                anyString()
        );
    }

    @Test
    @DisplayName("9. Kiểm tra quyền Admin khi từ chối: User không phải ADMIN ném ForbiddenException")
    void testRejectWithdrawal_NonAdmin_ThrowsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> walletService.rejectWithdrawal(501L, 2L, "Lý do", "ROLE_SELLER"));
    }
}
