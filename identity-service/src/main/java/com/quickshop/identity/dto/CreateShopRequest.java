package com.quickshop.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Yêu cầu đăng ký mở gian hàng (Onboarding KYC)")
public class CreateShopRequest {

    @NotBlank(message = "Tên gian hàng không được để trống")
    @Size(min = 3, max = 100, message = "Tên gian hàng phải từ 3 đến 100 ký tự")
    @Schema(description = "Tên gian hàng", example = "TechWorld Official Store")
    private String shopName;

    @NotBlank(message = "Địa chỉ kho lấy/trả hàng không được để trống")
    @Schema(description = "Địa chỉ kho hàng", example = "Số 123 Cầu Giấy, Dịch Vọng, Cầu Giấy, Hà Nội")
    private String warehouseAddress;

    @NotBlank(message = "Số điện thoại liên hệ của gian hàng không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại hotline gian hàng", example = "0988776655")
    private String phoneNumber;

    @NotBlank(message = "Mã số thuế hoặc CCCD người đại diện không được để trống")
    @Schema(description = "Mã số thuế doanh nghiệp hoặc hộ kinh doanh", example = "0108998877")
    private String taxCode;

    @Schema(description = "Số CCCD/Hộ chiếu người đại diện pháp luật", example = "001200001234")
    private String citizenId;

    @Builder.Default
    @Schema(description = "Loại hình kinh doanh: INDIVIDUAL (Cá nhân), BUSINESS (Hộ kinh doanh), ENTERPRISE (Doanh nghiệp)", example = "BUSINESS")
    private String businessType = "INDIVIDUAL";

    @Schema(description = "Mô tả ngắn giới thiệu gian hàng", example = "Chuyên phân phối phụ kiện công nghệ chính hãng")
    private String description;

    @Schema(description = "Đường dẫn ảnh logo gian hàng", example = "https://images.unsplash.com/photo-tech-logo.jpg")
    private String logoUrl;

    @NotBlank(message = "Mã ngân hàng không được để trống")
    @Schema(description = "Mã ngân hàng (Vietcombank, Techcombank, MB...)", example = "VCB")
    private String bankCode;

    @NotBlank(message = "Số tài khoản ngân hàng không được để trống")
    @Schema(description = "Số tài khoản ngân hàng nhận tiền quyết toán", example = "0011004567890")
    private String bankAccountNumber;

    @NotBlank(message = "Tên chủ tài khoản ngân hàng không được để trống")
    @Schema(description = "Tên chủ tài khoản ngân hàng (viết hoa không dấu)", example = "NGUYEN VAN A")
    private String bankAccountName;
}
