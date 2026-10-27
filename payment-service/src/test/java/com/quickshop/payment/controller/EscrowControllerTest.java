package com.quickshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.payment.dto.*;
import com.quickshop.payment.entity.enums.EscrowStatus;
import com.quickshop.payment.exception.GlobalExceptionHandler;
import com.quickshop.payment.service.EscrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EscrowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EscrowService escrowService;

    @InjectMocks
    private EscrowController escrowController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(escrowController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. POST /api/payments/escrow/hold: Tạm giữ tiền thành công trả HTTP 200")
    void testHoldEscrow_Success() throws Exception {
        HoldEscrowRequest request = HoldEscrowRequest.builder()
                .subOrderId(101L)
                .masterOrderId(1001L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .build();

        EscrowResponse response = EscrowResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .masterOrderId(1001L)
                .shopId(5L)
                .amount(new BigDecimal("500000"))
                .commissionFee(new BigDecimal("15000"))
                .status(EscrowStatus.HOLDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(escrowService.holdEscrow(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/escrow/hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subOrderId").value(101))
                .andExpect(jsonPath("$.data.status").value("HOLDING"));
    }

    @Test
    @DisplayName("2. POST /api/payments/escrow/release: Giải phóng tiền Escrow thành công")
    void testReleaseEscrow_Success() throws Exception {
        ReleaseEscrowRequest request = ReleaseEscrowRequest.builder()
                .subOrderId(101L)
                .trigger("BUYER_CONFIRMED")
                .build();

        EscrowResponse response = EscrowResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .amount(new BigDecimal("500000"))
                .netSellerAmount(new BigDecimal("485000"))
                .status(EscrowStatus.RELEASED)
                .releasedAt(LocalDateTime.now())
                .build();

        when(escrowService.releaseEscrow(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/escrow/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RELEASED"))
                .andExpect(jsonPath("$.data.netSellerAmount").value(485000));
    }

    @Test
    @DisplayName("3. POST /api/payments/escrow/refund: Hoàn tiền Escrow thành công")
    void testRefundEscrow_Success() throws Exception {
        RefundEscrowRequest request = RefundEscrowRequest.builder()
                .subOrderId(101L)
                .refundAmount(new BigDecimal("500000"))
                .reason("Khách hàng khiếu nại trả hàng")
                .build();

        EscrowResponse response = EscrowResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.REFUNDED)
                .refundedAt(LocalDateTime.now())
                .build();

        when(escrowService.refundEscrow(any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/escrow/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("4. POST /api/payments/escrow/dispute: Đóng băng giải phóng khi có khiếu nại")
    void testDisputeEscrow_Success() throws Exception {
        DisputeEscrowRequest request = DisputeEscrowRequest.builder()
                .subOrderId(101L)
                .reason("Khiếu nại sản phẩm sai mô tả")
                .build();

        EscrowResponse response = EscrowResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .isDisputed(true)
                .disputeReason("Khiếu nại sản phẩm sai mô tả")
                .status(EscrowStatus.HOLDING)
                .build();

        when(escrowService.freezeEscrowForDispute(101L, "Khiếu nại sản phẩm sai mô tả")).thenReturn(response);

        mockMvc.perform(post("/api/payments/escrow/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDisputed").value(true));
    }

    @Test
    @DisplayName("5. GET /api/payments/escrow/sub-orders/{subOrderId}: Tra cứu Escrow theo subOrderId")
    void testGetEscrowBySubOrderId() throws Exception {
        EscrowResponse response = EscrowResponse.builder()
                .id(1L)
                .subOrderId(101L)
                .amount(new BigDecimal("500000"))
                .status(EscrowStatus.HOLDING)
                .build();

        when(escrowService.getEscrowBySubOrderId(101L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/escrow/sub-orders/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subOrderId").value(101));
    }
}
