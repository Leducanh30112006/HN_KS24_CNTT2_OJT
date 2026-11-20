package com.quickshop.identity.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(0|\\+84)[35789][0-9]{8}$",
            message = "Số điện thoại phải đúng định dạng 10 số điện thoại Việt Nam (ví dụ: 0912345678 hoặc +84912345678)"
    )
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ chi tiết (số nhà, tên đường) không được để trống")
    @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự")
    private String streetAddress;

    private String ward;

    private String district;

    private String city;

    private String fullAddress;

    @Builder.Default
    private String tag = "Nhà riêng";

    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Tọa độ Vĩ độ (-90 đến 90), chọn trực tiếp từ bản đồ Leaflet.
     */
    @DecimalMin(value = "-90.0", message = "Vĩ độ (latitude) không được nhỏ hơn -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ (latitude) không được lớn hơn 90")
    private BigDecimal latitude;

    /**
     * Tọa độ Kinh độ (-180 đến 180), chọn trực tiếp từ bản đồ Leaflet.
     */
    @DecimalMin(value = "-180.0", message = "Kinh độ (longitude) không được nhỏ hơn -180")
    @DecimalMax(value = "180.0", message = "Kinh độ (longitude) không được lớn hơn 180")
    private BigDecimal longitude;
}
