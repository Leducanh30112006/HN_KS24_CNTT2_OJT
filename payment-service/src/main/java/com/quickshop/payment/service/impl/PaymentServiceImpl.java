package com.quickshop.payment.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.LedgerEntryRepository;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.PaymentService;
import com.quickshop.payment.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VNPayConfig vnPayConfig;

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

    @Override
    @Transactional
    public VNPayPaymentResponse createVNPayPayment(CreateVNPayPaymentRequest request, String clientIp) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu yêu cầu thanh toán không hợp lệ");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new BadRequestException("Số tiền thanh toán tối thiểu qua VNPay là 1,000 VND");
        }

        log.info("Bắt đầu khởi tạo giao dịch VNPay cho đơn hàng #{}, số tiền: {}", request.getOrderId(), request.getAmount());

        ZoneId zoneVn = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(zoneVn);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnpCreateDate = now.format(formatter);
        ZonedDateTime expireTime = now.plusMinutes(vnPayConfig.getExpireMinutes());
        String vnpExpireDate = expireTime.format(formatter);

        // Mã tham chiếu giao dịch độc nhất cho VNPay Sandbox: ORDER_{orderId}_{timestamp}
        String txnRef = request.getOrderId() + "_" + System.currentTimeMillis();

        String orderInfo = (request.getOrderInfo() != null && !request.getOrderInfo().isBlank())
                ? request.getOrderInfo()
                : "Thanh toan don hang #" + request.getOrderId();

        long amountInVndCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String effectiveIp = (clientIp != null && !clientIp.isBlank()) ? clientIp : "127.0.0.1";

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVndCents));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", effectiveIp);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        if (request.getBankCode() != null && !request.getBankCode().isBlank()) {
            vnpParams.put("vnp_BankCode", request.getBankCode());
        }

        // Tạo Payment URL kèm chuỗi chữ ký số HMAC-SHA512
        String paymentUrl = VNPayUtil.buildPaymentUrl(vnPayConfig.getPaymentUrl(), vnpParams, vnPayConfig.getHashSecret());

        // Lấy vnp_SecureHash từ URL đã tạo
        String secureHash = "";
        int hashIdx = paymentUrl.lastIndexOf("vnp_SecureHash=");
        if (hashIdx != -1) {
            secureHash = paymentUrl.substring(hashIdx + "vnp_SecureHash=".length());
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .orderInfo(orderInfo)
                .paymentUrl(paymentUrl)
                .secureHash(secureHash)
                .bankCode(request.getBankCode())
                .expireAt(expireTime.toLocalDateTime())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        log.info("Khởi tạo và lưu giao dịch VNPay thành công. TxnRef: {}, OrderId: {}, ID: {}", txnRef, request.getOrderId(), saved.getId());

        return VNPayPaymentResponse.builder()
                .txnRef(txnRef)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentUrl(paymentUrl)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .expireAt(saved.getExpireAt())
                .message("Tạo URL thanh toán VNPay thành công")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransactionByTxnRef(String txnRef) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán với txnRef: " + txnRef));
        return mapToTransactionResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getTransactionByOrderId(String orderId) {
        PaymentTransaction transaction = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch thanh toán cho đơn hàng: " + orderId));
        return mapToTransactionResponse(transaction);
    }

    private PaymentTransactionResponse mapToTransactionResponse(PaymentTransaction txn) {
        return PaymentTransactionResponse.builder()
                .id(txn.getId())
                .txnRef(txn.getTxnRef())
                .orderId(txn.getOrderId())
                .userId(txn.getUserId())
                .amount(txn.getAmount())
                .paymentMethod(txn.getPaymentMethod())
                .status(txn.getStatus())
                .orderInfo(txn.getOrderInfo())
                .paymentUrl(txn.getPaymentUrl())
                .secureHash(txn.getSecureHash())
                .bankCode(txn.getBankCode())
                .vnpResponseCode(txn.getVnpResponseCode())
                .vnpTransactionNo(txn.getVnpTransactionNo())
                .vnpBankCode(txn.getVnpBankCode())
                .vnpCardType(txn.getVnpCardType())
                .vnpPayDate(txn.getVnpPayDate())
                .createdAt(txn.getCreatedAt())
                .updatedAt(txn.getUpdatedAt())
                .expireAt(txn.getExpireAt())
                .build();
    }
}
