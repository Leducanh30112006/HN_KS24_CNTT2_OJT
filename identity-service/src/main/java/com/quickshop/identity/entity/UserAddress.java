package com.quickshop.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_user_addresses_default", columnList = "user_id, is_default")
})
@EntityListeners(AuditingEntityListener.class)
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "tag", length = 50)
    @Builder.Default
    private String tag = "Nhà riêng";

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    /**
     * Tọa độ Vĩ độ (Latitude) chọn trực tiếp từ bản đồ Leaflet OpenStreetMap.
     * Cho phép null để tương thích ngược dữ liệu cũ.
     */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    /**
     * Tọa độ Kinh độ (Longitude) chọn trực tiếp từ bản đồ Leaflet OpenStreetMap.
     * Cho phép null để tương thích ngược dữ liệu cũ.
     */
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void generateFullAddressIfEmpty() {
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (streetAddress != null && !streetAddress.trim().isEmpty()) {
                sb.append(streetAddress.trim());
            }
            if (ward != null && !ward.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ward.trim());
            }
            if (district != null && !district.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(district.trim());
            }
            if (city != null && !city.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(city.trim());
            }
            this.fullAddress = sb.toString();
        }
    }
}
