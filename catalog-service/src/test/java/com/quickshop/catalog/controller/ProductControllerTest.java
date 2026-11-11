package com.quickshop.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.exception.GlobalExceptionHandler;
import com.quickshop.catalog.service.ProductReviewService;
import com.quickshop.catalog.service.ProductService;
import com.quickshop.common.exception.BadRequestException;
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

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProductController - REST APIs CRUD Sản phẩm theo Shop & Phân quyền")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductReviewService reviewService;

    @Test
    @DisplayName("POST /api/catalog/products - Người bán tạo sản phẩm thành công trả về 201 Created")
    void createProduct_Success() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .categoryId(1L)
                .name("Tai nghe chống ồn Bluetooth")
                .variants(List.of(
                        VariantCreateRequest.builder()
                                .sku("HEAD-01")
                                .color("Đen")
                                .size("One Size")
                                .price(new BigDecimal("500000"))
                                .stock(20)
                                .build()
                ))
                .build();

        ProductDto createdDto = ProductDto.builder()
                .id(100L)
                .shopId(5L)
                .name(request.getName())
                .status(ProductStatus.PENDING)
                .build();

        when(productService.createProduct(any(), eq(5L), eq("ROLE_SELLER"))).thenReturn(createdDto);

        mockMvc.perform(post("/api/catalog/products")
                        .header("X-Shop-Id", "5")
                        .header("X-User-Role", "ROLE_SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/catalog/products - Người mua (ROLE_BUYER) cố tạo sản phẩm bị từ chối 403 Forbidden")
    void createProduct_AsBuyer_ReturnsForbidden() throws Exception {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .categoryId(1L)
                .name("Sản phẩm gian lận")
                .variants(List.of(
                        VariantCreateRequest.builder().sku("FAKE-01").price(BigDecimal.TEN).build()
                ))
                .build();

        when(productService.createProduct(any(), any(), eq("ROLE_BUYER")))
                .thenThrow(new ForbiddenException("Chỉ người bán hoặc quản trị viên mới có quyền tạo sản phẩm"));

        mockMvc.perform(post("/api/catalog/products")
                        .header("X-User-Role", "ROLE_BUYER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Chỉ người bán hoặc quản trị viên mới có quyền tạo sản phẩm"));
    }

    @Test
    @DisplayName("PUT /api/catalog/products/{id} - Shop khác sửa sản phẩm bị từ chối 403 Forbidden")
    void updateProduct_WrongShop_ReturnsForbidden() throws Exception {
        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .name("Tên mới lừa đảo")
                .build();

        when(productService.updateProduct(eq(100L), any(), eq(99L), eq("ROLE_SELLER")))
                .thenThrow(new ForbiddenException("Bạn không có quyền chỉnh sửa sản phẩm thuộc gian hàng khác"));

        mockMvc.perform(put("/api/catalog/products/100")
                        .header("X-Shop-Id", "99")
                        .header("X-User-Role", "ROLE_SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền chỉnh sửa sản phẩm thuộc gian hàng khác"));
    }

    @Test
    @DisplayName("DELETE /api/catalog/products/{id} - Shop khác xóa sản phẩm bị từ chối 403 Forbidden")
    void deleteProduct_WrongShop_ReturnsForbidden() throws Exception {
        doThrow(new ForbiddenException("Bạn không có quyền xóa sản phẩm thuộc gian hàng khác"))
                .when(productService).deleteProduct(eq(100L), eq(99L), eq("ROLE_SELLER"));

        mockMvc.perform(delete("/api/catalog/products/100")
                        .header("X-Shop-Id", "99")
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/catalog/products/{id}/submit - Nộp duyệt sản phẩm chuyển sang PENDING thành công")
    void submitProduct_Success() throws Exception {
        ProductDto submittedDto = ProductDto.builder()
                .id(100L)
                .shopId(5L)
                .status(ProductStatus.PENDING)
                .build();

        when(productService.submitForApproval(eq(100L), eq(5L), eq("ROLE_SELLER"))).thenReturn(submittedDto);

        mockMvc.perform(post("/api/catalog/products/100/submit")
                        .header("X-Shop-Id", "5")
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/catalog/products/{id}/status - Admin duyệt APPROVED thành công")
    void updateStatus_ByAdmin_Success() throws Exception {
        ProductStatusUpdateRequest request = ProductStatusUpdateRequest.builder()
                .status(ProductStatus.APPROVED)
                .build();

        ProductDto approvedDto = ProductDto.builder()
                .id(100L)
                .status(ProductStatus.APPROVED)
                .build();

        when(productService.updateProductStatus(eq(100L), eq(ProductStatus.APPROVED), isNull(), eq("ROLE_ADMIN")))
                .thenReturn(approvedDto);

        mockMvc.perform(patch("/api/catalog/products/100/status")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/catalog/products/{id}/status - Không phải Admin bị từ chối 403 Forbidden")
    void updateStatus_ByNonAdmin_ReturnsForbidden() throws Exception {
        ProductStatusUpdateRequest request = ProductStatusUpdateRequest.builder()
                .status(ProductStatus.APPROVED)
                .build();

        when(productService.updateProductStatus(eq(100L), any(), any(), eq("ROLE_SELLER")))
                .thenThrow(new ForbiddenException("Chỉ Quản trị viên hệ thống (ROLE_ADMIN) mới có quyền kiểm duyệt sản phẩm"));

        mockMvc.perform(patch("/api/catalog/products/100/status")
                        .header("X-User-Role", "ROLE_SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Chỉ Quản trị viên hệ thống (ROLE_ADMIN) mới có quyền kiểm duyệt sản phẩm"));
    }

    @Test
    @DisplayName("GET /api/catalog/variants/{variantId}/validation - Contract API cho Order Service")
    void getVariantValidation_Success() throws Exception {
        CatalogVariantValidationDto validation = CatalogVariantValidationDto.builder()
                .variantId(200L)
                .productId(100L)
                .shopId(5L)
                .shopName("Gian Hàng 5")
                .sku("KEY-75-WHITE")
                .price(new BigDecimal("1200000"))
                .stock(50)
                .reservedStock(5)
                .availableStock(45)
                .active(true)
                .build();

        when(productService.getVariantValidation(200L)).thenReturn(validation);

        mockMvc.perform(get("/api/catalog/variants/200/validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variantId").value(200L))
                .andExpect(jsonPath("$.data.availableStock").value(45))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("GET /api/catalog/products/admin - Admin lấy danh sách kiểm duyệt mọi trạng thái (200 OK)")
    void getProductsForAdmin_Success() throws Exception {
        PageResult<ProductDto> adminPage = PageResult.<ProductDto>builder()
                .content(List.of(ProductDto.builder().id(100L).name("SP Chờ duyệt").status(ProductStatus.PENDING).build()))
                .page(0)
                .size(50)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(productService.getProductsForAdmin(isNull(), isNull(), isNull(), isNull(), eq(0), eq(50), eq("ROLE_ADMIN")))
                .thenReturn(adminPage);

        mockMvc.perform(get("/api/catalog/products/admin")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/catalog/products/admin - Không phải Admin bị từ chối 403 Forbidden")
    void getProductsForAdmin_AsNonAdmin_ReturnsForbidden() throws Exception {
        when(productService.getProductsForAdmin(any(), any(), any(), any(), anyInt(), anyInt(), eq("ROLE_BUYER")))
                .thenThrow(new ForbiddenException("Chỉ Admin mới có quyền truy cập danh sách kiểm duyệt toàn hệ thống"));

        mockMvc.perform(get("/api/catalog/products/admin")
                        .header("X-User-Role", "ROLE_BUYER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
