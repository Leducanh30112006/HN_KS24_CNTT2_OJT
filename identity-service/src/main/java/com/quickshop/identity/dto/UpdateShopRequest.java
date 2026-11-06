package com.quickshop.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Yêu cầu cập nhật hồ sơ gian hàng")
public class UpdateShopRequest {

    @Size(min = 3, max = 100, message = "Tên gian hàng phải từ 3 đến 100 ký tự")
    @Schema(description = "Tên gian hàng cập nhật", example = "TechWorld Premium Store")
    private String shopName;

    @Schema(description = "Địa chỉ kho hàng", example = "Số 456 Trần Duy Hưng, Cầu Giấy, Hà Nội")
    private String warehouseAddress;

    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    @Schema(description = "Số điện thoại hotline", example = "0911223344")
    private String phoneNumber;

    @Schema(description = "Mã số thuế", example = "0109998888")
    private String taxCode;

    @Schema(description = "Số CCCD/Hộ chiếu người đại diện", example = "001200005678")
    private String citizenId;

    @Schema(description = "Loại hình kinh doanh", example = "BUSINESS")
    private String businessType;

    @Schema(description = "Mô tả gian hàng", example = "Chuyên phân phối các sản phẩm Apple, phụ kiện chính hãng")
    private String description;

    @Schema(description = "Đường dẫn ảnh logo", example = "https://images.unsplash.com/new-logo.jpg")
    private String logoUrl;

    @Schema(description = "Mã ngân hàng", example = "TCB")
    private String bankCode;

    @Schema(description = "Số tài khoản ngân hàng", example = "19033445566778")
    private String bankAccountNumber;

    @Schema(description = "Tên chủ tài khoản", example = "NGUYEN VAN A")
    private String bankAccountName;
}
