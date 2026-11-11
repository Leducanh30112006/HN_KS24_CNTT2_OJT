package com.quickshop.notification.ai.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * DTO dữ liệu sản phẩm an toàn phục vụ AI Shopping Assistant
 * Chỉ chứa các trường thông tin thương mại công khai, bảo vệ quyền riêng tư người bán.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiCatalogItemDto implements Serializable {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String shopName;
    private String description;
    private String mainImage;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer totalStock;
    private Double averageRating;
    private Long reviewCount;
    private String status;

    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    /**
     * Chuyển đổi thông tin sản phẩm thành đoạn văn bản ngữ cảnh (Snippet) cho Prompt AI
     */
    public String toSnippet() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(VI_LOCALE);
        String priceText;
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) != 0) {
            priceText = currencyFormat.format(minPrice) + " - " + currencyFormat.format(maxPrice);
        } else if (minPrice != null) {
            priceText = currencyFormat.format(minPrice);
        } else {
            priceText = "Liên hệ";
        }

        String ratingText = (averageRating != null && averageRating > 0)
                ? String.format("%.1f★ (%d đánh giá)", averageRating, reviewCount != null ? reviewCount : 0)
                : "Chưa có đánh giá";

        String stockText = (totalStock != null && totalStock > 0)
                ? "Còn hàng (" + totalStock + ")"
                : "Tạm hết hàng";

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name != null ? name : "Sản phẩm").append("]");
        if (categoryName != null && !categoryName.isBlank()) {
            sb.append(" [Ngành hàng: ").append(categoryName).append("]");
        }
        sb.append(" - Giá: ").append(priceText);
        sb.append(" | ").append(ratingText);
        sb.append(" | ").append(stockText);
        if (description != null && !description.isBlank()) {
            String shortDesc = description.length() > 120
                    ? description.substring(0, 117) + "..."
                    : description;
            sb.append(" - Đặc điểm: ").append(shortDesc.replace("\n", " "));
        }
        return sb.toString();
    }
}
