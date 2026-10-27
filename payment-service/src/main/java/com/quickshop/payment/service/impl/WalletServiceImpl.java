package com.quickshop.payment.service.impl;

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
import com.quickshop.payment.service.DoubleEntryLedgerService;
import com.quickshop.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final DoubleEntryLedgerService doubleEntryLedgerService;
    private final BankTransferAdapter bankTransferAdapter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public WalletResponse getWalletByShopId(Long shopId, Long currentUserId, String currentUserRole) {
        log.info("[Wallet][ShopID: {}] Lấy thông tin số dư ví. Yêu cầu bởi User #{} ({})",
                shopId, currentUserId, currentUserRole);

        // Yêu cầu 10: Kiểm tra quyền truy cập ví
        if (currentUserRole != null && "ROLE_BUYER".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Người mua không có quyền xem thông tin số dư ví của gian hàng.");
        }

        Wallet wallet = walletRepository.findByShopId(shopId)
                .orElseGet(() -> {
                    log.info("[Wallet] Tạo ví mới mặc định cho Shop #{}", shopId);
                    Wallet newWallet = Wallet.builder()
                            .shopId(shopId)
                            .balance(BigDecimal.ZERO)
                            .frozenBalance(BigDecimal.ZERO)
                            .build();
                    return walletRepository.save(newWallet);
                });

        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public Withdrawal requestWithdrawal(WithdrawalRequestDto request, Long currentUserId, String currentUserRole) {
        Long shopId = request.getShopId();
        BigDecimal amount = request.getAmount();
        log.info("[Withdrawal][ShopID: {}] Tạo lệnh rút tiền: {} VND. Yêu cầu bởi User #{} ({})",
                shopId, amount, currentUserId, currentUserRole);

        // Yêu cầu 10: Kiểm tra quyền sở hữu ví (Chỉ SELLER hoặc ADMIN mới được tạo lệnh rút)
        if (currentUserRole != null && !"ROLE_SELLER".equalsIgnoreCase(currentUserRole) && !"ROLE_ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Chỉ người bán (Seller) của gian hàng mới có quyền tạo lệnh rút tiền.");
        }

        if (amount == null || amount.compareTo(new BigDecimal("50000")) < 0) {
            throw new BadRequestException("Số tiền rút tối thiểu là 50,000 VND");
        }

        // Yêu cầu 11: Xử lý request lặp qua idempotencyKey
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Withdrawal> existingOpt = withdrawalRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingOpt.isPresent()) {
                log.info("[Withdrawal][ShopID: {}] Phát hiện request lặp với Idempotency Key: {}. Trả về kết quả hiện tại.",
                        shopId, request.getIdempotencyKey());
                return existingOpt.get();
            }
        }

        // Khóa phân tán Redis chống gửi request rút tiền dồn dập đồng thời
        String lockKey = "lock:wallet:withdraw:" + shopId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("Yêu cầu rút tiền cho Shop #" + shopId + " đang được xử lý đồng thời.");
        }

        try {
            // Yêu cầu 7 & 11: Khóa bi quan CSDL (Pessimistic Lock) trên ví
            Wallet wallet = walletRepository.findByShopIdForUpdate(shopId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của Shop: " + shopId));

            // Yêu cầu 5 & 8: Kiểm tra số dư trước khi rút, không cho số dư âm
            if (wallet.getBalance().compareTo(amount) < 0) {
                log.warn("[Withdrawal][ShopID: {}] Số dư khả dụng không đủ: Có {} VND, yêu cầu rút {} VND",
                        shopId, wallet.getBalance(), amount);
                throw new InsufficientBalanceException("Số dư khả dụng không đủ để rút tiền: " + wallet.getBalance() + " VND");
            }

            // Yêu cầu 6: Đóng băng số tiền khi withdrawal được tạo (Giảm balance, tăng frozenBalance)
            wallet.setBalance(wallet.getBalance().subtract(amount));
            wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
            walletRepository.save(wallet);

            // Yêu cầu 7: Tạo lệnh rút tiền với trạng thái PENDING
            Withdrawal withdrawal = Withdrawal.builder()
                    .shopId(shopId)
                    .amount(amount)
                    .bankCode(request.getBankCode())
                    .bankAccountNumber(request.getBankAccountNumber())
                    .bankAccountName(request.getBankAccountName())
                    .idempotencyKey(request.getIdempotencyKey())
                    .status(WithdrawalStatus.PENDING)
                    .build();
            Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

            // Ghi bút toán kép vào Sổ cái: Nợ SELLER_WALLET_{shopId} | Có WITHDRAWAL_PENDING
            String txId = "WITHDRAWAL_REQ_" + savedWithdrawal.getId();
            doubleEntryLedgerService.recordEntry(
                    txId,
                    LedgerTransactionType.WITHDRAWAL,
                    LedgerAccount.sellerWallet(shopId),
                    LedgerAccount.WITHDRAWAL_PENDING,
                    amount,
                    "Tạo lệnh rút tiền #" + savedWithdrawal.getId() + " cho Shop #" + shopId
            );

            log.info("[Withdrawal][ShopID: {}] Tạo lệnh rút tiền #{} thành công. Số dư khả dụng còn lại: {} VND",
                    shopId, savedWithdrawal.getId(), wallet.getBalance());

            return savedWithdrawal;

        } finally {
            try {
                String current = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(current)) {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.error("[Withdrawal] Lỗi giải phóng lock Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public Withdrawal approveWithdrawal(Long withdrawalId, Long adminId, String currentUserRole) {
        log.info("[Withdrawal Approve][ID: {}] Admin #{} ({}) phê duyệt lệnh rút tiền...",
                withdrawalId, adminId, currentUserRole);

        // Yêu cầu 10: Kiểm tra quyền Admin
        if (currentUserRole != null && !"ROLE_ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Chỉ quản trị viên (Admin) mới có quyền phê duyệt lệnh rút tiền.");
        }

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lệnh rút tiền: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể phê duyệt lệnh rút ở trạng thái PENDING. Trạng thái hiện tại: " + withdrawal.getStatus());
        }

        Long shopId = withdrawal.getShopId();
        BigDecimal amount = withdrawal.getAmount();

        Wallet wallet = walletRepository.findByShopIdForUpdate(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của Shop: " + shopId));

        if (wallet.getFrozenBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư đóng băng của ví (" + wallet.getFrozenBalance() + ") nhỏ hơn số tiền rút (" + amount + ")");
        }

        // Yêu cầu 7: Chuyển trạng thái sang APPROVED
        withdrawal.setStatus(WithdrawalStatus.APPROVED);
        withdrawal.setAdminId(adminId);

        // Yêu cầu 12: Gọi BankTransferAdapter (Mô phỏng ngân hàng chuyển khoản)
        BankTransferRequest transferRequest = BankTransferRequest.builder()
                .withdrawalId(withdrawalId)
                .shopId(shopId)
                .amount(amount)
                .bankCode(withdrawal.getBankCode())
                .bankAccountNumber(withdrawal.getBankAccountNumber())
                .bankAccountName(withdrawal.getBankAccountName())
                .build();

        BankTransferResult transferResult = bankTransferAdapter.transfer(transferRequest);

        if (transferResult.isSuccess()) {
            // Yêu cầu 7: Cập nhật trạng thái sang COMPLETED khi ngân hàng tất toán thành công
            withdrawal.setStatus(WithdrawalStatus.COMPLETED);
            withdrawal.setBankTransactionRef(transferResult.getBankTransactionRef());
            withdrawal.setProcessedAt(LocalDateTime.now());
            Withdrawal saved = withdrawalRepository.save(withdrawal);

            // Trừ số dư đóng băng của ví
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
            walletRepository.save(wallet);

            // Yêu cầu 9: Ghi Sổ cái chính xác: Nợ WITHDRAWAL_PENDING | Có BANK_SETTLEMENT
            String txId = "WITHDRAWAL_SETTLE_" + withdrawalId;
            doubleEntryLedgerService.recordEntry(
                    txId,
                    LedgerTransactionType.WITHDRAWAL,
                    LedgerAccount.WITHDRAWAL_PENDING,
                    LedgerAccount.BANK_SETTLEMENT,
                    amount,
                    "Tất toán chuyển khoản lệnh rút tiền #" + withdrawalId + " (Mã NH: " + transferResult.getBankTransactionRef() + ")"
            );

            log.info("[Withdrawal Approve][ID: {}] Tất toán chuyển khoản rút tiền THÀNH CÔNG. Mã NH: {}",
                    withdrawalId, transferResult.getBankTransactionRef());

            return saved;
        } else {
            log.error("[Withdrawal Approve][ID: {}] Chuyển khoản ngân hàng thất bại: {}",
                    withdrawalId, transferResult.getMessage());
            throw new IllegalStateException("Chuyển tiền ngân hàng thất bại: " + transferResult.getMessage());
        }
    }

    @Override
    @Transactional
    public Withdrawal rejectWithdrawal(Long withdrawalId, Long adminId, String reason, String currentUserRole) {
        log.info("[Withdrawal Reject][ID: {}] Admin #{} ({}) từ chối lệnh rút tiền. Lý do: {}",
                withdrawalId, adminId, currentUserRole, reason);

        // Yêu cầu 10: Kiểm tra quyền Admin
        if (currentUserRole != null && !"ROLE_ADMIN".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Chỉ quản trị viên (Admin) mới có quyền từ chối lệnh rút tiền.");
        }

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lệnh rút tiền: " + withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối lệnh rút ở trạng thái PENDING. Trạng thái hiện tại: " + withdrawal.getStatus());
        }

        Long shopId = withdrawal.getShopId();
        BigDecimal amount = withdrawal.getAmount();

        Wallet wallet = walletRepository.findByShopIdForUpdate(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của Shop: " + shopId));

        // Yêu cầu 8: Hoàn lại số dư đã đóng băng về số dư khả dụng
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Yêu cầu 7: Cập nhật trạng thái REJECTED
        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setAdminId(adminId);
        withdrawal.setRejectionReason(reason);
        withdrawal.setProcessedAt(LocalDateTime.now());
        Withdrawal saved = withdrawalRepository.save(withdrawal);

        // Yêu cầu 9: Ghi bút toán đảo vào Sổ cái: Nợ WITHDRAWAL_PENDING | Có SELLER_WALLET_{shopId}
        String txId = "WITHDRAWAL_REJECT_" + withdrawalId;
        doubleEntryLedgerService.recordEntry(
                txId,
                LedgerTransactionType.WITHDRAWAL,
                LedgerAccount.WITHDRAWAL_PENDING,
                LedgerAccount.sellerWallet(shopId),
                amount,
                "Từ chối lệnh rút tiền #" + withdrawalId + ". Hoàn tiền vào ví Seller: " + reason
        );

        log.info("[Withdrawal Reject][ID: {}] Đã từ chối lệnh rút tiền và hoàn tiền về ví cho Shop #{} thành công.",
                withdrawalId, shopId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> getWithdrawalsByShopId(Long shopId, Long currentUserId, String currentUserRole) {
        if (currentUserRole != null && "ROLE_BUYER".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Người mua không có quyền xem lịch sử rút tiền của gian hàng.");
        }
        return withdrawalRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public Withdrawal getWithdrawalById(Long withdrawalId, Long currentUserId, String currentUserRole) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lệnh rút tiền: " + withdrawalId));

        if (currentUserRole != null && "ROLE_BUYER".equalsIgnoreCase(currentUserRole)) {
            throw new ForbiddenException("Người mua không có quyền xem thông tin lệnh rút tiền.");
        }

        return withdrawal;
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        BigDecimal total = wallet.getBalance().add(wallet.getFrozenBalance());
        return WalletResponse.builder()
                .id(wallet.getId())
                .shopId(wallet.getShopId())
                .balance(wallet.getBalance())
                .frozenBalance(wallet.getFrozenBalance())
                .totalBalance(total)
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
