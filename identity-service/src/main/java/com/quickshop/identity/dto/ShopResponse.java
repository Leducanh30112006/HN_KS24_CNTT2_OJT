package com.quickshop.identity.dto;

import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết hồ sơ gian hàng")
public class ShopResponse {

    @Schema(description = "ID gian hàng", example = "1")
    private Long id;

    @Schema(description = "ID chủ sở hữu tài khoản (User ID)", example = "10")
    private Long ownerId;

    @Schema(description = "Tên gian hàng", example = "TechWorld Official Store")
    private String shopName;

    @Schema(description = "Mã số thuế", example = "0108998877")
    private String taxCode;

    @Schema(description = "Số CCCD người đại diện", example = "001200001234")
    private String citizenId;

    @Schema(description = "Loại hình kinh doanh", example = "BUSINESS")
    private String businessType;

    @Schema(description = "Số điện thoại hotline", example = "0988776655")
    private String phoneNumber;

    @Schema(description = "Mô tả gian hàng", example = "Chuyên phân phối phụ kiện công nghệ chính hãng")
    private String description;

    @Schema(description = "Đường dẫn ảnh logo", example = "https://images.unsplash.com/photo-tech-logo.jpg")
    private String logoUrl;

    @Schema(description = "Mã ngân hàng", example = "VCB")
    private String bankCode;

    @Schema(description = "Số tài khoản ngân hàng", example = "0011004567890")
    private String bankAccountNumber;

    @Schema(description = "Tên chủ tài khoản ngân hàng", example = "NGUYEN VAN A")
    private String bankAccountName;

    @Schema(description = "Trạng thái gian hàng: PENDING_APPROVAL, ACTIVE, SUSPENDED, REJECTED", example = "PENDING_APPROVAL")
    private ShopStatus status;

    @Schema(description = "Địa chỉ kho hàng", example = "Số 123 Cầu Giấy, Dịch Vọng, Cầu Giấy, Hà Nội")
    private String warehouseAddress;

    @Schema(description = "Thời gian tạo hồ sơ")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật hồ sơ gần nhất")
    private LocalDateTime updatedAt;

    public static ShopResponse fromEntity(Shop shop) {
        if (shop == null) return null;
        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(shop.getOwnerId())
                .shopName(shop.getShopName())
                .taxCode(shop.getTaxCode())
                .citizenId(shop.getCitizenId())
                .businessType(shop.getBusinessType())
                .phoneNumber(shop.getPhoneNumber())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bankCode(shop.getBankCode())
                .bankAccountNumber(shop.getBankAccountNumber())
                .bankAccountName(shop.getBankAccountName())
                .status(shop.getStatus())
                .warehouseAddress(shop.getWarehouseAddress())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
