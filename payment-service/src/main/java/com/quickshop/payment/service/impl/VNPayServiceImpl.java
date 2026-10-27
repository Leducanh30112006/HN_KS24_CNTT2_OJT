package com.quickshop.payment.service.impl;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.config.RabbitMQPaymentConfig;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.VNPayCreatePaymentRequest;
import com.quickshop.payment.dto.VNPayIpnResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.dto.VNPayReturnResponse;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.entity.enums.PaymentMethod;
import com.quickshop.payment.entity.enums.PaymentStatus;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.service.VNPayService;
import com.quickshop.payment.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Override
    @Transactional
    public VNPayPaymentResponse createPaymentUrl(VNPayCreatePaymentRequest request, HttpServletRequest servletRequest) {
        log.info("[VNPay][OrderID: {}] Bắt đầu tạo URL thanh toán VNPay Sandbox với số tiền: {} VND",
                request.getOrderId(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new BadRequestException("Số tiền thanh toán tối thiểu là 1,000 VND");
        }

        // Tạo mã tham chiếu giao dịch độc nhất: {orderId}_{timestamp}_{random}
        String txnRef = request.getOrderId() + "_" + System.currentTimeMillis();

        // 1. Lưu giao dịch PENDING vào payment_db
        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .orderInfo(request.getOrderInfo() != null && !request.getOrderInfo().isBlank()
                        ? request.getOrderInfo()
                        : "Thanh toan don hang #" + request.getOrderId())
                .build();
        paymentTransactionRepository.save(transaction);

        // 2. Chuẩn bị các tham số theo tài liệu chuẩn VNPay v2.1.0
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime expireTime = now.plusMinutes(15); // SRS: Giữ kho & thanh toán 15 phút

        String vnpCreateDate = now.format(DATE_FORMATTER);
        String vnpExpireDate = expireTime.format(DATE_FORMATTER);

        long vnpAmount = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String ipAddress = request.getIpAddress() != null && !request.getIpAddress().isBlank()
                ? request.getIpAddress()
                : VNPayUtil.getIpAddress(servletRequest);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", transaction.getOrderInfo());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // 3. Tạo chữ ký HMAC-SHA512 và ghép URL hoàn chỉnh
        String paymentUrl = VNPayUtil.buildPaymentUrl(vnPayConfig.getPaymentUrl(), vnpParams, vnPayConfig.getHashSecret());

        log.info("[VNPay][OrderID: {}] Tạo URL thanh toán thành công. TxnRef: {}", request.getOrderId(), txnRef);

        return VNPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(txnRef)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .build();
    }

    @Override
    @Transactional
    public VNPayIpnResponse processIpn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        log.info("[VNPay IPN][TxnRef: {}] Tiếp nhận yêu cầu IPN từ VNPay...", txnRef);

        // 1. Xác minh chữ ký bảo mật HMAC-SHA512
        boolean isValidSignature = VNPayUtil.verifySignature(params, vnPayConfig.getHashSecret());
        if (!isValidSignature) {
            log.error("[VNPay IPN][TxnRef: {}] Xác minh chữ ký THẤT BẠI (Invalid Checksum)!", txnRef);
            return new VNPayIpnResponse("97", "Invalid Checksum");
        }

        // 2. Kiểm tra giao dịch và Order ID có tồn tại trong payment_db không
        if (txnRef == null || txnRef.isBlank()) {
            log.error("[VNPay IPN] Tham số vnp_TxnRef bị trống!");
            return new VNPayIpnResponse("01", "Order not found");
        }

        Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByTxnRef(txnRef);
        if (transactionOpt.isEmpty()) {
            log.error("[VNPay IPN][TxnRef: {}] Không tìm thấy giao dịch trong payment_db!", txnRef);
            return new VNPayIpnResponse("01", "Order not found");
        }

        PaymentTransaction transaction = transactionOpt.get();

        // Khóa phân tán Redis chống race condition khi nhận IPN lặp cùng lúc
        String lockKey = "lock:vnpay:ipn:" + txnRef;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("[VNPay IPN][TxnRef: {}] Yêu cầu IPN đang được xử lý đồng thời bởi tiến trình khác.", txnRef);
            return new VNPayIpnResponse("02", "Order already confirmed");
        }

        try {
            // 3. Kiểm tra số tiền thanh toán (vnp_Amount = amount * 100)
            String vnpAmountStr = params.get("vnp_Amount");
            if (vnpAmountStr == null) {
                log.error("[VNPay IPN][TxnRef: {}] Tham số vnp_Amount bị trống!", txnRef);
                return new VNPayIpnResponse("04", "Invalid amount");
            }

            long vnpAmount;
            try {
                vnpAmount = Long.parseLong(vnpAmountStr);
            } catch (NumberFormatException e) {
                log.error("[VNPay IPN][TxnRef: {}] vnp_Amount không phải là số hợp lệ: {}", txnRef, vnpAmountStr);
                return new VNPayIpnResponse("04", "Invalid amount");
            }

            long expectedAmount = transaction.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            if (vnpAmount != expectedAmount) {
                log.error("[VNPay IPN][TxnRef: {}] Số tiền không khớp! VNPay gửi: {}, Kỳ vọng: {}",
                        txnRef, vnpAmount, expectedAmount);
                return new VNPayIpnResponse("04", "Invalid amount");
            }

            // 4. Xử lý Idempotency: Kiểm tra nếu giao dịch đã được xác nhận trước đó
            if (transaction.getStatus() != PaymentStatus.PENDING) {
                log.info("[VNPay IPN][TxnRef: {}] Giao dịch đã được xử lý trước đó với trạng thái: {}. Bỏ qua IPN lặp.",
                        txnRef, transaction.getStatus());
                return new VNPayIpnResponse("02", "Order already confirmed");
            }

            // 5. Cập nhật thông tin chi tiết từ VNPay
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String bankCode = params.get("vnp_BankCode");
            String bankTranNo = params.get("vnp_BankTranNo");
            String cardType = params.get("vnp_CardType");
            String payDate = params.get("vnp_PayDate");
            String transactionStatus = params.get("vnp_TransactionStatus");

            transaction.setVnpayResponseCode(responseCode);
            transaction.setVnpayTransactionNo(transactionNo);
            transaction.setVnpayBankCode(bankCode);
            transaction.setVnpayBankTranNo(bankTranNo);
            transaction.setVnpayCardType(cardType);
            transaction.setVnpayPayDate(payDate);
            transaction.setVnpayTransactionStatus(transactionStatus);

            // Kiểm tra mã kết quả giao dịch (00 = Thành công)
            if ("00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus == null)) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                paymentTransactionRepository.save(transaction);

                log.info("[VNPay IPN][TxnRef: {}][OrderID: {}] Thanh toán THÀNH CÔNG! Số tiền: {} VND. Đang phát sự kiện...",
                        txnRef, transaction.getOrderId(), transaction.getAmount());

                // 6. Phát sự kiện thanh toán thành công qua RabbitMQ
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .orderId(String.valueOf(transaction.getOrderId()))
                        .txnRef(transaction.getTxnRef())
                        .vnpayTransactionNo(transactionNo)
                        .amount(transaction.getAmount())
                        .paymentMethod("VNPAY")
                        .bankCode(bankCode)
                        .payDate(payDate)
                        .timestamp(LocalDateTime.now())
                        .build();

                // Bắn event vào payment.exchange
                rabbitTemplate.convertAndSend(
                        RabbitMQPaymentConfig.EXCHANGE_PAYMENT,
                        RabbitMQPaymentConfig.ROUTING_KEY_PAYMENT_COMPLETED,
                        event
                );

                // Bắn event vào order.exchange để Order Saga cập nhật đơn hàng
                rabbitTemplate.convertAndSend(
                        RabbitMQPaymentConfig.EXCHANGE_ORDER,
                        "payment.completed",
                        event
                );

                log.info("[VNPay IPN][OrderID: {}] Đã phát sự kiện payment.completed tới RabbitMQ thành công.",
                        transaction.getOrderId());
                return new VNPayIpnResponse("00", "Confirm Success");
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentTransactionRepository.save(transaction);
                log.warn("[VNPay IPN][TxnRef: {}][OrderID: {}] Thanh toán THẤT BẠI! Mã phản hồi VNPay: {}",
                        txnRef, transaction.getOrderId(), responseCode);
                return new VNPayIpnResponse("00", "Confirm Success");
            }
        } finally {
            // Giải phóng lock Redis
            try {
                String currentLock = stringRedisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentLock)) {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.error("[VNPay IPN] Lỗi giải phóng lock Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VNPayReturnResponse processReturn(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");
        log.info("[VNPay Return][TxnRef: {}] Người dùng được redirect về từ trình duyệt...", txnRef);

        // 1. Xác thực chữ ký
        boolean isValidSignature = VNPayUtil.verifySignature(params, vnPayConfig.getHashSecret());
        if (!isValidSignature) {
            log.warn("[VNPay Return][TxnRef: {}] Chữ ký không hợp lệ từ trình duyệt!", txnRef);
            return VNPayReturnResponse.builder()
                    .txnRef(txnRef)
                    .validSignature(false)
                    .success(false)
                    .message("Chữ ký không hợp lệ (Invalid Checksum)")
                    .build();
        }

        // 2. Tra cứu dữ liệu giao dịch trong DB
        Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByTxnRef(txnRef);
        if (transactionOpt.isEmpty()) {
            return VNPayReturnResponse.builder()
                    .txnRef(txnRef)
                    .validSignature(true)
                    .success(false)
                    .message("Không tìm thấy thông tin đơn hàng trong hệ thống")
                    .build();
        }

        PaymentTransaction transaction = transactionOpt.get();
        String responseCode = params.get("vnp_ResponseCode");

        // 3. Nguyên tắc SRS & Yêu cầu: KHÔNG tin tưởng redirect từ trình duyệt làm bằng chứng thanh toán thành công
        // Nếu IPN đã tới trước và cập nhật trạng thái SUCCESS trong DB, hiển thị thành công.
        // Nếu chưa, trạng thái vẫn là PENDING chờ IPN Webhook xác nhận.
        boolean isSuccess = (transaction.getStatus() == PaymentStatus.SUCCESS);
        String message;

        if (isSuccess) {
            message = "Thanh toán thành công. Đơn hàng đã được xác nhận qua cổng thanh toán VNPay.";
        } else if ("00".equals(responseCode)) {
            message = "Giao dịch đã được tiếp nhận tại VNPay. Đang chờ hệ thống đồng bộ qua IPN Webhook.";
        } else {
            message = "Giao dịch thanh toán không thành công tại cổng VNPay.";
        }

        return VNPayReturnResponse.builder()
                .orderId(transaction.getOrderId())
                .txnRef(txnRef)
                .amount(transaction.getAmount())
                .vnpayTransactionNo(params.get("vnp_TransactionNo"))
                .bankCode(params.get("vnp_BankCode"))
                .responseCode(responseCode)
                .status(transaction.getStatus())
                .validSignature(true)
                .success(isSuccess)
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getTransactionByTxnRef(String txnRef) {
        return paymentTransactionRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + txnRef));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransaction getLatestTransactionByOrderId(Long orderId) {
        return paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch cho đơn hàng: " + orderId));
    }
}
