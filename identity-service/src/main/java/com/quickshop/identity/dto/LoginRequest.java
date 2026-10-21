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
public class LoginRequest {
    @NotBlank(message = "Username hoặc Email không được để trống")
    private String usernameOrEmail;

    @NotBlank(message = "Password không được để trống")
    private String password;
}
