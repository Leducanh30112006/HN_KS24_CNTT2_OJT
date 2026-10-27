package com.quickshop.payment.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class VNPayUtil {

    public static final String HMAC_SHA512 = "HmacSHA512";

    /**
     * Băm chuỗi dữ liệu bằng thuật toán HMAC-SHA512 với khóa bí mật
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new IllegalArgumentException("Key and data cannot be null");
            }
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.error("Lỗi tạo chữ ký HMAC-SHA512: {}", ex.getMessage(), ex);
            return "";
        }
    }

    /**
     * Tạo URL chuyển hướng sang VNPay Sandbox kèm chuỗi băm chữ ký vnp_SecureHash
     */
    public static String buildPaymentUrl(String baseUrl, Map<String, String> vnpParams, String secretKey) {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean first = true;

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                if (!first) {
                    hashData.append('&');
                    query.append('&');
                }
                first = false;

                try {
                    // Dữ liệu tạo chuỗi băm HMAC-SHA512
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Dữ liệu gắn vào URL query string
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    log.error("Lỗi URL encode trường {}: {}", fieldName, e.getMessage());
                }
            }
        }

        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        return baseUrl + "?" + queryUrl;
    }

    /**
     * Xác thực chữ ký vnp_SecureHash từ VNPay gửi về (Return URL hoặc IPN Webhook)
     */
    public static boolean verifySignature(Map<String, String> fields, String secretKey) {
        String vnpSecureHash = fields.get("vnp_SecureHash");
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }

        Map<String, String> validFields = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                if (!"vnp_SecureHash".equalsIgnoreCase(key) && !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                    validFields.put(key, value);
                }
            }
        }

        List<String> fieldNames = new ArrayList<>(validFields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        boolean first = true;

        for (String fieldName : fieldNames) {
            String fieldValue = validFields.get(fieldName);
            if (!first) {
                hashData.append('&');
            }
            first = false;

            try {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            } catch (Exception e) {
                log.error("Lỗi URL encode khi xác thực chữ ký cho {}: {}", fieldName, e.getMessage());
            }
        }

        String calculatedHash = hmacSHA512(secretKey, hashData.toString());
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }

    /**
     * Lấy địa chỉ IP thực của client hỗ trợ Reverse Proxy, Cloudflare, Load Balancer
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }

        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip != null ? ip : "127.0.0.1";
    }
}
