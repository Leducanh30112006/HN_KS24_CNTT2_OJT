package com.quickshop.notification.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Yêu cầu sinh mô tả sản phẩm chuẩn SEO từ thông số kỹ thuật sản phẩm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu sinh tiêu đề và mô tả sản phẩm chuẩn SEO bằng AI")
public class AiSeoGenerateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm không được vượt quá 200 ký tự")
    @Schema(description = "Tên sản phẩm cơ bản", example = "Laptop QuickBook Ultra 14 inch M3 Pro")
    private String productName;

    @Size(max = 120, message = "Danh mục không được vượt quá 120 ký tự")
    @Schema(description = "Ngành hàng / danh mục sản phẩm", example = "Laptop & Máy tính")
    private String category;

    @Size(max = 100, message = "Thương hiệu không được vượt quá 100 ký tự")
    @Schema(description = "Thương hiệu sản phẩm", example = "QuickBook")
    private String brand;

    @Schema(description = "Giá bán tham khảo", example = "45990000")
    private String price;

    @Builder.Default
    @Schema(description = "Bảng thông số kỹ thuật dạng key-value", example = "{\"CPU\":\"M3 Pro 11-core\",\"RAM\":\"18GB\",\"SSD\":\"512GB\"}")
    private Map<String, String> specifications = new LinkedHashMap<>();

    @Size(max = 1000, message = "Chuỗi thông số bổ sung không được vượt quá 1000 ký tự")
    @Schema(description = "Thông số kỹ thuật dạng văn bản tự do", example = "Màn hình Liquid Retina XDR 120Hz, pin 18 tiếng")
    private String specificationsText;

    @Size(max = 300, message = "Từ khóa SEO không được vượt quá 300 ký tự")
    @Schema(description = "Danh sách từ khóa SEO mục tiêu phân cách bằng dấu phẩy", example = "laptop lập trình, màn hình retina, chính hãng")
    private String keywords;

    @Size(max = 50, message = "Phong cách viết không được vượt quá 50 ký tự")
    @Schema(description = "Giọng văn SEO: CHUYEN_NGHIEP, TRE_TRUNG, SANG_TRONG", example = "CHUYEN_NGHIEP")
    private String tone;

    @Size(max = 20, message = "Định dạng đầu ra không hợp lệ")
    @Schema(description = "Định dạng nội dung đầu ra: MARKDOWN hoặc PLAIN_TEXT", example = "MARKDOWN")
    private String outputFormat;

    @Schema(description = "Giới hạn độ dài prompt đầu vào (tối đa hệ thống 800 ký tự)", example = "800")
    private Integer maxPromptLength;

    @Schema(description = "Giới hạn độ dài mô tả SEO đầu ra (300 - 2500 ký tự)", example = "1800")
    private Integer maxOutputLength;
}
