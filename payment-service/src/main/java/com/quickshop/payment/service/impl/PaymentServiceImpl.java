package com.quickshop.payment.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional
    public Wallet getWalletByShopId(Long shopId) {
        return walletRepository.findByShopId(shopId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .shopId(shopId)
                        .balance(BigDecimal.ZERO)
                        .frozenBalance(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public EscrowAccount getEscrowBySubOrderId(Long subOrderId) {
        return escrowAccountRepository.findBySubOrderId(subOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản ký quỹ cho subOrderId: " + subOrderId));
    }

    @Override
    @Transactional
    public EscrowAccount holdEscrow(HoldEscrowRequest request) {
        if (request.getSubOrderId() == null || request.getAmount() == null) {
            throw new BadRequestException("Thiếu thông tin subOrderId hoặc số tiền ký quỹ");
        }

        return escrowAccountRepository.findBySubOrderId(request.getSubOrderId())
                .orElseGet(() -> {
                    BigDecimal commission = request.getCommissionFee() != null ? request.getCommissionFee() : BigDecimal.ZERO;
                    EscrowAccount escrow = EscrowAccount.builder()
                            .subOrderId(request.getSubOrderId())
                            .amount(request.getAmount())
                            .commissionFee(commission)
                            .status("HOLDING")
                            .build();
                    EscrowAccount saved = escrowAccountRepository.save(escrow);

                    // Đóng băng số tiền doanh thu dự kiến của Shop
                    if (request.getShopId() != null) {
                        Wallet wallet = getWalletByShopId(request.getShopId());
                        BigDecimal netHolding = request.getAmount().subtract(commission).max(BigDecimal.ZERO);
                        wallet.setFrozenBalance(wallet.getFrozenBalance().add(netHolding));
                        walletRepository.save(wallet);
                    }

                    // Ghi sổ cái kép Ledger
                    ledgerEntryRepository.save(LedgerEntry.builder()
                            .transactionId("ESCROW-HOLD-" + request.getSubOrderId())
                            .transactionType("ESCROW_HOLD")
                            .accountDebit("CUSTOMER_PAYMENT")
                            .accountCredit("ESCROW_HOLDING")
                            .amount(request.getAmount())
                            .build());

                    log.info("Held escrow for subOrder {}: total={}, commission={}",
                            request.getSubOrderId(), request.getAmount(), commission);
                    return saved;
                });
    }

    @Override
    @Transactional
    public EscrowAccount releaseEscrow(ReleaseEscrowRequest request) {
        EscrowAccount escrow = getEscrowBySubOrderId(request.getSubOrderId());
        if (!"HOLDING".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Tài khoản ký quỹ đơn hàng " + request.getSubOrderId() + " không ở trạng thái HOLDING (hiện tại: " + escrow.getStatus() + ")");
        }

        BigDecimal netAmount = escrow.getAmount().subtract(escrow.getCommissionFee()).max(BigDecimal.ZERO);
        escrow.setStatus("RELEASED");
        EscrowAccount updated = escrowAccountRepository.save(escrow);

        // Mở băng và cộng vào số dư ví của Shop
        if (request.getShopId() != null) {
            Wallet wallet = getWalletByShopId(request.getShopId());
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(netAmount).max(BigDecimal.ZERO));
            wallet.setBalance(wallet.getBalance().add(netAmount));
            walletRepository.save(wallet);

            // Ghi sổ cái cho shop
            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId("ESCROW-REL-" + request.getSubOrderId())
                    .transactionType("ESCROW_RELEASE")
                    .accountDebit("ESCROW_HOLDING")
                    .accountCredit("SHOP_WALLET_" + request.getShopId())
                    .amount(netAmount)
                    .build());
        }

        // Ghi sổ cái hoa hồng sàn
        if (escrow.getCommissionFee().compareTo(BigDecimal.ZERO) > 0) {
            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId("COMMISSION-" + request.getSubOrderId())
                    .transactionType("PLATFORM_COMMISSION")
                    .accountDebit("ESCROW_HOLDING")
                    .accountCredit("PLATFORM_REVENUE")
                    .amount(escrow.getCommissionFee())
                    .build());
        }

        log.info("Released escrow for subOrder {}: netToShop={}, platformCommission={}",
                request.getSubOrderId(), netAmount, escrow.getCommissionFee());
        return updated;
    }

    @Override
    @Transactional
    public EscrowAccount refundEscrow(RefundEscrowRequest request) {
        EscrowAccount escrow = getEscrowBySubOrderId(request.getSubOrderId());
        if (!"HOLDING".equalsIgnoreCase(escrow.getStatus())) {
            throw new BadRequestException("Tài khoản ký quỹ đơn hàng " + request.getSubOrderId() + " không ở trạng thái HOLDING");
        }

        BigDecimal netAmount = escrow.getAmount().subtract(escrow.getCommissionFee()).max(BigDecimal.ZERO);
        escrow.setStatus("REFUNDED");
        EscrowAccount updated = escrowAccountRepository.save(escrow);

        // Giảm phần đóng băng của Shop nếu có
        if (request.getShopId() != null) {
            Wallet wallet = getWalletByShopId(request.getShopId());
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(netAmount).max(BigDecimal.ZERO));
            walletRepository.save(wallet);
        }

        // Ghi sổ cái hoàn tiền
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("ESCROW-REFUND-" + request.getSubOrderId())
                .transactionType("ESCROW_REFUND")
                .accountDebit("ESCROW_HOLDING")
                .accountCredit("CUSTOMER_REFUND")
                .amount(escrow.getAmount())
                .build());

        log.info("Refunded escrow for subOrder {}: amount={}", request.getSubOrderId(), escrow.getAmount());
        return updated;
    }

    @Override
    @Transactional
    public Withdrawal requestWithdrawal(WithdrawalRequestDto request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số tiền rút phải lớn hơn 0");
        }

        Wallet wallet = getWalletByShopId(request.getShopId());
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Số dư khả dụng (" + wallet.getBalance() + "đ) không đủ để rút " + request.getAmount() + "đ");
        }

        // Khóa tiền cần rút
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        Withdrawal withdrawal = Withdrawal.builder()
                .shopId(request.getShopId())
                .amount(request.getAmount())
                .bankCode(request.getBankCode())
                .bankAccountNumber(request.getBankAccountNumber())
                .bankAccountName(request.getBankAccountName())
                .status("PENDING")
                .build();
        Withdrawal saved = withdrawalRepository.save(withdrawal);

        // Ghi sổ cái
        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId("WITHDRAWAL-REQ-" + saved.getId())
                .transactionType("WITHDRAWAL_REQUEST")
                .accountDebit("SHOP_WALLET_" + request.getShopId())
                .accountCredit("WITHDRAWAL_PENDING")
                .amount(request.getAmount())
                .build());

        log.info("Shop {} requested withdrawal id={}: amount={}", request.getShopId(), saved.getId(), request.getAmount());
        return saved;
    }

    @Override
    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, WithdrawalActionRequest request) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền ID: " + withdrawalId));

        if (!"PENDING".equalsIgnoreCase(withdrawal.getStatus())) {
            throw new BadRequestException("Yêu cầu rút tiền này đã được xử lý (trạng thái: " + withdrawal.getStatus() + ")");
        }

        Wallet wallet = getWalletByShopId(withdrawal.getShopId());
        String action = request.getAction() != null ? request.getAction().toUpperCase() : "APPROVE";

        if ("REJECT".equals(action)) {
            // Hoàn lại tiền từ frozenBalance về balance
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(withdrawal.getAmount()).max(BigDecimal.ZERO));
            wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
            walletRepository.save(wallet);

            withdrawal.setStatus("REJECTED");
            withdrawal.setAdminId(request.getAdminId());
            withdrawal.setProcessedAt(LocalDateTime.now());

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId("WITHDRAWAL-REJ-" + withdrawal.getId())
                    .transactionType("WITHDRAWAL_REJECTED")
                    .accountDebit("WITHDRAWAL_PENDING")
                    .accountCredit("SHOP_WALLET_" + withdrawal.getShopId())
                    .amount(withdrawal.getAmount())
                    .build());

            log.info("Rejected withdrawal id={}, refunded {} to shop {}", withdrawal.getId(), withdrawal.getAmount(), withdrawal.getShopId());
        } else {
            // Duyệt / Hoàn tất chuyển khoản: trừ dứt điểm khỏi frozenBalance
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(withdrawal.getAmount()).max(BigDecimal.ZERO));
            walletRepository.save(wallet);

            withdrawal.setStatus("COMPLETED");
            withdrawal.setAdminId(request.getAdminId());
            withdrawal.setProcessedAt(LocalDateTime.now());

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId("WITHDRAWAL-CMP-" + withdrawal.getId())
                    .transactionType("WITHDRAWAL_COMPLETED")
                    .accountDebit("WITHDRAWAL_PENDING")
                    .accountCredit("BANK_SETTLEMENT")
                    .amount(withdrawal.getAmount())
                    .build());

            log.info("Completed withdrawal id={}, paid out {} to shop {}", withdrawal.getId(), withdrawal.getAmount(), withdrawal.getShopId());
        }

        return withdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> getWithdrawalsByShop(Long shopId) {
        return withdrawalRepository.findByShopId(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Withdrawal> getPendingWithdrawals() {
        return withdrawalRepository.findByStatus("PENDING");
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getAllLedgerEntries() {
        return ledgerEntryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getLedgerEntriesByTxn(String transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }
}
