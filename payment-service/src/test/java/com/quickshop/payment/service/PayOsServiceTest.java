package com.quickshop.payment.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.payment.dto.CreatePayOsPaymentRequest;
import com.quickshop.payment.dto.CreatePayOsPaymentResponse;
import com.quickshop.payment.entity.PaymentStatus;
import com.quickshop.payment.entity.PaymentTransaction;
import com.quickshop.payment.repository.PaymentTransactionRepository;
import com.quickshop.payment.service.impl.PayOsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import vn.payos.service.blocking.webhooks.WebhooksService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayOsServiceTest {

    @Mock
    private PayOS payOS;

    @Mock
    private PaymentRequestsService paymentRequestsService;

    @Mock
    private WebhooksService webhooksService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PayOsServiceImpl payOsService;

    @BeforeEach
    void setUp() {
        lenient().when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        lenient().when(payOS.webhooks()).thenReturn(webhooksService);
    }

    @Test
    @DisplayName("PayOS Service: Tạo yêu cầu thanh toán chuyển khoản thành công")
    void testCreatePaymentLink_Success() {
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-12345"))
                .thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePayOsPaymentRequest req = CreatePayOsPaymentRequest.builder()
                .orderId("QS-12345")
                .amount(BigDecimal.valueOf(150000))
                .userId(1001L)
                .build();

        CreatePayOsPaymentResponse resp = payOsService.createPaymentLink(req);

        assertNotNull(resp);
        assertEquals("QS-12345", resp.getOrderId());
        assertEquals(BigDecimal.valueOf(150000), resp.getAmount());
        assertEquals("0346746521", resp.getAccountNumber());
        assertEquals("NGUYEN VAN KIEN", resp.getAccountName());
        assertNotNull(resp.getQrCode());
        verify(paymentTransactionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("PayOS Service: Tạo yêu cầu thanh toán chỉ cần nhận orderId")
    void testCreatePayment_WithOnlyOrderId() {
        PaymentTransaction existingTxn = PaymentTransaction.builder()
                .orderId("1002")
                .amount(BigDecimal.valueOf(180000))
                .userId(2002L)
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("1002"))
                .thenReturn(Optional.of(existingTxn));
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePayOsPaymentResponse resp = payOsService.createPayment("1002");

        assertNotNull(resp);
        assertEquals("1002", resp.getOrderId());
        assertEquals(BigDecimal.valueOf(180000), resp.getAmount());
        assertEquals("0346746521", resp.getAccountNumber());
        assertEquals("NGUYEN VAN KIEN", resp.getAccountName());
        assertNotNull(resp.getQrCode());
    }

    @Test
    @DisplayName("PayOS Service: Ném lỗi khi tạo yêu cầu thanh toán với số tiền không hợp lệ")
    void testCreatePaymentLink_InvalidAmount() {
        CreatePayOsPaymentRequest req = CreatePayOsPaymentRequest.builder()
                .orderId("QS-12345")
                .amount(BigDecimal.valueOf(500))
                .userId(1001L)
                .build();

        assertThrows(BadRequestException.class, () -> payOsService.createPaymentLink(req));
    }

    @Test
    @DisplayName("PayOS Service: Kiểm tra trạng thái đơn hàng khi đã SUCCESS trong DB")
    void testCheckPaymentStatus_AlreadySuccess() {
        PaymentTransaction txn = PaymentTransaction.builder()
                .orderId("QS-999")
                .txnRef("999")
                .amount(BigDecimal.valueOf(200000))
                .status(PaymentStatus.SUCCESS)
                .bankCode("MB")
                .vnpTransactionNo("MB_FAST_999")
                .build();

        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-999"))
                .thenReturn(Optional.of(txn));

        Map<String, Object> result = payOsService.checkPaymentStatus("QS-999");
        assertTrue((Boolean) result.get("paid"));
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("MB", result.get("bankCode"));
        assertEquals("MB_FAST_999", result.get("transactionNo"));
    }

    @Test
    @DisplayName("PayOS Service: Kiểm tra trạng thái đơn PENDING và PayOS trả về PAID")
    void testCheckPaymentStatus_PendingAndPayOsConfirmsPaid() throws Exception {
        PaymentTransaction txn = PaymentTransaction.builder()
                .orderId("QS-1001")
                .txnRef("1001")
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-1001"))
                .thenReturn(Optional.of(txn));

        PaymentLink mockLink = mock(PaymentLink.class);
        when(mockLink.getStatus()).thenReturn(PaymentLinkStatus.PAID);
        when(mockLink.getId()).thenReturn("PAYLINK_1001");
        when(paymentRequestsService.get(1001L)).thenReturn(mockLink);

        Map<String, Object> result = payOsService.checkPaymentStatus("QS-1001");
        assertTrue((Boolean) result.get("paid"));
        assertEquals("SUCCESS", result.get("status"));
        verify(paymentTransactionRepository, times(1)).save(txn);
        verify(ledgerService, times(1)).recordDoubleEntry(
                anyString(), eq("PAYMENT_PAYOS_MBBANK_SUCCESS"), eq("MBBANK_PAYOS_GATEWAY"), eq("ESCROW_HOLDING"), eq(BigDecimal.valueOf(100000)));
    }

    @Test
    @DisplayName("PayOS Service: Xử lý Webhook thành công và Idempotent chống lặp lại")
    void testProcessWebhook_SuccessAndIdempotent() {
        when(paymentTransactionRepository.findByTxnRef("777")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("777")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> webhookData = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", 777L);
        data.put("amount", 300000L);
        data.put("description", "QS DH777");
        data.put("reference", "MB_REF_777");
        data.put("code", "00");
        webhookData.put("data", data);

        // Lần 1: Thành công
        Map<String, Object> res1 = payOsService.processWebhook(webhookData);
        assertEquals(0, res1.get("error"));
        verify(ledgerService, times(1)).recordDoubleEntry(
                anyString(), eq("PAYMENT_PAYOS_MBBANK_SUCCESS"), eq("MBBANK_PAYOS_GATEWAY"), eq("ESCROW_HOLDING"), any());

        // Lần 2: Idempotent
        PaymentTransaction existing = PaymentTransaction.builder()
                .orderId("777")
                .txnRef("777")
                .amount(BigDecimal.valueOf(300000))
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentTransactionRepository.findByTxnRef("777")).thenReturn(Optional.of(existing));

        Map<String, Object> res2 = payOsService.processWebhook(webhookData);
        assertEquals(0, res2.get("error"));
        assertTrue(String.valueOf(res2.get("message")).contains("Idempotent"));
        verify(ledgerService, times(1)).recordDoubleEntry(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("PayOS Service: Hủy liên kết thanh toán thành công")
    void testCancelPaymentLink_Success() throws Exception {
        PaymentTransaction txn = PaymentTransaction.builder()
                .orderId("QS-555")
                .txnRef("555")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc("QS-555"))
                .thenReturn(Optional.of(txn));

        PaymentLink mockLink = mock(PaymentLink.class);
        when(mockLink.getStatus()).thenReturn(PaymentLinkStatus.CANCELLED);
        when(paymentRequestsService.cancel(eq(555L), anyString())).thenReturn(mockLink);

        Map<String, Object> res = payOsService.cancelPaymentLink("QS-555", "Người dùng đổi ý");
        assertEquals("QS-555", res.get("orderId"));
        assertEquals("CANCELLED", res.get("transactionStatus"));
        assertEquals(PaymentStatus.CANCELLED, txn.getStatus());
        verify(paymentTransactionRepository, times(1)).save(txn);
    }
}
