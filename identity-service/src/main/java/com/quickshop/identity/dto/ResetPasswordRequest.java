package com.quickshop.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đặt lại mật khẩu mới với mã OTP")
public class ResetPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Schema(description = "Email tài khoản", example = "buyer@quickshop.vn")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm chính xác 6 chữ số")
    @Schema(description = "Mã OTP 6 chữ số nhận qua email", example = "123456")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự")
    @Schema(description = "Mật khẩu mới", example = "NewSecret123")
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    @Schema(description = "Nhập lại mật khẩu mới", example = "NewSecret123")
    private String confirmPassword;
}
