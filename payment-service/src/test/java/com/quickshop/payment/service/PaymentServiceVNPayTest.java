package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.payment.config.VNPayConfig;
import com.quickshop.payment.dto.CreateVNPayPaymentRequest;
import com.quickshop.payment.dto.PaymentTransactionResponse;
import com.quickshop.payment.dto.VNPayPaymentResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.repository.EscrowAccountRepository;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.repository.WalletRepository;
import com.quickshop.payment.repository.WithdrawalRepository;
import com.quickshop.payment.service.LedgerService;
import com.quickshop.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceVNPayTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EscrowAccountRepository escrowAccountRepository;

    @Mock
    private WithdrawalRepository withdrawalRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(vnPayConfig.getTmnCode()).thenReturn("QUICKSHOP");
        lenient().when(vnPayConfig.getHashSecret()).thenReturn("TESTSECRETKEYVNPAY2026");
        lenient().when(vnPayConfig.getPaymentUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        lenient().when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:5173/payment/vnpay-return");
        lenient().when(vnPayConfig.getVersion()).thenReturn("2.1.0");
        lenient().when(vnPayConfig.getCommand()).thenReturn("pay");
        lenient().when(vnPayConfig.getCurrCode()).thenReturn("VND");
        lenient().when(vnPayConfig.getOrderType()).thenReturn("other");
        lenient().when(vnPayConfig.getLocale()).thenReturn("vn");
        lenient().when(vnPayConfig.getExpireMinutes()).thenReturn(15);
    }

    @Test
    @DisplayName("createVNPayPayment: Tạo payment URL thành công, nhân 100 số tiền và lưu transaction PENDING")
    void testCreateVNPayPaymentSuccess() {
        CreateVNPayPaymentRequest request = CreateVNPayPaymentRequest.builder()
                .orderId("1001")
                .amount(BigDecimal.valueOf(250000))
                .userId(10L)
                .orderInfo("Thanh toan don hang #1001")
                .bankCode("NCB")
                .build();

        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> {
                    PaymentTransaction t = invocation.getArgument(0);
                    t.setId(1L);
                    return t;
                });

        VNPayPaymentResponse response = paymentService.createVNPayPayment(request, "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("1001");
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentUrl()).contains("vnp_Amount=25000000"); // 250000 * 100
        assertThat(response.getPaymentUrl()).contains("vnp_BankCode=NCB");
        assertThat(response.getPaymentUrl()).contains("vnp_SecureHash=");

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, times(1)).save(captor.capture());

        PaymentTransaction savedTxn = captor.getValue();
        assertThat(savedTxn.getOrderId()).isEqualTo("1001");
        assertThat(savedTxn.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(savedTxn.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedTxn.getPaymentMethod()).isEqualTo("VNPAY");
        assertThat(savedTxn.getSecureHash()).isNotEmpty();
        assertThat(savedTxn.getExpireAt()).isNotNull();
    }

    @Test
    @DisplayName("createVNPayPayment: Quăng BadRequestException khi request null, thiếu orderId hoặc số tiền < 1000")
    void testCreateVNPayPaymentValidation() {
        assertThatThrownBy(() -> paymentService.createVNPayPayment(null, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);

        CreateVNPayPaymentRequest missingOrderId = CreateVNPayPaymentRequest.builder()
                .amount(BigDecimal.valueOf(50000))
                .build();
        assertThatThrownBy(() -> paymentService.createVNPayPayment(missingOrderId, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);

        CreateVNPayPaymentRequest invalidAmount = CreateVNPayPaymentRequest.builder()
                .orderId("1002")
                .amount(BigDecimal.valueOf(500)) // < 1000
                .build();
        assertThatThrownBy(() -> paymentService.createVNPayPayment(invalidAmount, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getTransactionByTxnRef: Tìm thấy giao dịch trả về DTO tương ứng")
    void testGetTransactionByTxnRefFound() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L)
                .txnRef("TXN_12345")
                .orderId("1001")
                .userId(10L)
                .amount(BigDecimal.valueOf(500000))
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .orderInfo("Thanh toan don hang")
                .paymentUrl("https://sandbox.vnpayment.vn/...")
                .secureHash("hash123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentTransactionRepository.findByTxnRef("TXN_12345")).thenReturn(Optional.of(txn));

        PaymentTransactionResponse response = paymentService.getTransactionByTxnRef("TXN_12345");

        assertThat(response).isNotNull();
        assertThat(response.getTxnRef()).isEqualTo("TXN_12345");
        assertThat(response.getOrderId()).isEqualTo("1001");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("getTransactionByTxnRef: Không tìm thấy quăng ResourceNotFoundException")
    void testGetTransactionByTxnRefNotFound() {
        when(paymentTransactionRepository.findByTxnRef("NOT_EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getTransactionByTxnRef("NOT_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getTransactionByOrderId: Tìm thấy giao dịch mới nhất của đơn hàng")
    void testGetTransactionByOrderIdFound() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(2L)
                .txnRef("TXN_99999")
                .orderId("1001")
                .userId(10L)
                .amount(BigDecimal.valueOf(150000))
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("1001")).thenReturn(Optional.of(txn));

        PaymentTransactionResponse response = paymentService.getTransactionByOrderId("1001");

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("1001");
        assertThat(response.getTxnRef()).isEqualTo("TXN_99999");
    }

    @Test
    @DisplayName("getTransactionByOrderId: Không tìm thấy quăng ResourceNotFoundException")
    void testGetTransactionByOrderIdNotFound() {
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("NON_EXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getTransactionByOrderId("NON_EXIST"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("processVNPayIpn: Chữ ký số sai trả về RspCode 97")
    void testProcessVNPayIpnInvalidChecksum() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "TXN_001");
        params.put("vnp_SecureHash", "wrong_hash");

        java.util.Map<String, String> res = paymentService.processVNPayIpn(params);
        assertThat(res.get("RspCode")).isEqualTo("97");
    }

    @Test
    @DisplayName("processVNPayIpn: Giao dịch không tồn tại trả về RspCode 01")
    void testProcessVNPayIpnOrderNotFound() {
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "TXN_NON_EXIST");
        params.put("vnp_Amount", "10000000");

        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String url = com.quickshop.payment.util.VNPayUtil.buildPaymentUrl(baseUrl, params, "TESTSECRETKEYVNPAY2026");
        String secureHash = url.substring(url.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        params.put("vnp_SecureHash", secureHash);

        when(paymentTransactionRepository.findByTxnRef("TXN_NON_EXIST")).thenReturn(Optional.empty());

        java.util.Map<String, String> res = paymentService.processVNPayIpn(params);
        assertThat(res.get("RspCode")).isEqualTo("01");
    }

    @Test
    @DisplayName("processVNPayIpn: Số tiền không khớp trả về RspCode 04")
    void testProcessVNPayIpnInvalidAmount() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L)
                .txnRef("TXN_002")
                .amount(BigDecimal.valueOf(200000)) // Kỳ vọng 20,000,000 VND cents
                .status(PaymentStatus.PENDING)
                .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "TXN_002");
        params.put("vnp_Amount", "15000000"); // Chỉ có 150,000 VND

        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String url = com.quickshop.payment.util.VNPayUtil.buildPaymentUrl(baseUrl, params, "TESTSECRETKEYVNPAY2026");
        String secureHash = url.substring(url.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        params.put("vnp_SecureHash", secureHash);

        when(paymentTransactionRepository.findByTxnRef("TXN_002")).thenReturn(Optional.of(txn));

        java.util.Map<String, String> res = paymentService.processVNPayIpn(params);
        assertThat(res.get("RspCode")).isEqualTo("04");
    }

    @Test
    @DisplayName("processVNPayIpn: Giao dịch đã xử lý trước đó trả về RspCode 02 (Idempotent)")
    void testProcessVNPayIpnIdempotent() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L)
                .txnRef("TXN_003")
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentStatus.SUCCESS) // Đã thành công từ trước
                .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "TXN_003");
        params.put("vnp_Amount", "10000000");

        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String url = com.quickshop.payment.util.VNPayUtil.buildPaymentUrl(baseUrl, params, "TESTSECRETKEYVNPAY2026");
        String secureHash = url.substring(url.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        params.put("vnp_SecureHash", secureHash);

        when(paymentTransactionRepository.findByTxnRef("TXN_003")).thenReturn(Optional.of(txn));

        java.util.Map<String, String> res = paymentService.processVNPayIpn(params);
        assertThat(res.get("RspCode")).isEqualTo("02");
        verify(paymentTransactionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("processVNPayIpn: Thành công mã 00 -> Cập nhật SUCCESS, gửi RabbitMQ và trả RspCode 00")
    void testProcessVNPayIpnSuccess() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .id(1L)
                .txnRef("TXN_004")
                .orderId("5001")
                .userId(10L)
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentStatus.PENDING)
                .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("vnp_TxnRef", "TXN_004");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "14567890");
        params.put("vnp_BankCode", "NCB");

        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String url = com.quickshop.payment.util.VNPayUtil.buildPaymentUrl(baseUrl, params, "TESTSECRETKEYVNPAY2026");
        String secureHash = url.substring(url.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        params.put("vnp_SecureHash", secureHash);

        when(paymentTransactionRepository.findByTxnRef("TXN_004")).thenReturn(Optional.of(txn));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(i -> i.getArgument(0));

        java.util.Map<String, String> res = paymentService.processVNPayIpn(params);
        assertThat(res.get("RspCode")).isEqualTo("00");
        assertThat(txn.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(txn.getVnpTransactionNo()).isEqualTo("14567890");

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(com.quickshop.payment.config.RabbitMQPaymentConfig.EXCHANGE_ORDER),
                eq(com.quickshop.payment.config.RabbitMQPaymentConfig.ROUTING_KEY_PAYMENT_COMPLETED),
                any(com.quickshop.common.event.PaymentCompletedEvent.class)
        );

        verify(ledgerService, times(1)).recordDoubleEntry(
                eq("VNPAY-PAY-TXN_004"),
                eq("PAYMENT_VNPAY_SUCCESS"),
                eq("VNPAY_GATEWAY"),
                eq("ESCROW_HOLDING"),
                eq(BigDecimal.valueOf(100000))
        );
    }
}
