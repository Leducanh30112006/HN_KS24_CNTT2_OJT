package com.quickshop.notification.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Phản hồi nội dung SEO sản phẩm đã qua kiểm tra đầu ra và lọc HTML/Markdown an toàn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả sinh nội dung SEO sản phẩm đã được kiểm định an toàn")
public class AiSeoGenerateResponse {

    @Schema(description = "Tiêu đề sản phẩm tối ưu chuẩn SEO (plain text an toàn)")
    private String optimizedTitle;

    @Schema(description = "Bài mô tả sản phẩm chuẩn SEO (đã lọc HTML độc hại và chuẩn hóa Markdown)")
    private String fullDescription;

    @Schema(description = "Thẻ Meta Title chuẩn SEO (tối đa 70 ký tự)")
    private String metaTitle;

    @Schema(description = "Thẻ Meta Description chuẩn SEO (tối đa 160 ký tự)")
    private String metaDescription;

    @Schema(description = "Đường dẫn slug thân thiện SEO")
    private String slug;

    @Builder.Default
    @Schema(description = "Danh sách từ khóa SEO đã trích xuất/tối ưu")
    private List<String> seoKeywords = new ArrayList<>();

    @Schema(description = "Định dạng đầu ra thực tế (MARKDOWN hoặc PLAIN_TEXT)")
    private String outputFormat;

    @Schema(description = "Đánh dấu nội dung đã được lọc/làm sạch HTML hoặc Markdown không an toàn")
    private boolean sanitized;

    @Schema(description = "Đánh dấu prompt đầu vào có bị cắt ngắn theo giới hạn hệ thống hay không")
    private boolean promptTruncated;

    @Schema(description = "Số ký tự prompt thực tế đã gửi tới AI")
    private int promptLengthUsed;

    @Schema(description = "Ngưỡng giới hạn prompt được áp dụng")
    private int maxPromptLengthLimit;

    @Schema(description = "Kết quả kiểm tra chất lượng và an toàn đầu ra")
    private boolean outputValid;

    @Builder.Default
    @Schema(description = "Các cảnh báo bảo mật / chuẩn hóa phát sinh trong quá trình xử lý")
    private List<String> safetyWarnings = new ArrayList<>();

    @Schema(description = "Có sử dụng bộ sinh SEO dự phòng an toàn hay không")
    private boolean fallbackUsed;

    @Schema(description = "Mô hình AI hoặc engine được sử dụng")
    private String modelUsed;

    @Schema(description = "Thời gian xử lý tính bằng mili-giây")
    private long latencyMs;

    @Schema(description = "Thời điểm phản hồi")
    private String timestamp;
}
