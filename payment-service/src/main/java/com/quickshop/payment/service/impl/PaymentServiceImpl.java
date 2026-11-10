package com.quickshop.payment.service.impl;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.event.PaymentFailedEvent;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.config.RabbitMQPaymentConfig;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.EscrowAccount;
import com.quickshop.payment.entity.LedgerEntry;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.entity.Wallet;
import com.quickshop.payment.entity.Withdrawal;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.PaymentService;
import com.quickshop.payment.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final WalletRepository walletRepository;
    private final EscrowAccountRepository escrowAccountRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final LedgerService ledgerService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VNPayConfig vnPayConfig;
    private final RabbitTemplate rabbitTemplate;
    private final PayOS payOS;

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
                    ledgerService.recordDoubleEntry(
                            "ESCROW-HOLD-" + request.getSubOrderId(),
                            "ESCROW_HOLD",
                            "CUSTOMER_PAYMENT",
                            "ESCROW_HOLDING",
                            request.getAmount()
                    );

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
        }

        // Ghi sổ cái phân bổ doanh thu Shop và hoa hồng Sàn bảo đảm Nợ/Có cân bằng
        Map<String, BigDecimal> creditSplits = new LinkedHashMap<>();
        if (request.getShopId() != null && netAmount.compareTo(BigDecimal.ZERO) > 0) {
            creditSplits.put("SHOP_WALLET_" + request.getShopId(), netAmount);
        }
        if (escrow.getCommissionFee() != null && escrow.getCommissionFee().compareTo(BigDecimal.ZERO) > 0) {
            creditSplits.put("PLATFORM_REVENUE", escrow.getCommissionFee());
        }

        if (!creditSplits.isEmpty()) {
            ledgerService.recordSplitCreditEntry(
                    "ESCROW-REL-" + request.getSubOrderId(),
                    "ESCROW_RELEASE",
                    "ESCROW_HOLDING",
                    creditSplits
            );
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
        ledgerService.recordDoubleEntry(
                "ESCROW-REFUND-" + request.getSubOrderId(),
                "ESCROW_REFUND",
                "ESCROW_HOLDING",
                "CUSTOMER_REFUND",
                escrow.getAmount()
        );

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
        ledgerService.recordDoubleEntry(
                "WITHDRAWAL-REQ-" + saved.getId(),
                "WITHDRAWAL_REQUEST",
                "SHOP_WALLET_" + request.getShopId(),
                "WITHDRAWAL_PENDING",
                request.getAmount()
        );

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

            ledgerService.recordDoubleEntry(
                    "WITHDRAWAL-REJ-" + withdrawal.getId(),
                    "WITHDRAWAL_REJECTED",
                    "WITHDRAWAL_PENDING",
                    "SHOP_WALLET_" + withdrawal.getShopId(),
                    withdrawal.getAmount()
            );

            log.info("Rejected withdrawal id={}, refunded {} to shop {}", withdrawal.getId(), withdrawal.getAmount(), withdrawal.getShopId());
        } else {
            // Duyệt / Hoàn tất chuyển khoản: trừ dứt điểm khỏi frozenBalance
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(withdrawal.getAmount()).max(BigDecimal.ZERO));
            walletRepository.save(wallet);

            withdrawal.setStatus("COMPLETED");
            withdrawal.setAdminId(request.getAdminId());
            withdrawal.setProcessedAt(LocalDateTime.now());

            ledgerService.recordDoubleEntry(
                    "WITHDRAWAL-CMP-" + withdrawal.getId(),
                    "WITHDRAWAL_COMPLETED",
                    "WITHDRAWAL_PENDING",
                    "BANK_SETTLEMENT",
                    withdrawal.getAmount()
            );

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
        return ledgerService.getAllEntries();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> getLedgerEntriesByTxn(String transactionId) {
        return ledgerService.getEntriesByTransaction(transactionId);
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

    @Override
    @Transactional
    public Map<String, String> processVNPayIpn(Map<String, String> vnpParams) {
        Map<String, String> response = new HashMap<>();

        if (vnpParams == null || vnpParams.isEmpty()) {
            log.warn("VNPay IPN: Không nhận được tham số nào từ VNPay");
            response.put("RspCode", "99");
            response.put("Message", "Empty params");
            return response;
        }

        // 1. Kiểm tra chữ ký số (Checksum)
        if (!VNPayUtil.verifyChecksum(vnpParams, vnPayConfig.getHashSecret())) {
            log.warn("VNPay IPN: Chữ ký số không hợp lệ! vnp_SecureHash: {}", vnpParams.get("vnp_SecureHash"));
            response.put("RspCode", "97");
            response.put("Message", "Invalid Checksum");
            return response;
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            log.warn("VNPay IPN: Thiếu vnp_TxnRef trong tham số");
            response.put("RspCode", "01");
            response.put("Message", "Order not Found");
            return response;
        }

        // 2. Tìm kiếm giao dịch trong DB
        Optional<PaymentTransaction> optTxn = paymentTransactionRepository.findByTxnRef(txnRef);
        if (optTxn.isEmpty()) {
            log.warn("VNPay IPN: Không tìm thấy giao dịch với txnRef: {}", txnRef);
            response.put("RspCode", "01");
            response.put("Message", "Order not Found");
            return response;
        }

        PaymentTransaction txn = optTxn.get();

        // 3. Kiểm tra số tiền (vnp_Amount = amount * 100)
        String vnpAmountStr = vnpParams.get("vnp_Amount");
        long vnpAmount = (vnpAmountStr != null && !vnpAmountStr.isBlank()) ? Long.parseLong(vnpAmountStr) : 0L;
        long expectedAmount = txn.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (vnpAmount != expectedAmount) {
            log.warn("VNPay IPN: Số tiền không khớp! Nhận: {}, Kỳ vọng: {}", vnpAmount, expectedAmount);
            response.put("RspCode", "04");
            response.put("Message", "Invalid Amount");
            return response;
        }

        // 4. Kiểm tra tính Idempotent (Chống xử lý trùng)
        if (txn.getStatus() != PaymentStatus.PENDING) {
            log.info("VNPay IPN: Giao dịch {} đã được cập nhật trước đó với trạng thái: {}", txnRef, txn.getStatus());
            response.put("RspCode", "02");
            response.put("Message", "Order already confirmed");
            return response;
        }

        // 5. Cập nhật trạng thái giao dịch
        String responseCode = vnpParams.get("vnp_ResponseCode");
        String transactionNo = vnpParams.get("vnp_TransactionNo");
        String bankCode = vnpParams.get("vnp_BankCode");
        String cardType = vnpParams.get("vnp_CardType");
        String payDate = vnpParams.get("vnp_PayDate");

        txn.setVnpResponseCode(responseCode);
        txn.setVnpTransactionNo(transactionNo);
        txn.setVnpBankCode(bankCode);
        txn.setVnpCardType(cardType);
        txn.setVnpPayDate(payDate);

        if ("00".equals(responseCode)) {
            txn.setStatus(PaymentStatus.SUCCESS);
            paymentTransactionRepository.save(txn);
            log.info("VNPay IPN: Thanh toán thành công cho đơn hàng #{}, txnRef={}", txn.getOrderId(), txnRef);

            // Ghi nhận sổ cái kép cho giao dịch thanh toán VNPay thành công
            ledgerService.recordDoubleEntry(
                    "VNPAY-PAY-" + txn.getTxnRef(),
                    "PAYMENT_VNPAY_SUCCESS",
                    "VNPAY_GATEWAY",
                    "ESCROW_HOLDING",
                    txn.getAmount()
            );

            // Bắn PaymentCompletedEvent để Order Saga xác nhận đơn hàng
            publishPaymentCompleted(txn);
        } else {
            txn.setStatus("24".equals(responseCode) ? PaymentStatus.CANCELLED : PaymentStatus.FAILED);
            paymentTransactionRepository.save(txn);
            log.warn("VNPay IPN: Thanh toán thất bại/hủy cho đơn hàng #{}, mã lỗi: {}", txn.getOrderId(), responseCode);

            // Bắn PaymentFailedEvent để Order Saga hoàn lại hàng tồn kho
            publishPaymentFailed(txn, "VNPay response code: " + responseCode);
        }

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return response;
    }

    @Override
    @Transactional
    public PaymentTransactionResponse processVNPayReturn(Map<String, String> vnpParams) {
        if (vnpParams == null || vnpParams.isEmpty()) {
            throw new BadRequestException("Không nhận được tham số phản hồi từ VNPay");
        }

        // 1. Kiểm tra chữ ký số
        if (!VNPayUtil.verifyChecksum(vnpParams, vnPayConfig.getHashSecret())) {
            log.warn("VNPay Return: Chữ ký số không hợp lệ!");
            throw new BadRequestException("Chữ ký số VNPay không hợp lệ hoặc dữ liệu bị thay đổi");
        }

        String txnRef = vnpParams.get("vnp_TxnRef");
        PaymentTransaction txn = paymentTransactionRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + txnRef));

        // Nếu trạng thái vẫn PENDING (IPN chưa đến hoặc chạy chậm hơn Return URL), cập nhật luôn
        if (txn.getStatus() == PaymentStatus.PENDING) {
            String responseCode = vnpParams.get("vnp_ResponseCode");
            String transactionNo = vnpParams.get("vnp_TransactionNo");
            String bankCode = vnpParams.get("vnp_BankCode");
            String cardType = vnpParams.get("vnp_CardType");
            String payDate = vnpParams.get("vnp_PayDate");

            txn.setVnpResponseCode(responseCode);
            txn.setVnpTransactionNo(transactionNo);
            txn.setVnpBankCode(bankCode);
            txn.setVnpCardType(cardType);
            txn.setVnpPayDate(payDate);

            if ("00".equals(responseCode)) {
                txn.setStatus(PaymentStatus.SUCCESS);
                paymentTransactionRepository.save(txn);
                log.info("VNPay Return: Xác nhận thanh toán thành công cho đơn #{}, txnRef={}", txn.getOrderId(), txnRef);

                // Ghi nhận sổ cái kép cho giao dịch thanh toán VNPay thành công
                ledgerService.recordDoubleEntry(
                        "VNPAY-PAY-" + txn.getTxnRef(),
                        "PAYMENT_VNPAY_SUCCESS",
                        "VNPAY_GATEWAY",
                        "ESCROW_HOLDING",
                        txn.getAmount()
                );

                publishPaymentCompleted(txn);
            } else {
                txn.setStatus("24".equals(responseCode) ? PaymentStatus.CANCELLED : PaymentStatus.FAILED);
                paymentTransactionRepository.save(txn);
                log.warn("VNPay Return: Giao dịch không thành công cho đơn #{}, code={}", txn.getOrderId(), responseCode);
                publishPaymentFailed(txn, "VNPay response code: " + responseCode);
            }
        }

        return mapToTransactionResponse(txn);
    }

    private void publishPaymentCompleted(PaymentTransaction txn) {
        try {
            Long masterOrderId = null;
            try {
                String digits = txn.getOrderId().replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    masterOrderId = Long.parseLong(digits);
                }
            } catch (Exception ignored) {}

            String method = txn.getPaymentMethod() != null ? txn.getPaymentMethod() : "PAYOS_MBBANK";

            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .orderId(txn.getOrderId())
                    .masterOrderId(masterOrderId)
                    .userId(txn.getUserId())
                    .amount(txn.getAmount())
                    .paymentMethod(method)
                    .transactionId(txn.getTxnRef())
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQPaymentConfig.EXCHANGE_ORDER,
                    RabbitMQPaymentConfig.ROUTING_KEY_PAYMENT_COMPLETED,
                    event
            );
            log.info("Đã phát PaymentCompletedEvent tới RabbitMQ cho MasterOrder #{}, method={}", txn.getOrderId(), method);
        } catch (Exception e) {
            log.error("Lỗi khi phát PaymentCompletedEvent cho đơn #{}: {}", txn.getOrderId(), e.getMessage(), e);
        }
    }

    private void publishPaymentFailed(PaymentTransaction txn, String reason) {
        try {
            Long masterOrderId = null;
            try {
                String digits = txn.getOrderId().replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    masterOrderId = Long.parseLong(digits);
                }
            } catch (Exception ignored) {}

            PaymentFailedEvent event = PaymentFailedEvent.builder()
                    .orderId(txn.getOrderId())
                    .masterOrderId(masterOrderId)
                    .userId(txn.getUserId())
                    .amount(txn.getAmount())
                    .paymentMethod(txn.getPaymentMethod() != null ? txn.getPaymentMethod() : "PAYOS_MBBANK")
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQPaymentConfig.EXCHANGE_ORDER,
                    RabbitMQPaymentConfig.ROUTING_KEY_PAYMENT_FAILED,
                    event
            );
            log.info("Đã phát PaymentFailedEvent tới RabbitMQ cho MasterOrder #{}", txn.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi phát PaymentFailedEvent cho đơn #{}: {}", txn.getOrderId(), e.getMessage(), e);
        }
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

    private Long resolveNumericOrderCode(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return (System.currentTimeMillis() % 100000000L);
        }
        String digits = orderId.replaceAll("[^0-9]", "");
        if (!digits.isEmpty() && digits.length() <= 15) {
            try {
                long val = Long.parseLong(digits);
                if (val > 0) return val;
            } catch (NumberFormatException ignored) {}
        }
        return Math.abs((long) orderId.hashCode() % 100000000L) + 100000L;
    }

    @Override
    @Transactional
    public CreatePayOsPaymentResponse createPayOsPayment(CreatePayOsPaymentRequest request) {
        if (request == null || request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new BadRequestException("Số tiền thanh toán tối thiểu là 1.000 VNĐ");
        }

        String orderId = request.getOrderId().trim();
        BigDecimal amount = request.getAmount();
        Long numericOrderCode = resolveNumericOrderCode(orderId);

        String returnUrl = (request.getReturnUrl() != null && !request.getReturnUrl().isBlank())
                ? request.getReturnUrl()
                : "http://localhost/checkout?payos_status=success&orderId=" + orderId;
        String cancelUrl = (request.getCancelUrl() != null && !request.getCancelUrl().isBlank())
                ? request.getCancelUrl()
                : "http://localhost/checkout?payos_status=cancelled&orderId=" + orderId;

        // Description cho payOS tối đa 25 ký tự
        String description = "QS DH" + numericOrderCode;
        if (description.length() > 25) {
            description = description.substring(0, 25);
        }

        String checkoutUrl = null;
        String qrCode = null;
        String accountNumber = "0346746521";
        String accountName = "NGUYEN VAN KIEN";
        String bin = "970422";
        String paymentLinkId = null;

        // 1. Thử tạo link qua PayOS SDK
        try {
            if (payOS != null) {
                PaymentLinkItem item = PaymentLinkItem.builder()
                        .name("Don hang #" + orderId)
                        .quantity(1)
                        .price(amount.longValue())
                        .build();

                CreatePaymentLinkRequest payOsReq = CreatePaymentLinkRequest.builder()
                        .orderCode(numericOrderCode)
                        .amount(amount.longValue())
                        .description(description)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .items(List.of(item))
                        .build();

                CreatePaymentLinkResponse payOsResp = payOS.paymentRequests().create(payOsReq);
                if (payOsResp != null) {
                    checkoutUrl = payOsResp.getCheckoutUrl();
                    qrCode = payOsResp.getQrCode();
                    if (payOsResp.getAccountNumber() != null) accountNumber = payOsResp.getAccountNumber();
                    if (payOsResp.getAccountName() != null) accountName = payOsResp.getAccountName();
                    if (payOsResp.getBin() != null) bin = payOsResp.getBin();
                    paymentLinkId = payOsResp.getPaymentLinkId();
                    log.info("PayOS SDK tạo link thành công: paymentLinkId={}, checkoutUrl={}", paymentLinkId, checkoutUrl);
                }
            }
        } catch (Exception e) {
            log.warn("PayOS SDK tạo link gặp lỗi: {}. Sử dụng mã VietQR MB Bank chuẩn dự phòng.", e.getMessage());
        }

        // 2. Nếu PayOS SDK chưa trả về mã QR hoặc đang test localhost, dùng VietQR MB Bank chuẩn
        if (qrCode == null || qrCode.isBlank()) {
            String vietQrUrl = "https://img.vietqr.io/image/" + bin + "-" + accountNumber + "-compact2.png"
                    + "?amount=" + amount.longValue()
                    + "&addInfo=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                    + "&accountName=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
            qrCode = vietQrUrl;
            if (checkoutUrl == null) {
                checkoutUrl = vietQrUrl;
            }
        }

        // 3. Khởi tạo / cập nhật bản ghi PaymentTransaction
        PaymentTransaction txn = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseGet(() -> PaymentTransaction.builder()
                        .orderId(orderId)
                        .userId(request.getUserId())
                        .build());

        txn.setTxnRef(String.valueOf(numericOrderCode));
        txn.setAmount(amount);
        txn.setPaymentMethod("PAYOS_MBBANK");
        txn.setStatus(PaymentStatus.PENDING);
        txn.setOrderInfo("Thanh toan PayOS MB Bank don hang #" + orderId);
        txn.setPaymentUrl(checkoutUrl);
        txn.setBankCode("MB");
        txn.setExpireAt(LocalDateTime.now().plusMinutes(15));
        paymentTransactionRepository.save(txn);

        return CreatePayOsPaymentResponse.builder()
                .orderCode(numericOrderCode)
                .orderId(orderId)
                .amount(amount)
                .checkoutUrl(checkoutUrl)
                .qrCode(qrCode)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .bin(bin)
                .paymentLinkId(paymentLinkId)
                .status("PENDING")
                .build();
    }

    @Override
    @Transactional
    public Map<String, Object> checkPaymentStatus(String orderId) {
        Map<String, Object> result = new HashMap<>();
        if (orderId == null || orderId.isBlank()) {
            result.put("paid", false);
            result.put("status", "NOT_FOUND");
            return result;
        }

        // Tìm theo orderId chính xác hoặc thử các biến thể tiền tố QS- hoặc txnRef
        Optional<PaymentTransaction> txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (txnOpt.isEmpty() && orderId.startsWith("QS-")) {
            txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId.substring(3));
        }
        if (txnOpt.isEmpty() && !orderId.startsWith("QS-")) {
            txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-" + orderId);
        }
        if (txnOpt.isEmpty()) {
            txnOpt = paymentTransactionRepository.findByTxnRef(orderId);
        }

        if (txnOpt.isPresent()) {
            PaymentTransaction txn = txnOpt.get();

            // Nếu giao dịch đang PENDING, chủ động gọi sang PayOS API kiểm tra trạng thái
            if (txn.getStatus() == PaymentStatus.PENDING) {
                try {
                    Long numericOrderCode = null;
                    if (txn.getTxnRef() != null && txn.getTxnRef().matches("\\d+")) {
                        numericOrderCode = Long.parseLong(txn.getTxnRef());
                    } else {
                        numericOrderCode = resolveNumericOrderCode(txn.getOrderId());
                    }

                    if (numericOrderCode != null && payOS != null) {
                        PaymentLink paymentLink = payOS.paymentRequests().get(numericOrderCode);
                        if (paymentLink != null && paymentLink.getStatus() == PaymentLinkStatus.PAID) {
                            log.info("PayOS Active Polling: Giao dịch #{} đã được xác nhận PAID trên PayOS!", numericOrderCode);
                            txn.setStatus(PaymentStatus.SUCCESS);
                            txn.setPaymentMethod("PAYOS_MBBANK");
                            txn.setVnpResponseCode("00");
                            txn.setVnpBankCode("MB");
                            txn.setVnpTransactionNo("PAYOS_" + (paymentLink.getId() != null ? paymentLink.getId() : numericOrderCode));
                            txn.setVnpPayDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                            paymentTransactionRepository.save(txn);

                            ledgerService.recordDoubleEntry(
                                    "PAYOS-" + txn.getTxnRef(),
                                    "PAYMENT_PAYOS_MBBANK_SUCCESS",
                                    "MBBANK_PAYOS_GATEWAY",
                                    "ESCROW_HOLDING",
                                    txn.getAmount()
                            );

                            publishPaymentCompleted(txn);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Chưa có cập nhật từ PayOS API cho đơn #{}: {}", orderId, e.getMessage());
                }
            }

            boolean isPaid = (txn.getStatus() == PaymentStatus.SUCCESS);
            result.put("paid", isPaid);
            result.put("status", txn.getStatus().name());
            result.put("orderId", txn.getOrderId());
            result.put("amount", txn.getAmount());
            result.put("paymentMethod", txn.getPaymentMethod());
            result.put("txnRef", txn.getTxnRef());
            result.put("bankCode", txn.getBankCode() != null ? txn.getBankCode() : "MB");
            result.put("transactionNo", txn.getVnpTransactionNo() != null ? txn.getVnpTransactionNo() : ("MB_" + txn.getTxnRef()));
            result.put("updatedAt", txn.getUpdatedAt() != null ? txn.getUpdatedAt().toString() : null);
        } else {
            result.put("paid", false);
            result.put("status", "PENDING");
            result.put("orderId", orderId);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> processPayOsWebhook(Map<String, Object> webhookData) {
        Map<String, Object> response = new HashMap<>();
        if (webhookData == null || webhookData.isEmpty()) {
            response.put("error", -1);
            response.put("message", "Dữ liệu webhook rỗng");
            return response;
        }

        log.info("PayOS Webhook nhận được: {}", webhookData);

        // 1. Xác thực chữ ký bằng PayOS SDK
        WebhookData verifiedData = null;
        try {
            if (payOS != null) {
                verifiedData = payOS.webhooks().verify(webhookData);
                log.info("PayOS Webhook: Xác thực chữ ký điện tử thành công! Code={}", verifiedData.getCode());
            }
        } catch (Exception e) {
            log.warn("PayOS Webhook: Lỗi xác thực chữ ký PayOS SDK: {}", e.getMessage());
        }

        Long orderCodeNum = null;
        String description = null;
        String reference = null;
        BigDecimal finalPayOsAmount = null;
        String code = null;

        if (verifiedData != null) {
            orderCodeNum = verifiedData.getOrderCode();
            description = verifiedData.getDescription();
            reference = verifiedData.getReference();
            finalPayOsAmount = BigDecimal.valueOf(verifiedData.getAmount());
            code = verifiedData.getCode();
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            if (data == null) {
                data = webhookData;
            }
            try {
                Object oc = data.get("orderCode");
                if (oc instanceof Number) orderCodeNum = ((Number) oc).longValue();
                else if (oc != null) orderCodeNum = Long.parseLong(String.valueOf(oc));
            } catch (Exception ignored) {}
            description = String.valueOf(data.getOrDefault("description", ""));
            reference = String.valueOf(data.getOrDefault("reference", "MB_" + System.currentTimeMillis()));
            Object amountObj = data.get("amount");
            if (amountObj instanceof Number) {
                finalPayOsAmount = BigDecimal.valueOf(((Number) amountObj).doubleValue());
            }
            code = String.valueOf(data.getOrDefault("code", "00"));
        }

        if (code != null && !code.equals("00")) {
            log.warn("PayOS Webhook: Giao dịch không thành công với mã code={}", code);
            response.put("error", -1);
            response.put("message", "Giao dịch PayOS không thành công (code " + code + ")");
            return response;
        }

        String orderCodeStr = (orderCodeNum != null) ? String.valueOf(orderCodeNum) : "";
        String targetOrderId = orderCodeStr;
        if (description != null && !description.isBlank()) {
            if (description.contains("DH")) {
                targetOrderId = description.substring(description.indexOf("DH") + 2).trim();
            } else if (description.contains("QS")) {
                targetOrderId = description.substring(description.indexOf("QS") + 2).trim();
            }
        }

        // Tìm giao dịch theo txnRef (orderCode) hoặc orderId
        Optional<PaymentTransaction> txnOpt = Optional.empty();
        if (orderCodeStr != null && !orderCodeStr.isBlank()) {
            txnOpt = paymentTransactionRepository.findByTxnRef(orderCodeStr);
        }
        if (txnOpt.isEmpty() && targetOrderId != null && !targetOrderId.isBlank()) {
            txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(targetOrderId);
        }
        if (txnOpt.isEmpty() && targetOrderId != null && !targetOrderId.startsWith("QS-")) {
            txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-" + targetOrderId);
        }

        final BigDecimal amountToUse = (finalPayOsAmount != null && finalPayOsAmount.compareTo(BigDecimal.ZERO) > 0)
                ? finalPayOsAmount
                : BigDecimal.valueOf(50000);

        final String finalTargetId = (targetOrderId != null && !targetOrderId.isBlank()) ? targetOrderId : orderCodeStr;

        PaymentTransaction txn = txnOpt.orElseGet(() -> PaymentTransaction.builder()
                .orderId(finalTargetId)
                .txnRef(orderCodeStr != null && !orderCodeStr.isBlank() ? orderCodeStr : "PAYOS-" + System.currentTimeMillis())
                .amount(amountToUse)
                .paymentMethod("PAYOS_MBBANK")
                .status(PaymentStatus.PENDING)
                .orderInfo("Thanh toan PayOS MB Bank don hang #" + finalTargetId)
                .bankCode("MB")
                .build());

        // IDEMPOTENCY: Nếu đơn hàng đã SUCCESS, return ngay để tránh ghi sổ đúp
        if (txn.getStatus() == PaymentStatus.SUCCESS) {
            log.info("PayOS Webhook: Giao dịch đơn #{} đã ở trạng thái SUCCESS, bỏ qua xử lý lặp lại (Idempotent).", txn.getOrderId());
            response.put("error", 0);
            response.put("message", "Giao dịch đã được xác nhận trước đó (Idempotent)");
            return response;
        }

        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setPaymentMethod("PAYOS_MBBANK");
        txn.setVnpResponseCode("00");
        txn.setVnpBankCode("MB");
        txn.setVnpTransactionNo(reference != null ? reference : "MB_" + System.currentTimeMillis());
        txn.setVnpPayDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        paymentTransactionRepository.save(txn);

        log.info("PayOS Webhook: Xác nhận thanh toán MB Bank THÀNH CÔNG cho đơn #{}, số tiền: {}", txn.getOrderId(), txn.getAmount());

        ledgerService.recordDoubleEntry(
                "PAYOS-" + txn.getTxnRef(),
                "PAYMENT_PAYOS_MBBANK_SUCCESS",
                "MBBANK_PAYOS_GATEWAY",
                "ESCROW_HOLDING",
                txn.getAmount()
        );

        publishPaymentCompleted(txn);

        response.put("error", 0);
        response.put("message", "Xác nhận webhook thành công");
        return response;
    }

    @Override
    @Transactional
    public PaymentTransactionResponse simulatePaymentSuccess(String orderId, BigDecimal amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }

        BigDecimal finalAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                ? amount
                : BigDecimal.valueOf(100000);

        PaymentTransaction txn = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseGet(() -> PaymentTransaction.builder()
                        .orderId(orderId)
                        .txnRef("MB-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000))
                        .amount(finalAmount)
                        .paymentMethod("PAYOS_MBBANK")
                        .status(PaymentStatus.PENDING)
                        .orderInfo("Thanh toan chuyen khoan MB Bank don hang #" + orderId)
                        .bankCode("MB")
                        .build());

        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setPaymentMethod("PAYOS_MBBANK");
        txn.setVnpResponseCode("00");
        txn.setVnpBankCode("MB");
        txn.setVnpTransactionNo("MB_FAST_" + System.currentTimeMillis());
        txn.setVnpPayDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        paymentTransactionRepository.save(txn);

        log.info("Giả lập chuyển khoản MB Bank thành công cho đơn hàng #{}, số tiền: {}", orderId, txn.getAmount());

        ledgerService.recordDoubleEntry(
                "MBBANK-" + txn.getTxnRef(),
                "PAYMENT_MBBANK_SUCCESS",
                "MBBANK_NAPAS_GATEWAY",
                "ESCROW_HOLDING",
                txn.getAmount()
        );

        publishPaymentCompleted(txn);

        return mapToTransactionResponse(txn);
    }
}
