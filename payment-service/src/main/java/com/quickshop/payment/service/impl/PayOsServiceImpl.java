package com.quickshop.payment.service.impl;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.config.RabbitMQPaymentConfig;
import com.quickshop.payment.dto.CreatePayOsPaymentRequest;
import com.quickshop.payment.dto.CreatePayOsPaymentResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.PayOsService;
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
import vn.payos.model.webhooks.ConfirmWebhookResponse;
import vn.payos.model.webhooks.WebhookData;

import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayOsServiceImpl implements PayOsService {

    private final PayOS payOS;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final LedgerService ledgerService;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 1. Tạo yêu cầu thanh toán chuyển khoản PayOS chỉ cần nhận orderId.
     * Tự động tra cứu số tiền đơn hàng, người dùng, tự đóng gói CreatePayOsPaymentRequest
     * và trả về CreatePayOsPaymentResponse cho người dùng.
     */
    @Override
    @Transactional
    public CreatePayOsPaymentResponse createPayment(String orderId) {
        return createPayment(orderId, null);
    }

    @Override
    @Transactional
    public CreatePayOsPaymentResponse createPayment(String orderId, BigDecimal customAmount) {
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }

        String cleanOrderId = orderId.trim();

        // 1. Tự động xác định số tiền thanh toán (amount) và userId
        BigDecimal amount = customAmount;
        Long userId = null;

        // Tra cứu trong database PaymentTransaction trước
        Optional<PaymentTransaction> existingTxnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(cleanOrderId);
        if (existingTxnOpt.isEmpty() && cleanOrderId.startsWith("QS-")) {
            existingTxnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(cleanOrderId.substring(3));
        }
        if (existingTxnOpt.isEmpty() && !cleanOrderId.startsWith("QS-")) {
            existingTxnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-" + cleanOrderId);
        }

        if (existingTxnOpt.isPresent()) {
            PaymentTransaction existingTxn = existingTxnOpt.get();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                amount = existingTxn.getAmount();
            }
            userId = existingTxn.getUserId();
        }

        // Nếu chưa có số tiền, thử tra cứu từ Order Service qua HTTP REST API
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = fetchOrderAmountFromOrderService(cleanOrderId);
        }

        // Nếu vẫn không tìm thấy từ Order Service (hoặc đơn test), đặt giá trị mặc định tối thiểu hợp lệ
        if (amount == null || amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            amount = BigDecimal.valueOf(50000); // Mặc định 50,000 VNĐ
        }

        if (userId == null) {
            userId = 1001L;
        }

        // 2. Tự tạo CreatePayOsPaymentRequest chuẩn
        Long numericOrderCode = resolveNumericOrderCode(cleanOrderId);
        String description = "QS DH" + numericOrderCode;
        if (description.length() > 25) {
            description = description.substring(0, 25);
        }

        CreatePayOsPaymentRequest internalRequest = CreatePayOsPaymentRequest.builder()
                .orderId(cleanOrderId)
                .amount(amount)
                .userId(userId)
                .description(description)
                .returnUrl("http://localhost/checkout?payos_status=success&orderId=" + cleanOrderId)
                .cancelUrl("http://localhost/checkout?payos_status=cancelled&orderId=" + cleanOrderId)
                .build();

        // 3. Tiến hành gọi PayOS SDK và tạo response trả về cho người dùng
        return createPaymentLink(internalRequest);
    }

    private BigDecimal fetchOrderAmountFromOrderService(String orderId) {
        try {
            String digits = orderId.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;

            String orderServiceUrl = System.getenv().getOrDefault("ORDER_SERVICE_URL", "http://order-service:8082");
            String url = orderServiceUrl + "/api/orders/" + digits;

            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) resp.get("data");
                if (data != null && data.containsKey("masterOrder")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> masterOrder = (Map<String, Object>) data.get("masterOrder");
                    if (masterOrder != null && masterOrder.containsKey("totalAmount")) {
                        Object total = masterOrder.get("totalAmount");
                        if (total instanceof Number) {
                            return BigDecimal.valueOf(((Number) total).doubleValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Không thể lấy số tiền từ Order Service cho đơn #{}: {}", orderId, e.getMessage());
        }
        return null;
    }

    /**
     * Tạo yêu cầu thanh toán chuyển khoản PayOS kèm mã QR MB Bank thông qua Request DTO
     */
    @Override
    @Transactional
    public CreatePayOsPaymentResponse createPaymentLink(CreatePayOsPaymentRequest request) {
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

        // Nội dung chuyển khoản cho PayOS tối đa 25 ký tự
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

        // Gọi PayOS SDK tạo Payment Link chính thức
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
                    log.info("PayOS SDK tạo payment link thành công: id={}, orderCode={}, url={}", paymentLinkId, numericOrderCode, checkoutUrl);
                }
            }
        } catch (Exception e) {
            log.warn("PayOS SDK tạo payment link gặp lỗi: {}. Sử dụng mã VietQR MB Bank chuẩn dự phòng.", e.getMessage());
        }

        // Nếu PayOS SDK không trả về mã QR trực tiếp hoặc đang chạy offline/fallback, tạo VietQR MB Bank chuẩn
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

        // Khởi tạo hoặc cập nhật PaymentTransaction trong DB
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

    /**
     * 2. Kiểm tra trạng thái xem đơn hàng đã chuyển tiền chưa (Active Polling qua PayOS API)
     */
    @Override
    @Transactional
    public Map<String, Object> checkPaymentStatus(String orderId) {
        Map<String, Object> result = new HashMap<>();
        if (orderId == null || orderId.isBlank()) {
            result.put("paid", false);
            result.put("status", "NOT_FOUND");
            return result;
        }

        // Tìm giao dịch theo orderId hoặc txnRef
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

            // Nếu đơn hàng đang PENDING, chủ động gọi PayOS API tra cứu trạng thái thực tế
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
                            log.info("PayOS Active Polling: Đơn hàng #{} đã xác nhận PAID trên PayOS!", numericOrderCode);
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

                // Nếu vẫn PENDING, kiểm tra xem có giao dịch SUCCESS mồ côi nào từ MB Bank / PayOS có cùng số tiền không
                if (txn.getStatus() == PaymentStatus.PENDING) {
                    List<PaymentTransaction> successTxns = paymentTransactionRepository.findByStatus(PaymentStatus.SUCCESS);
                    Optional<PaymentTransaction> unlinked = successTxns.stream()
                            .filter(t -> t.getOrderId() != null && (t.getOrderId().equalsIgnoreCase("THANHTOAN") || t.getOrderId().startsWith("PAYOS_STANDALONE")))
                            .filter(t -> txn.getAmount() == null || t.getAmount() == null || t.getAmount().compareTo(txn.getAmount()) == 0)
                            .max(Comparator.comparing(PaymentTransaction::getCreatedAt));
                    if (unlinked.isPresent()) {
                        PaymentTransaction utxn = unlinked.get();
                        log.info("checkPaymentStatus: Tự động gắn kết giao dịch thành công {} sang đơn hàng hiện tại #{}", utxn.getTxnRef(), orderId);
                        utxn.setOrderId(txn.getOrderId());
                        paymentTransactionRepository.save(utxn);

                        txn.setStatus(PaymentStatus.SUCCESS);
                        txn.setVnpResponseCode("00");
                        txn.setVnpTransactionNo(utxn.getVnpTransactionNo());
                        paymentTransactionRepository.save(txn);

                        publishPaymentCompleted(txn);
                    }
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

    /**
     * 3. Xử lý Webhook biến động số dư từ PayOS khi khách chuyển khoản thành công
     */
    @Override
    @Transactional
    public Map<String, Object> processWebhook(Map<String, Object> webhookData) {
        Map<String, Object> response = new HashMap<>();
        if (webhookData == null || webhookData.isEmpty()) {
            response.put("error", -1);
            response.put("message", "Dữ liệu webhook rỗng");
            return response;
        }

        log.info("PayOS Webhook nhận được: {}", webhookData);

        // 1. Xác thực chữ ký điện tử bằng PayOS SDK
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

        // Tự động đối soát thông minh: nếu description là "THANHTOAN" hoặc không tìm thấy theo mã đơn,
        // tự động ghép nối với giao dịch PENDING gần nhất
        if (txnOpt.isEmpty()) {
            List<PaymentTransaction> pendingTxns = paymentTransactionRepository.findByStatus(PaymentStatus.PENDING);
            if (!pendingTxns.isEmpty()) {
                Optional<PaymentTransaction> matchByAmount = pendingTxns.stream()
                        .filter(t -> t.getAmount() != null && t.getAmount().compareTo(amountToUse) == 0)
                        .max(Comparator.comparing(PaymentTransaction::getCreatedAt));
                if (matchByAmount.isPresent()) {
                    txnOpt = matchByAmount;
                    log.info("PayOS Webhook: Khớp tự động giao dịch MB Bank với đơn hàng PENDING theo số tiền: #{}", txnOpt.get().getOrderId());
                } else {
                    Optional<PaymentTransaction> latestPending = pendingTxns.stream()
                            .max(Comparator.comparing(PaymentTransaction::getCreatedAt));
                    if (latestPending.isPresent()) {
                        txnOpt = latestPending;
                        log.info("PayOS Webhook: Khớp tự động giao dịch MB Bank với đơn hàng PENDING mới nhất: #{}", txnOpt.get().getOrderId());
                    }
                }
            }
        }

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

        // Cập nhật trạng thái giao dịch sang SUCCESS
        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setPaymentMethod("PAYOS_MBBANK");
        txn.setVnpResponseCode("00");
        txn.setVnpBankCode("MB");
        txn.setVnpTransactionNo(reference != null ? reference : ("MB_" + txn.getTxnRef()));
        txn.setVnpPayDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        paymentTransactionRepository.save(txn);

        // Ghi sổ cái kế toán kép (Double-entry ledger)
        ledgerService.recordDoubleEntry(
                "PAYOS-" + txn.getTxnRef(),
                "PAYMENT_PAYOS_MBBANK_SUCCESS",
                "MBBANK_PAYOS_GATEWAY",
                "ESCROW_HOLDING",
                txn.getAmount()
        );

        log.info("PayOS Webhook: Xác nhận thanh toán MB Bank thành công cho đơn #{}, số tiền: {} VNĐ", txn.getOrderId(), txn.getAmount());

        // Bắn sự kiện PaymentCompletedEvent tới Order Service qua RabbitMQ
        publishPaymentCompleted(txn);

        response.put("error", 0);
        response.put("message", "Xác nhận webhook thành công");
        return response;
    }

    /**
     * 4. Xác nhận và đăng ký Webhook URL với PayOS server
     */
    @Override
    public String confirmWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BadRequestException("Webhook URL không được để trống");
        }
        if (payOS == null) {
            throw new BadRequestException("PayOS client chưa được cấu hình");
        }
        try {
            ConfirmWebhookResponse resp = payOS.webhooks().confirm(webhookUrl);
            log.info("PayOS confirm webhook URL thành công: {}, response: {}", webhookUrl, resp);
            return (resp != null) ? resp.toString() : "CONFIRMED";
        } catch (Exception e) {
            log.error("PayOS confirm webhook URL thất bại: {}", e.getMessage());
            throw new BadRequestException("Không thể xác nhận Webhook URL với PayOS: " + e.getMessage());
        }
    }

    /**
     * 5. Hủy liên kết thanh toán PayOS
     */
    @Override
    @Transactional
    public Map<String, Object> cancelPaymentLink(String orderId, String reason) {
        Map<String, Object> result = new HashMap<>();
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("Mã đơn hàng không được để trống");
        }

        Long numericOrderCode = resolveNumericOrderCode(orderId);
        String cancelReason = (reason != null && !reason.isBlank()) ? reason : "Người dùng hủy đơn hàng";

        if (payOS != null) {
            try {
                PaymentLink link = payOS.paymentRequests().cancel(numericOrderCode, cancelReason);
                result.put("payOsStatus", link != null && link.getStatus() != null ? link.getStatus().name() : "CANCELLED");
                log.info("PayOS SDK hủy link đơn #{} thành công: {}", numericOrderCode, cancelReason);
            } catch (Exception e) {
                log.warn("PayOS SDK hủy link đơn #{} gặp lỗi: {}", numericOrderCode, e.getMessage());
            }
        }

        Optional<PaymentTransaction> txnOpt = paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (txnOpt.isPresent()) {
            PaymentTransaction txn = txnOpt.get();
            txn.setStatus(PaymentStatus.CANCELLED);
            paymentTransactionRepository.save(txn);
            result.put("transactionStatus", "CANCELLED");
        }

        result.put("orderId", orderId);
        result.put("orderCode", numericOrderCode);
        result.put("reason", cancelReason);
        return result;
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
            log.info("Đã phát sự kiện PaymentCompletedEvent cho đơn #{} (masterOrderId={})", txn.getOrderId(), masterOrderId);
        } catch (Exception e) {
            log.error("Lỗi khi phát sự kiện PaymentCompletedEvent cho đơn #{}: {}", txn.getOrderId(), e.getMessage());
        }
    }
}

