package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.order.entity.MasterOrder;
import com.quickshop.order.entity.SubOrder;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.SubOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final MasterOrderRepository masterOrderRepository;
    private final SubOrderRepository subOrderRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MasterOrder>>> getMyOrders(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            Long userId = Long.parseLong(userIdHeader);
            return ResponseEntity.ok(ApiResponse.success(masterOrderRepository.findByUserId(userId)));
        }
        return ResponseEntity.ok(ApiResponse.success(masterOrderRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderDetails(@PathVariable Long id) {
        Optional<MasterOrder> orderOpt = masterOrderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<SubOrder> subOrders = subOrderRepository.findByMasterOrderId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("masterOrder", orderOpt.get());
        result.put("subOrders", subOrders);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<List<SubOrder>>> getShopSubOrders(@PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.success(subOrderRepository.findByShopId(shopId)));
    }
}
