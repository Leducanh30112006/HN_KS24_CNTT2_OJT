package com.quickshop.notification.ai.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Kiểm soát quyền truy cập dữ liệu và làm sạch tham số Catalog Search
 * Ngăn ngừa rò rỉ dữ liệu Seller nhạy cảm và bảo vệ chống Prompt / SQL Injection.
 */
@Slf4j
@Component
public class AiCatalogAccessValidator {

    private static final int MAX_KEYWORD_LENGTH = 80;
    private static final int DEFAULT_MAX_ITEMS = 5;

    // Pattern loại bỏ toàn bộ khối script nguy hiểm (bao gồm cả nội dung bên trong)
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    // Pattern làm sạch thẻ HTML còn lại
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    // Pattern ký tự nghi vấn SQL / Command injection
    private static final Pattern SUSPICIOUS_SQL_PATTERN = Pattern.compile("(?i)(--|;|/\\*|\\*/|union\\s+select|drop\\s+table|exec\\s+|sleep\\()");

    /**
     * Làm sạch và chuẩn hóa từ khóa tìm kiếm trước khi gọi Catalog API
     */
    public String sanitizeSearchKeyword(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return "";
        }

        String cleaned = rawKeyword.trim();

        // 1. Loại bỏ các khối <script>...</script> độc hại
        cleaned = SCRIPT_TAG_PATTERN.matcher(cleaned).replaceAll("");

        // 2. Loại bỏ các thẻ HTML còn lại
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");

        // 2. Loại bỏ các ký tự điều khiển và dấu ngoặc nhạy cảm
        cleaned = cleaned.replaceAll("[\\r\\n\\t\\{\\}\\[\\]\\\"\\']", " ");

        // 3. Loại bỏ mẫu SQL Injection nếu có
        cleaned = SUSPICIOUS_SQL_PATTERN.matcher(cleaned).replaceAll("");

        // 4. Chuẩn hóa khoảng trắng dư thừa
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        // 5. Cắt ngắn nếu vượt quá độ dài cho phép
        if (cleaned.length() > MAX_KEYWORD_LENGTH) {
            cleaned = cleaned.substring(0, MAX_KEYWORD_LENGTH).trim();
        }

        return cleaned;
    }

    public String sanitizeKeyword(String rawKeyword) {
        return sanitizeSearchKeyword(rawKeyword);
    }

    public boolean isProductVisibleForBuyer(AiCatalogItemDto item) {
        if (item == null) return false;
        String status = item.getStatus();
        return "ACTIVE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
    }

    public List<AiCatalogItemDto> filterVisibleProductsForBuyer(List<AiCatalogItemDto> rawItems) {
        return filterAccessibleProducts(rawItems, "ROLE_BUYER", 10);
    }

    /**
     * Kiểm tra và lọc danh sách sản phẩm theo quyền truy cập thương mại
     * Đảm bảo chỉ những sản phẩm ACTIVE/APPROVED mới được hiển thị cho khách hàng & đưa vào Prompt AI.
     */
    public List<AiCatalogItemDto> filterAccessibleProducts(List<AiCatalogItemDto> rawItems, String userRole, int maxLimit) {
        if (rawItems == null || rawItems.isEmpty()) {
            return List.of();
        }

        int limit = (maxLimit > 0 && maxLimit <= 10) ? maxLimit : DEFAULT_MAX_ITEMS;
        List<AiCatalogItemDto> safeItems = new ArrayList<>();

        for (AiCatalogItemDto item : rawItems) {
            if (item == null) continue;

            // KIỂM TRA QUYỀN TRUY CẬP VÀ TRẠNG THÁI SẢN PHẨM:
            // Chỉ hiển thị sản phẩm ACTIVE hoặc APPROVED
            String status = item.getStatus();
            boolean isPublicStatus = "ACTIVE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);

            // Nếu người dùng là ADMIN thì có thể xem thêm, ngược lại người mua / khách vãng lai bắt buộc chỉ thấy sản phẩm công khai
            boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(userRole);
            if (!isPublicStatus && !isAdmin) {
                log.debug("[ACCESS_CONTROL] Bỏ qua sản phẩm ID={} vì ở trạng thái không công khai: {}", item.getId(), status);
                continue;
            }

            // Xóa bỏ các trường dữ liệu nội bộ không dành cho Buyer
            item.setShopName(sanitizeShopName(item.getShopName()));

            safeItems.add(item);
            if (safeItems.size() >= limit) {
                break;
            }
        }

        return safeItems;
    }

    private String sanitizeShopName(String rawShopName) {
        if (rawShopName == null || rawShopName.isBlank()) {
            return "Gian hàng chính hãng QuickShop";
        }
        return rawShopName.trim();
    }
}
