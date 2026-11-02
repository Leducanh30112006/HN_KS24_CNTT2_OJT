package com.quickshop.catalog.controller;

import com.quickshop.catalog.dto.StockReserveRequest;
import com.quickshop.catalog.service.InventoryService;
import com.quickshop.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory & Stock Reservation", description = "Quản lý tồn kho thực tế, giữ kho 15 phút theo Saga Orchestrator")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/reserve")
    @Operation(summary = "Giữ kho tạm thời (Saga Step: Reserve Stock)")
    public ResponseEntity<ApiResponse<Boolean>> reserveStock(@RequestBody StockReserveRequest request) {
        boolean ok = inventoryService.reserveStock(request.getOrderId(), request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Giữ kho thành công cho đơn hàng", ok));
    }

    @PostMapping("/release")
    @Operation(summary = "Giải phóng tồn kho khi hủy đơn hoặc thanh toán thất bại (Saga Rollback)")
    public ResponseEntity<ApiResponse<Void>> releaseStock(@RequestBody StockReserveRequest request) {
        inventoryService.releaseStock(request.getOrderId(), request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Giải phóng tồn kho thành công", null));
    }

    @PostMapping("/commit")
    @Operation(summary = "Xác nhận trừ kho chính thức sau khi đơn thanh toán thành công")
    public ResponseEntity<ApiResponse<Void>> commitStock(@RequestBody StockReserveRequest request) {
        inventoryService.commitStock(request.getOrderId(), request.getItems());
        return ResponseEntity.ok(ApiResponse.success("Xác nhận trừ kho thành công", null));
    }
}
