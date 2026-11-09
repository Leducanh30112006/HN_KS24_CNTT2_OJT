package com.quickshop.order.controller;

import com.quickshop.common.event.OrderDlqMessage;
import com.quickshop.order.saga.OrderDlqListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderDlqController - REST API tra cứu và quản lý Dead Letter Queue")
class OrderDlqControllerTest {

    @Mock
    private OrderDlqListener dlqListener;

    @InjectMocks
    private OrderDlqController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/orders/dlq/messages: Trả về danh sách thông điệp lỗi trong DLQ")
    void testGetDlqMessages() throws Exception {
        OrderDlqMessage msg = OrderDlqMessage.builder()
                .originalExchange("order.exchange")
                .originalRoutingKey("order.created")
                .errorMessage("Database connection refused")
                .retryCount(3)
                .build();

        when(dlqListener.getDlqMessages()).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/orders/dlq/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMessages").value(1))
                .andExpect(jsonPath("$.data.messages[0].originalRoutingKey").value("order.created"))
                .andExpect(jsonPath("$.data.messages[0].retryCount").value(3));
    }

    @Test
    @DisplayName("DELETE /api/orders/dlq/messages: Làm sạch bộ đệm DLQ thành công")
    void testClearDlqMessages() throws Exception {
        mockMvc.perform(delete("/api/orders/dlq/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dlqListener, times(1)).clearDlqMessages();
    }
}
