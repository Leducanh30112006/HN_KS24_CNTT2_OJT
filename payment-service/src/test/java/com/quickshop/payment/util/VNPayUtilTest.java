package com.quickshop.payment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VNPayUtilTest {

    private static final String SECRET_KEY = "TEST_SECRET_KEY_VNPAY_123456";
    private static final String BASE_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    @Test
    @DisplayName("1. Băm HMAC-SHA512 cho chuỗi đầu vào hợp lệ sinh mã Hex 128 ký tự")
    void testHmacSHA512_ValidInput() {
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=QUICKSHOP";
        String hash = VNPayUtil.hmacSHA512(SECRET_KEY, data);

        assertNotNull(hash);
        assertEquals(128, hash.length()); // SHA-512 = 512 bits = 64 bytes = 128 hex chars
        assertEquals(hash.toLowerCase(), hash); // Phải là chữ thường
    }

    @Test
    @DisplayName("2. Tạo URL thanh toán VNPay sắp xếp alphabet các tham số và sinh chữ ký vnp_SecureHash")
    void testBuildPaymentUrl_CorrectParamsAndSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "QUICKSHOP");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "ORDER_123");
        params.put("vnp_OrderInfo", "Thanh toan don hang #123");

        String paymentUrl = VNPayUtil.buildPaymentUrl(BASE_URL, params, SECRET_KEY);

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith(BASE_URL + "?"));
        assertTrue(paymentUrl.contains("vnp_SecureHash="));
        assertTrue(paymentUrl.contains("vnp_Amount=10000000"));
        assertTrue(paymentUrl.contains("vnp_TmnCode=QUICKSHOP"));

        // Kiểm tra thứ tự Alphabet trên query string: vnp_Amount phải đứng trước vnp_Command và vnp_OrderInfo
        int idxAmount = paymentUrl.indexOf("vnp_Amount=");
        int idxCommand = paymentUrl.indexOf("vnp_Command=");
        int idxOrderInfo = paymentUrl.indexOf("vnp_OrderInfo=");
        int idxTmnCode = paymentUrl.indexOf("vnp_TmnCode=");

        assertTrue(idxAmount < idxCommand, "vnp_Amount phải đứng trước vnp_Command");
        assertTrue(idxCommand < idxOrderInfo, "vnp_Command phải đứng trước vnp_OrderInfo");
        assertTrue(idxOrderInfo < idxTmnCode, "vnp_OrderInfo phải đứng trước vnp_TmnCode");
    }

    @Test
    @DisplayName("3. Xác thực chữ ký HỢP LỆ: Khớp với chữ ký sinh ra từ tập tham số")
    void testVerifySignature_ValidSignature_ReturnsTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan don hang #123");
        params.put("vnp_PayDate", "20261027120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "QUICKSHOP");
        params.put("vnp_TransactionNo", "14000123");
        params.put("vnp_TxnRef", "ORDER_123");

        // Giả lập tạo URL kèm chữ ký
        String paymentUrl = VNPayUtil.buildPaymentUrl(BASE_URL, params, SECRET_KEY);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());

        // Đưa chữ ký vào Map phản hồi
        params.put("vnp_SecureHash", secureHash);

        boolean isValid = VNPayUtil.verifySignature(params, SECRET_KEY);
        assertTrue(isValid, "Chữ ký phải được xác minh thành công");
    }

    @Test
    @DisplayName("4. Xác thực chữ ký SAI (Invalid Checksum): Dữ liệu bị thay đổi trên đường truyền")
    void testVerifySignature_TamperedData_ReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "ORDER_123");

        String paymentUrl = VNPayUtil.buildPaymentUrl(BASE_URL, params, SECRET_KEY);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());

        // Giả lập hacker sửa đổi số tiền nhưng giữ nguyên chữ ký cũ
        params.put("vnp_Amount", "10000"); // Sửa từ 100,000 xuống 100 VND
        params.put("vnp_SecureHash", secureHash);

        boolean isValid = VNPayUtil.verifySignature(params, SECRET_KEY);
        assertFalse(isValid, "Chữ ký phải không hợp lệ khi dữ liệu bị thay đổi");
    }

    @Test
    @DisplayName("5. Xác thực chữ ký SAI (Invalid Checksum): Dùng sai secret key")
    void testVerifySignature_WrongSecret_ReturnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_TxnRef", "ORDER_123");

        String paymentUrl = VNPayUtil.buildPaymentUrl(BASE_URL, params, SECRET_KEY);
        String secureHash = paymentUrl.substring(paymentUrl.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        params.put("vnp_SecureHash", secureHash);

        boolean isValid = VNPayUtil.verifySignature(params, "WRONG_SECRET_KEY");
        assertFalse(isValid, "Chữ ký phải không hợp lệ khi dùng sai secret key");
    }

    @Test
    @DisplayName("6. Lấy IP client ưu tiên CF-Connecting-IP và X-Forwarded-For")
    void testGetIpAddress_Headers() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.113.152.1");
        request.setRemoteAddr("10.0.0.1");

        assertEquals("203.113.152.1", VNPayUtil.getIpAddress(request));

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        request2.addHeader("X-Forwarded-For", "118.70.1.2, 10.0.0.2");
        request2.setRemoteAddr("10.0.0.1");

        assertEquals("118.70.1.2", VNPayUtil.getIpAddress(request2));
    }
}
