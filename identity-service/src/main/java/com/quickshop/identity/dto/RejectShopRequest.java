package com.quickshop.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu từ chối hồ sơ đăng ký mở gian hàng từ phía Admin")
public class RejectShopRequest {

    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    @Schema(description = "Lý do từ chối hồ sơ Shop", example = "Thông tin mã số thuế hoặc CCCD chưa chính xác")
    private String reason;
}
