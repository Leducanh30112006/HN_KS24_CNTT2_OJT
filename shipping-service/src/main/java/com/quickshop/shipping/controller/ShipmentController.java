package com.quickshop.shipping.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.shipping.dto.*;
import com.quickshop.shipping.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Shipping & Logistics Controller", description = "Quản lý tính phí vận chuyển, tạo vận đơn và tra cứu hành trình đơn hàng")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping("/calculate-fee")
    @Operation(summary = "Tính phí vận chuyển dựa trên địa chỉ, cân nặng và kích thước")
    public ResponseEntity<ApiResponse<CalculateFeeResponse>> calculateFee(
            @Valid @RequestBody CalculateFeeRequest request) {
        CalculateFeeResponse response = shipmentService.calculateShippingFee(request);
        return ResponseEntity.ok(ApiResponse.ok("Tính phí vận chuyển thành công", response));
    }

    @PostMapping("/shipments")
    @Operation(summary = "Tạo vận đơn cho Sub-Order khi đạt điều kiện nghiệp vụ")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo vận đơn thành công", response));
    }

    @GetMapping("/shipments/{subOrderId}")
    @Operation(summary = "Lấy thông tin vận đơn theo Sub-Order ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentBySubOrderId(
            @PathVariable Long subOrderId) {
        ShipmentResponse response = shipmentService.getShipmentBySubOrderId(subOrderId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin vận đơn thành công", response));
    }

    @GetMapping("/shipments/tracking/{trackingCode}")
    @Operation(summary = "Tra cứu hành trình vận đơn theo Tracking Code")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByTrackingCode(
            @PathVariable String trackingCode) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingCode(trackingCode);
        return ResponseEntity.ok(ApiResponse.ok("Tra cứu vận đơn thành công", response));
    }

    @GetMapping("/shops/{shopId}/shipments")
    @Operation(summary = "Lấy danh sách vận đơn của Shop")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipmentsByShopId(
            @PathVariable Long shopId) {
        List<ShipmentResponse> responses = shipmentService.getShipmentsByShopId(shopId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách vận đơn của Shop thành công", responses));
    }

    @PatchMapping("/shipments/{trackingCode}/status")
    @Operation(summary = "Cập nhật trạng thái vận đơn")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateStatus(
            @PathVariable String trackingCode,
            @Valid @RequestBody UpdateShippingStatusRequest request) {
        ShipmentResponse response = shipmentService.updateShippingStatus(trackingCode, request.getNewStatus(), request.getNote());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái vận đơn thành công", response));
    }
}
