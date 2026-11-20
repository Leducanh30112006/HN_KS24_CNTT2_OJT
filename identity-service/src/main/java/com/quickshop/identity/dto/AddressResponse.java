package com.quickshop.identity.dto;

import com.quickshop.identity.entity.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private Long userId;
    private String recipientName;
    private String phoneNumber;
    private String streetAddress;
    private String ward;
    private String district;
    private String city;
    private String fullAddress;
    private String tag;
    private Boolean isDefault;

    /**
     * Tọa độ Vĩ độ (Latitude) chọn từ bản đồ Leaflet OpenStreetMap.
     */
    private BigDecimal latitude;

    /**
     * Tọa độ Kinh độ (Longitude) chọn từ bản đồ Leaflet OpenStreetMap.
     */
    private BigDecimal longitude;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddressResponse fromEntity(UserAddress entity) {
        if (entity == null) {
            return null;
        }
        return AddressResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .recipientName(entity.getRecipientName())
                .phoneNumber(entity.getPhoneNumber())
                .streetAddress(entity.getStreetAddress())
                .ward(entity.getWard())
                .district(entity.getDistrict())
                .city(entity.getCity())
                .fullAddress(entity.getFullAddress())
                .tag(entity.getTag())
                .isDefault(entity.isDefault())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
