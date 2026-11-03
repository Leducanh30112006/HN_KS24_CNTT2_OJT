package com.quickshop.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phản hồi khi gửi mã OTP")
public class OtpResponse {

    @Schema(description = "Email người nhận", example = "buyer@quickshop.vn")
    private String email;

    @Schema(description = "Thông báo kết quả", example = "Mã OTP đã được gửi thành công")
    private String message;

    @Schema(description = "Thời gian hiệu lực của mã OTP tính bằng giây", example = "300")
    private Long ttlSeconds;

    @Schema(description = "Thời gian chờ tối thiểu để gửi lại OTP tính bằng giây", example = "60")
    private Long cooldownSeconds;
}
