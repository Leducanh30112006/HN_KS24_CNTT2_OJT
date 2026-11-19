package com.quickshop.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID Token hoặc Token xác thực không được để trống")
    private String idToken;

    /**
     * Hành động yêu cầu: 'LOGIN', 'REGISTER', 'FORGOT_PASSWORD'
     */
    @Builder.Default
    private String action = "LOGIN";

    /**
     * Mật khẩu mới (nếu action == 'FORGOT_PASSWORD' và người dùng muốn cập nhật mật khẩu mới trực tiếp)
     */
    private String newPassword;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
