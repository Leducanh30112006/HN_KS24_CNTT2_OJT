package com.quickshop.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.exception.GlobalExceptionHandler;
import com.quickshop.catalog.service.ProductVariantService;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductVariantController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProductVariantController - REST APIs SKU & Biến thể, Giá, Tồn kho & Reserved Stock")
class ProductVariantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductVariantService variantService;

    @Test
    @DisplayName("GET /api/catalog/products/{productId}/variants - Lấy danh sách biến thể thành công")
    void getVariantsByProduct_Success() throws Exception {
        VariantDto v1 = VariantDto.builder()
                .id(1L)
                .productId(100L)
                .sku("SKU-RED")
                .color("Đỏ")
                .size("M")
                .price(new BigDecimal("299000"))
                .stock(50)
                .reservedStock(5)
                .availableStock(45)
                .build();

        when(variantService.getVariantsByProductId(100L)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/catalog/products/100/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-RED"))
                .andExpect(jsonPath("$.data[0].availableStock").value(45));
    }

    @Test
    @DisplayName("GET /api/catalog/variants/{id} - Lấy chi tiết biến thể theo ID")
    void getVariantById_Success() throws Exception {
        VariantDto v1 = VariantDto.builder()
                .id(1L)
                .productId(100L)
                .sku("SKU-RED")
                .price(new BigDecimal("299000"))
                .stock(50)
                .build();

        when(variantService.getVariantById(1L)).thenReturn(v1);

        mockMvc.perform(get("/api/catalog/variants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.sku").value("SKU-RED"));
    }

    @Test
    @DisplayName("GET /api/catalog/variants/sku/{sku} - Tra cứu biến thể theo mã SKU")
    void getVariantBySku_Success() throws Exception {
        VariantDto v1 = VariantDto.builder()
                .id(1L)
                .productId(100L)
                .sku("SKU-RED")
                .price(new BigDecimal("299000"))
                .stock(50)
                .build();

        when(variantService.getVariantBySku("SKU-RED")).thenReturn(v1);

        mockMvc.perform(get("/api/catalog/variants/sku/SKU-RED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-RED"));
    }

    @Test
    @DisplayName("POST /api/catalog/products/{productId}/variants - Thêm biến thể SKU thành công (201 Created)")
    void addVariant_Success() throws Exception {
        VariantCreateRequest request = VariantCreateRequest.builder()
                .sku("SKU-BLUE-L")
                .color("Xanh")
                .size("L")
                .price(new BigDecimal("350000"))
                .stock(100)
                .build();

        VariantDto created = VariantDto.builder()
                .id(2L)
                .productId(100L)
                .sku("SKU-BLUE-L")
                .price(new BigDecimal("350000"))
                .stock(100)
                .reservedStock(0)
                .availableStock(100)
                .build();

        when(variantService.addVariant(eq(100L), any(VariantCreateRequest.class), any(), any()))
                .thenReturn(created);

        mockMvc.perform(post("/api/catalog/products/100/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-BLUE-L"))
                .andExpect(jsonPath("$.data.stock").value(100));
    }

    @Test
    @DisplayName("POST /api/catalog/products/{productId}/variants - 400 Bad Request khi thiếu SKU hoặc giá âm")
    void addVariant_ValidationFailure() throws Exception {
        VariantCreateRequest invalidRequest = VariantCreateRequest.builder()
                .sku("") // Rỗng
                .price(new BigDecimal("-1000")) // Âm
                .stock(-5) // Âm
                .build();

        mockMvc.perform(post("/api/catalog/products/100/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/catalog/variants/{id} - Cập nhật thông tin biến thể thành công")
    void updateVariant_Success() throws Exception {
        VariantUpdateRequest request = VariantUpdateRequest.builder()
                .sku("SKU-BLUE-XL")
                .color("Xanh Đậm")
                .size("XL")
                .price(new BigDecimal("380000"))
                .stock(120)
                .build();

        VariantDto updated = VariantDto.builder()
                .id(2L)
                .productId(100L)
                .sku("SKU-BLUE-XL")
                .price(new BigDecimal("380000"))
                .stock(120)
                .build();

        when(variantService.updateVariant(eq(2L), any(VariantUpdateRequest.class), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/catalog/variants/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-BLUE-XL"));
    }

    @Test
    @DisplayName("PATCH /api/catalog/variants/{id}/price - Cập nhật giá bán thành công")
    void updatePrice_Success() throws Exception {
        VariantPriceUpdateRequest request = VariantPriceUpdateRequest.builder()
                .price(new BigDecimal("420000"))
                .build();

        VariantDto updated = VariantDto.builder()
                .id(2L)
                .productId(100L)
                .sku("SKU-BLUE-XL")
                .price(new BigDecimal("420000"))
                .stock(120)
                .build();

        when(variantService.updatePrice(eq(2L), eq(new BigDecimal("420000")), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/catalog/variants/2/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.price").value(420000));
    }

    @Test
    @DisplayName("PATCH /api/catalog/variants/{id}/stock - Điều chỉnh tồn kho thành công")
    void updateStock_Success() throws Exception {
        VariantStockUpdateRequest request = VariantStockUpdateRequest.builder()
                .newStock(80)
                .reason("Cập nhật tồn kho kiểm kê")
                .build();

        VariantDto updated = VariantDto.builder()
                .id(2L)
                .productId(100L)
                .sku("SKU-BLUE-XL")
                .price(new BigDecimal("420000"))
                .stock(80)
                .availableStock(80)
                .build();

        when(variantService.updateStock(eq(2L), any(VariantStockUpdateRequest.class), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/catalog/variants/2/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stock").value(80));
    }

    @Test
    @DisplayName("DELETE /api/catalog/variants/{id} - Xóa biến thể thành công")
    void deleteVariant_Success() throws Exception {
        mockMvc.perform(delete("/api/catalog/variants/2")
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/catalog/variants/{id} - 400 Bad Request khi cố xóa biến thể duy nhất")
    void deleteVariant_ThrowsBadRequestWhenLastVariant() throws Exception {
        doThrow(new BadRequestException("Không thể xóa biến thể duy nhất"))
                .when(variantService).deleteVariant(eq(2L), any(), any());

        mockMvc.perform(delete("/api/catalog/variants/2")
                        .header("X-Shop-Id", "1")
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không thể xóa biến thể duy nhất"));
    }
}

