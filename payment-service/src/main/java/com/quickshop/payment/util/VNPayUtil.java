package com.quickshop.payment.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public final class VNPayUtil {

    private VNPayUtil() {
        // Utility class
    }

    /**
     * Ký băm dữ liệu theo thuật toán HMAC-SHA512 chuẩn VNPay 2.1.0
     *
     * @param key  Khóa bí mật (Hash Secret)
     * @param data Chuỗi dữ liệu cần băm (hashData)
     * @return Chuỗi Hex 128 ký tự chữ thường
     */
    public static String hmacSHA512(String key, String data) {
        if (key == null || data == null) {
            throw new IllegalArgumentException("Khóa bí mật hoặc dữ liệu băm không được null");
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Lỗi tạo mã băm HMAC-SHA512: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA512", e);
        }
    }

    /**
     * Xây dựng Payment URL hoàn chỉnh với các tham số được sắp xếp theo bảng chữ cái và ký checksum
     *
     * @param baseUrl    URL thanh toán VNPay Sandbox / Production
     * @param vnpParams  Map chứa các tham số VNPay (chưa bao gồm vnp_SecureHash)
     * @param hashSecret Khóa bí mật
     * @return URL thanh toán hoàn chỉnh kèm chuỗi chữ ký vnp_SecureHash
     */
    public static String buildPaymentUrl(String baseUrl, Map<String, String> vnpParams, String hashSecret) {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                // Build hash data
                hashData.append(fieldName).append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                hashData.append('&');
                query.append('&');
            }
        }

        // Xóa dấu '&' cuối cùng nếu có
        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }
        if (query.length() > 0) {
            query.setLength(query.length() - 1);
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + query.toString() + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác minh chữ ký số checksum trả về từ VNPay (IPN hoặc Return URL)
     *
     * @param vnpParams  Map tham số nhận về từ VNPay
     * @param hashSecret Khóa bí mật
     * @return true nếu chữ ký hợp lệ, false nếu bị giả mạo hoặc sai lệch
     */
    public static boolean verifyChecksum(Map<String, String> vnpParams, String hashSecret) {
        if (vnpParams == null || hashSecret == null) {
            return false;
        }

        String receivedSecureHash = vnpParams.get("vnp_SecureHash");
        if (receivedSecureHash == null || receivedSecureHash.isBlank()) {
            return false;
        }

        // Sao chép map và loại bỏ vnp_SecureHash / vnp_SecureHashType trước khi băm lại
        Map<String, String> fields = new HashMap<>(vnpParams);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                hashData.append(fieldName).append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                hashData.append('&');
            }
        }

        if (hashData.length() > 0) {
            hashData.setLength(hashData.length() - 1);
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(receivedSecureHash);
    }

    /**
     * Lấy địa chỉ IP thật của khách hàng, xử lý khi đứng sau Reverse Proxy / Cloudflare / Load Balancer
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP dạng: client, proxy1, proxy2
                int index = ip.indexOf(',');
                if (index != -1) {
                    return ip.substring(0, index).trim();
                }
                return ip.trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || "0:0:0:0:0:0:0:1".equals(remoteAddr) || remoteAddr.isBlank()) {
            return "127.0.0.1";
        }
        return remoteAddr;
    }
}
