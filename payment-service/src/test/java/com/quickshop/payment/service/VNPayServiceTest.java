package com.quickshop.payment.service;

import com.quickshop.common.event.PaymentCompletedEvent;
import com.quickshop.common.exception.BadRequestException;
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
import com.quickshop.payment.service.impl.VNPayServiceImpl;
import com.quickshop.payment.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private VNPayServiceImpl vnPayService;

    private static final String TEST_SECRET = "TEST_SECRET_KEY_VNPAY_123456";
    private static final String TEST_TMN = "QUICKSHOP";
    private static final String TEST_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String TEST_RETURN_URL = "http://localhost:5173/payment/vnpay-return";

    @BeforeEach
    void setUp() {
        lenient().when(vnPayConfig.getHashSecret()).thenReturn(TEST_SECRET);
        lenient().when(vnPayConfig.getTmnCode()).thenReturn(TEST_TMN);
        lenient().when(vnPayConfig.getPaymentUrl()).thenReturn(TEST_PAY_URL);
        lenient().when(vnPayConfig.getReturnUrl()).thenReturn(TEST_RETURN_URL);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("1. Tạo URL thanh toán VNPay thành công: Lưu trạng thái PENDING và trả URL kèm chữ ký")
    void testCreatePaymentUrl_Success() {
        VNPayCreatePaymentRequest request = VNPayCreatePaymentRequest.builder()
                .orderId(1001L)
                .amount(new BigDecimal("250000"))
                .orderInfo("Thanh toan don hang 1001")
                .ipAddress("127.0.0.1")
                .build();

        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        VNPayPaymentResponse response = vnPayService.createPaymentUrl(request, servletRequest);

        assertNotNull(response);
        assertEquals(1001L, response.getOrderId());
        assertEquals(new BigDecimal("250000"), response.getAmount());
        assertNotNull(response.getPaymentUrl());
        assertTrue(response.getPaymentUrl().contains("vnp_SecureHash="));
        assertTrue(response.getPaymentUrl().contains("vnp_Amount=25000000")); // Nhân 100
        assertTrue(response.getPaymentUrl().contains("vnp_TmnCode=" + TEST_TMN));

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        PaymentTransaction savedTx = captor.getValue();

        assertEquals(1001L, savedTx.getOrderId());
        assertEquals(PaymentStatus.PENDING, savedTx.getStatus());
        assertEquals(PaymentMethod.VNPAY, savedTx.getPaymentMethod());
        assertEquals(new BigDecimal("250000"), savedTx.getAmount());
    }

    @Test
    @DisplayName("2. Tạo URL thanh toán thất bại nếu số tiền nhỏ hơn 1,000 VND")
    void testCreatePaymentUrl_InvalidAmount_ThrowsException() {
        VNPayCreatePaymentRequest request = VNPayCreatePaymentRequest.builder()
                .orderId(1001L)
                .amount(new BigDecimal("500"))
                .build();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertThrows(BadRequestException.class, () -> vnPayService.createPaymentUrl(request, servletRequest));
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Xử lý IPN THÀNH CÔNG: Chữ ký hợp lệ, số tiền đúng -> Cập nhật SUCCESS và phát event payment.completed")
    void testProcessIpn_ValidPayment_UpdatesToSuccessAndPublishesEvent() {
        String txnRef = "1001_1700000000";
        BigDecimal amount = new BigDecimal("250000");

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(1L)
                .txnRef(txnRef)
                .orderId(1001L)
                .amount(amount)
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.of(transaction));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // Tạo bộ tham số IPN hợp lệ
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "25000000"); // 250,000 * 100
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_BankTranNo", "VNP140001");
        ipnParams.put("vnp_CardType", "ATM");
        ipnParams.put("vnp_OrderInfo", "Thanh toan don hang 1001");
        ipnParams.put("vnp_PayDate", "20261027123000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", TEST_TMN);
        ipnParams.put("vnp_TransactionNo", "14123456");
        ipnParams.put("vnp_TransactionStatus", "00");
        ipnParams.put("vnp_TxnRef", txnRef);

        // Ký chữ ký hợp lệ bằng VNPayUtil
        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, ipnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        ipnParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("00", ipnResponse.getRspCode());
        assertEquals("Confirm Success", ipnResponse.getMessage());

        assertEquals(PaymentStatus.SUCCESS, transaction.getStatus());
        assertEquals("14123456", transaction.getVnpayTransactionNo());
        assertEquals("NCB", transaction.getVnpayBankCode());
        assertEquals("00", transaction.getVnpayResponseCode());

        verify(paymentTransactionRepository).save(transaction);

        // Kiểm tra event PaymentCompletedEvent được bắn đi đúng 2 exchange
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), eventCaptor.capture());

        PaymentCompletedEvent sentEvent = eventCaptor.getValue();
        assertEquals("1001", sentEvent.getOrderId());
        assertEquals(txnRef, sentEvent.getTxnRef());
        assertEquals("14123456", sentEvent.getVnpayTransactionNo());
        assertEquals(amount, sentEvent.getAmount());
        assertEquals("VNPAY", sentEvent.getPaymentMethod());
    }

    @Test
    @DisplayName("4. Xử lý IPN CHỮ KÝ SAI (Invalid Checksum): Trả RspCode 97 và không cập nhật DB hay bắn event")
    void testProcessIpn_InvalidChecksum_Returns97() {
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "25000000");
        ipnParams.put("vnp_TxnRef", "1001_1700000000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_SecureHash", "INVALID_HASH_1234567890ABCDEF");

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("97", ipnResponse.getRspCode());
        assertEquals("Invalid Checksum", ipnResponse.getMessage());

        verify(paymentTransactionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("5. Xử lý IPN ORDER NOT FOUND: Mã giao dịch không tồn tại trả RspCode 01")
    void testProcessIpn_OrderNotFound_Returns01() {
        String txnRef = "NON_EXISTENT_TXN";

        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "25000000");
        ipnParams.put("vnp_TxnRef", txnRef);
        ipnParams.put("vnp_ResponseCode", "00");

        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, ipnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        ipnParams.put("vnp_SecureHash", secureHash);

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.empty());

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("01", ipnResponse.getRspCode());
        assertEquals("Order not found", ipnResponse.getMessage());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("6. Xử lý IPN SAI SỐ TIỀN (Invalid Amount): Số tiền gửi lên không khớp trả RspCode 04")
    void testProcessIpn_InvalidAmount_Returns04() {
        String txnRef = "1001_1700000000";
        BigDecimal expectedAmount = new BigDecimal("250000"); // Kỳ vọng 250,000 VND (vnp_Amount phải là 25000000)

        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(1001L)
                .amount(expectedAmount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.of(transaction));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // VNPay gửi số tiền khác: 100,000 VND (vnp_Amount = 10000000)
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "10000000");
        ipnParams.put("vnp_TxnRef", txnRef);
        ipnParams.put("vnp_ResponseCode", "00");

        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, ipnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        ipnParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("04", ipnResponse.getRspCode());
        assertEquals("Invalid amount", ipnResponse.getMessage());

        assertEquals(PaymentStatus.PENDING, transaction.getStatus());
        verify(paymentTransactionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("7. Xử lý IPN LẶP (Idempotency): Giao dịch đã SUCCESS từ trước trả RspCode 02 và không bắn event lại")
    void testProcessIpn_DuplicateIpn_Returns02_Idempotent() {
        String txnRef = "1001_1700000000";
        BigDecimal amount = new BigDecimal("250000");

        // Giao dịch này đã được xác nhận SUCCESS từ IPN trước đó
        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(1001L)
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.of(transaction));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "25000000");
        ipnParams.put("vnp_TxnRef", txnRef);
        ipnParams.put("vnp_ResponseCode", "00");

        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, ipnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        ipnParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("02", ipnResponse.getRspCode());
        assertEquals("Order already confirmed", ipnResponse.getMessage());

        // Đảm bảo không lưu lại trạng thái hay bắn event trùng lặp
        verify(paymentTransactionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("8. Xử lý IPN THẤT BẠI TỪ CỔNG VNPAY (Khách hủy hoặc lỗi thẻ): Cập nhật FAILED, không bắn event thành công")
    void testProcessIpn_FailedPayment_UpdatesToFailed() {
        String txnRef = "1001_1700000000";
        BigDecimal amount = new BigDecimal("250000");

        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(1001L)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.of(transaction));
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // Khách hàng hủy giao dịch: vnp_ResponseCode = 24
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "25000000");
        ipnParams.put("vnp_TxnRef", txnRef);
        ipnParams.put("vnp_ResponseCode", "24");

        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, ipnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        ipnParams.put("vnp_SecureHash", secureHash);

        VNPayIpnResponse ipnResponse = vnPayService.processIpn(ipnParams);

        assertNotNull(ipnResponse);
        assertEquals("00", ipnResponse.getRspCode()); // Xác nhận đã nhận thông tin kết quả

        assertEquals(PaymentStatus.FAILED, transaction.getStatus());
        verify(paymentTransactionRepository).save(transaction);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("9. Return URL: KHÔNG tin tưởng redirect từ trình duyệt làm bằng chứng thanh toán thành công")
    void testProcessReturn_DoesNotTrustBrowserRedirect() {
        String txnRef = "1001_1700000000";
        BigDecimal amount = new BigDecimal("250000");

        // Transaction trong DB vẫn đang PENDING (vì IPN chưa tới hoặc chưa xác thực)
        PaymentTransaction transaction = PaymentTransaction.builder()
                .txnRef(txnRef)
                .orderId(1001L)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findByTxnRef(txnRef)).thenReturn(Optional.of(transaction));

        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_Amount", "25000000");
        returnParams.put("vnp_TxnRef", txnRef);
        returnParams.put("vnp_ResponseCode", "00");

        String paymentUrl = VNPayUtil.buildPaymentUrl(TEST_PAY_URL, returnParams, TEST_SECRET);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        returnParams.put("vnp_SecureHash", secureHash);

        VNPayReturnResponse returnResponse = vnPayService.processReturn(returnParams);

        assertNotNull(returnResponse);
        assertTrue(returnResponse.isValidSignature());
        // Trạng thái thành công chỉ là false vì chưa có xác nhận từ IPN
        assertFalse(returnResponse.isSuccess());
        assertEquals(PaymentStatus.PENDING, returnResponse.getStatus());
        assertTrue(returnResponse.getMessage().contains("Đang chờ hệ thống đồng bộ qua IPN"));

        // Tuyệt đối không thay đổi trạng thái trong DB hay bắn event
        verify(paymentTransactionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("10. Return URL: Chữ ký không hợp lệ trả validSignature = false")
    void testProcessReturn_InvalidChecksum() {
        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_TxnRef", "1001_1700000000");
        returnParams.put("vnp_SecureHash", "INVALID_CHECKSUM_RETURN");

        VNPayReturnResponse returnResponse = vnPayService.processReturn(returnParams);

        assertNotNull(returnResponse);
        assertFalse(returnResponse.isValidSignature());
        assertFalse(returnResponse.isSuccess());
        assertTrue(returnResponse.getMessage().contains("Chữ ký không hợp lệ"));
    }
}
