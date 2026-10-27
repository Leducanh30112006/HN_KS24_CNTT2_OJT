package com.quickshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.dto.voucher.ApplyVoucherResult;
import com.quickshop.order.dto.voucher.ValidateVoucherRequest;
import com.quickshop.order.dto.voucher.VoucherResponse;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.exception.GlobalExceptionHandler;
import com.quickshop.order.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VoucherController.class)
@Import(GlobalExceptionHandler.class)
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/vouchers/{code} thành công -> Trả về 200 OK và chi tiết voucher")
    void testGetVoucher_Success() throws Exception {
        VoucherResponse response = VoucherResponse.builder()
                .id(1L)
                .code("QUICKSHOP10")
                .shopId(null)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderValue(BigDecimal.valueOf(200000))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .usageLimit(100)
                .usedCount(10)
                .remainingUsage(90)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherService.getVoucherByCode("QUICKSHOP10")).thenReturn(response);

        mockMvc.perform(get("/api/vouchers/QUICKSHOP10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("QUICKSHOP10"))
                .andExpect(jsonPath("$.data.remainingUsage").value(90));
    }

    @Test
    @DisplayName("POST /api/vouchers/preview thành công -> Trả về 200 OK và mức giảm giá xem trước")
    void testPreviewVoucher_Success() throws Exception {
        ValidateVoucherRequest request = ValidateVoucherRequest.builder()
                .code("SHOP20")
                .shopId(10L)
                .orderAmount(BigDecimal.valueOf(500000))
                .build();

        ApplyVoucherResult result = ApplyVoucherResult.builder()
                .code("SHOP20")
                .voucherId(2L)
                .shopId(10L)
                .discountAmount(BigDecimal.valueOf(50000))
                .applied(true)
                .build();

        when(voucherService.previewVoucherDiscount(eq("SHOP20"), eq(10L), any())).thenReturn(result);

        mockMvc.perform(post("/api/vouchers/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(50000));
    }

    @Test
    @DisplayName("POST /api/vouchers/preview thiếu mã voucher -> Trả về 400 Bad Request")
    void testPreviewVoucher_MissingCode_Returns400() throws Exception {
        ValidateVoucherRequest request = ValidateVoucherRequest.builder()
                .code("") // Rỗng
                .orderAmount(BigDecimal.valueOf(100000))
                .build();

        mockMvc.perform(post("/api/vouchers/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu đầu vào không hợp lệ"));
    }
}
