package com.quickshop.notification.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu trò chuyện cùng QuickShop AI Assistant")
public class AiChatRequest {

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 500, message = "Độ dài câu hỏi không được vượt quá 500 ký tự")
    @Schema(description = "Tin nhắn của khách hàng", example = "Tư vấn cho mình điện thoại chụp ảnh đẹp tầm 30 triệu")
    private String message;

    @Schema(description = "ID phiên hội thoại", example = "conv_123456")
    private String conversationId;

    @Schema(description = "ID người dùng (nếu đã đăng nhập)", example = "1")
    private Long userId;

    @Schema(description = "Vai trò người dùng (ROLE_BUYER, ROLE_SELLER, ROLE_ADMIN)", example = "ROLE_BUYER")
    private String userRole;
}

