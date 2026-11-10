package com.quickshop.payment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VNPayUtilTest {

    private static final String TEST_SECRET = "TESTSECRETKEYVNPAY2026";

    @Test
    @DisplayName("hmacSHA512: Băm chuỗi dữ liệu ra mã hex 128 ký tự chữ thường chính xác")
    void testHmacSHA512Success() {
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=QUICKSHOP";
        String hash1 = VNPayUtil.hmacSHA512(TEST_SECRET, data);
        String hash2 = VNPayUtil.hmacSHA512(TEST_SECRET, data);

        assertThat(hash1).isNotNull();
        assertThat(hash1).hasSize(128);
        assertThat(hash1).isEqualTo(hash2); // Tính bất biến
        assertThat(hash1).matches("^[a-f0-9]{128}$");
    }

    @Test
    @DisplayName("hmacSHA512: Ném ngoại lệ IllegalArgumentException khi khóa hoặc dữ liệu null")
    void testHmacSHA512NullInputs() {
        assertThatThrownBy(() -> VNPayUtil.hmacSHA512(null, "someData"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> VNPayUtil.hmacSHA512(TEST_SECRET, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("buildPaymentUrl: Sắp xếp tham số theo alphabet, encode URL và đính kèm vnp_SecureHash")
    void testBuildPaymentUrl() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "QUICKSHOP");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "ORDER_12345");
        params.put("vnp_OrderInfo", "Thanh toan don hang #12345");

        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String paymentUrl = VNPayUtil.buildPaymentUrl(baseUrl, params, TEST_SECRET);

        assertThat(paymentUrl).startsWith(baseUrl + "?");
        assertThat(paymentUrl).contains("vnp_Amount=10000000");
        assertThat(paymentUrl).contains("vnp_Command=pay");
        assertThat(paymentUrl).contains("vnp_SecureHash=");

        // Kiểm tra thứ tự xuất hiện các tham số theo bảng chữ cái: vnp_Amount trước vnp_Command trước vnp_OrderInfo
        int idxAmount = paymentUrl.indexOf("vnp_Amount=");
        int idxCommand = paymentUrl.indexOf("vnp_Command=");
        int idxOrderInfo = paymentUrl.indexOf("vnp_OrderInfo=");
        int idxTmnCode = paymentUrl.indexOf("vnp_TmnCode=");

        assertThat(idxAmount).isLessThan(idxCommand);
        assertThat(idxCommand).isLessThan(idxOrderInfo);
        assertThat(idxOrderInfo).isLessThan(idxTmnCode);
    }

    @Test
    @DisplayName("verifyChecksum: Xác thực chữ ký số hợp lệ trả về true")
    void testVerifyChecksumValid() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "25000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "ORD_9999");

        // Tạo hash hợp lệ
        String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String url = VNPayUtil.buildPaymentUrl(baseUrl, params, TEST_SECRET);
        String secureHash = url.substring(url.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());

        params.put("vnp_SecureHash", secureHash);
        params.put("vnp_SecureHashType", "SHA512");

        boolean isValid = VNPayUtil.verifyChecksum(params, TEST_SECRET);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("verifyChecksum: Chữ ký bị can thiệp dữ liệu hoặc sai secret trả về false")
    void testVerifyChecksumInvalid() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "25000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "ORD_9999");
        params.put("vnp_SecureHash", "fake_hash_123456789");

        boolean isValid = VNPayUtil.verifyChecksum(params, TEST_SECRET);
        assertThat(isValid).isFalse();

        // Kiểm tra với tham số null
        assertThat(VNPayUtil.verifyChecksum(null, TEST_SECRET)).isFalse();
        assertThat(VNPayUtil.verifyChecksum(params, null)).isFalse();
    }

    @Test
    @DisplayName("getClientIp: Trích xuất IP thật từ header X-Forwarded-For hoặc remote address")
    void testGetClientIp() {
        MockHttpServletRequest requestWithProxy = new MockHttpServletRequest();
        requestWithProxy.addHeader("X-Forwarded-For", "113.190.234.12, 10.0.0.1");

        String ip1 = VNPayUtil.getClientIp(requestWithProxy);
        assertThat(ip1).isEqualTo("113.190.234.12");

        MockHttpServletRequest directRequest = new MockHttpServletRequest();
        directRequest.setRemoteAddr("192.168.1.100");

        String ip2 = VNPayUtil.getClientIp(directRequest);
        assertThat(ip2).isEqualTo("192.168.1.100");

        // IPv6 localhost chuyển về 127.0.0.1
        MockHttpServletRequest localhostRequest = new MockHttpServletRequest();
        localhostRequest.setRemoteAddr("0:0:0:0:0:0:0:1");
        String ip3 = VNPayUtil.getClientIp(localhostRequest);
        assertThat(ip3).isEqualTo("127.0.0.1");

        // Null request
        assertThat(VNPayUtil.getClientIp(null)).isEqualTo("127.0.0.1");
    }
}
