package com.quickshop.order.controller;

import com.quickshop.common.dto.ApiResponse;
import com.quickshop.common.event.OrderDlqMessage;
import com.quickshop.order.saga.OrderDlqListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders/dlq")
@RequiredArgsConstructor
@Tag(name = "Dead Letter Queue & Retry Management", description = "Giám sát thông điệp lỗi trong Dead Letter Queue order.dlq sau 3 lần retry")
public class OrderDlqController {

    private final OrderDlqListener dlqListener;

    @GetMapping("/messages")
    @Operation(summary = "Lấy danh sách các thông điệp sự kiện lỗi trong Dead Letter Queue order.dlq")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDlqMessages() {
        List<OrderDlqMessage> messages = dlqListener.getDlqMessages();
        Map<String, Object> data = Map.of(
                "totalMessages", messages.size(),
                "messages", messages
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách Dead Letter Queue thành công", data));
    }

    @DeleteMapping("/messages")
    @Operation(summary = "Xóa toàn bộ bộ đệm thông điệp sự kiện trong Dead Letter Queue")
    public ResponseEntity<ApiResponse<Void>> clearDlqMessages() {
        dlqListener.clearDlqMessages();
        return ResponseEntity.ok(ApiResponse.success("Đã dọn dẹp bộ đệm Dead Letter Queue thành công", null));
    }
}
