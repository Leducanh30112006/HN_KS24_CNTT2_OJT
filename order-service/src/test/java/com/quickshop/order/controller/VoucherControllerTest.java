package com.quickshop.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.dto.voucher.*;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.exception.GlobalExceptionHandler;
import com.quickshop.order.repository.VoucherRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VoucherController.class)
@Import(GlobalExceptionHandler.class)
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoucherRepository voucherRepository;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /api/vouchers/validate -> Xác thực voucher thành công 200 OK")
    void testValidateVoucher_Success() throws Exception {
        VoucherValidationRequest req = VoucherValidationRequest.builder()
                .code("GIAM10K")
                .shopId(null)
                .orderSubtotal(BigDecimal.valueOf(100000))
                .build();

        VoucherValidationResponse resp = VoucherValidationResponse.builder()
                .code("GIAM10K")
                .valid(true)
                .discountAmount(BigDecimal.valueOf(10000))
                .message("Áp dụng voucher thành công")
                .build();

        when(voucherService.validateAndCalculateVoucher(eq("GIAM10K"), eq(null), any(BigDecimal.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/vouchers/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("GIAM10K"))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(10000));
    }

    @Test
    @DisplayName("POST /api/vouchers/apply -> Áp dụng combo Voucher kép 200 OK")
    void testApplyVouchers_Success() throws Exception {
        VoucherApplyRequest req = VoucherApplyRequest.builder()
                .platformVoucherCode("SAN10K")
                .shopVouchers(Map.of(10L, "SHOP5K"))
                .shopSubtotals(Map.of(10L, BigDecimal.valueOf(100000)))
                .build();

        VoucherApplyResponse resp = VoucherApplyResponse.builder()
                .totalPlatformDiscount(BigDecimal.valueOf(10000))
                .totalShopDiscount(BigDecimal.valueOf(5000))
                .totalDiscount(BigDecimal.valueOf(15000))
                .build();

        when(voucherService.calculateDiscounts(any(VoucherApplyRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/vouchers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDiscount").value(15000));
    }

    @Test
    @DisplayName("GET /api/vouchers/applicable -> Lấy danh sách voucher áp dụng được 200 OK")
    void testGetApplicableVouchers_Success() throws Exception {
        VoucherResponse v = VoucherResponse.builder()
                .id(1L)
                .code("FREESHIP")
                .platformVoucher(true)
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(25000))
                .status("ACTIVE")
                .build();

        when(voucherService.getApplicableVouchers(eq(null), any(BigDecimal.class)))
                .thenReturn(List.of(v));

        mockMvc.perform(get("/api/vouchers/applicable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("FREESHIP"));
    }

    @Test
    @DisplayName("GET /api/vouchers/code/{code} -> Tra cứu chi tiết voucher theo mã 200 OK")
    void testGetVoucherByCode_Success() throws Exception {
        VoucherResponse v = VoucherResponse.builder()
                .id(1L)
                .code("QUICKSHOP")
                .platformVoucher(true)
                .status("ACTIVE")
                .build();

        when(voucherService.getVoucherByCode("QUICKSHOP")).thenReturn(v);

        mockMvc.perform(get("/api/vouchers/code/QUICKSHOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("QUICKSHOP"));
    }

    @Test
    @DisplayName("GET /api/vouchers -> Trả về danh sách voucher thành công 200 OK")
    void testGetAllVouchers_Success() throws Exception {
        Voucher v1 = Voucher.builder()
                .id(1L)
                .code("FREESHIPMAX")
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(25000))
                .minOrderValue(BigDecimal.valueOf(99000))
                .usageLimit(1000)
                .usedCount(100)
                .status("ACTIVE")
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        when(voucherRepository.findAll()).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/vouchers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("FREESHIPMAX"));
    }

    @Test
    @DisplayName("GET /api/vouchers/{id} -> Thành công 200 OK khi tồn tại")
    void testGetVoucherById_Success() throws Exception {
        Voucher v1 = Voucher.builder()
                .id(1L)
                .code("QUICKSHOP50K")
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(50000))
                .status("ACTIVE")
                .build();

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(v1));

        mockMvc.perform(get("/api/vouchers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("QUICKSHOP50K"));
    }

    @Test
    @DisplayName("GET /api/vouchers/{id} -> 404 khi không tìm thấy")
    void testGetVoucherById_NotFound() throws Exception {
        when(voucherRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vouchers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/vouchers -> Tạo mới voucher thành công 201 Created")
    void testCreateVoucher_Success() throws Exception {
        Voucher request = Voucher.builder()
                .code("TET2026")
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(100000))
                .minOrderValue(BigDecimal.valueOf(500000))
                .usageLimit(500)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(15))
                .build();

        Voucher saved = Voucher.builder()
                .id(10L)
                .code("TET2026")
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(100000))
                .status("ACTIVE")
                .build();

        when(voucherRepository.existsByCode("TET2026")).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenReturn(saved);

        mockMvc.perform(post("/api/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TET2026"));
    }

    @Test
    @DisplayName("POST /api/vouchers -> 400 Bad Request khi mã bị rỗng")
    void testCreateVoucher_BlankCode() throws Exception {
        Voucher request = Voucher.builder().code("").build();

        mockMvc.perform(post("/api/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/vouchers/{id}/status -> Cập nhật trạng thái voucher 200 OK")
    void testUpdateVoucherStatus_Success() throws Exception {
        Voucher existing = Voucher.builder().id(1L).code("CODE1").status("ACTIVE").build();
        Voucher updated = Voucher.builder().id(1L).code("CODE1").status("INACTIVE").build();

        when(voucherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(voucherRepository.save(any(Voucher.class))).thenReturn(updated);

        mockMvc.perform(put("/api/vouchers/1/status")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("DELETE /api/vouchers/{id} -> Xóa voucher thành công 200 OK")
    void testDeleteVoucher_Success() throws Exception {
        when(voucherRepository.existsById(1L)).thenReturn(true);
        doNothing().when(voucherRepository).deleteById(1L);

        mockMvc.perform(delete("/api/vouchers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
