package com.quickshop.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.identity.dto.CreateShopRequest;
import com.quickshop.identity.dto.ShopResponse;
import com.quickshop.identity.dto.UpdateShopRequest;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.exception.GlobalExceptionHandler;
import com.quickshop.identity.service.ShopService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShopController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("ShopController - REST APIs Onboarding KYC, Hồ sơ gian hàng và Phê duyệt")
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShopService shopService;

    @Test
    @DisplayName("POST /api/shops/register - Đăng ký mở gian hàng Onboarding KYC thành công (201 Created)")
    void registerShop_Success() throws Exception {
        CreateShopRequest request = CreateShopRequest.builder()
                .shopName("TechWorld Store")
                .warehouseAddress("123 Cầu Giấy, Hà Nội")
                .phoneNumber("0988776655")
                .taxCode("0108998877")
                .citizenId("001200001234")
                .businessType("BUSINESS")
                .bankCode("VCB")
                .bankAccountNumber("0011004567890")
                .bankAccountName("NGUYEN VAN A")
                .build();

        ShopResponse shopResponse = ShopResponse.builder()
                .id(1L)
                .ownerId(10L)
                .shopName("TechWorld Store")
                .status(ShopStatus.PENDING_APPROVAL)
                .warehouseAddress("123 Cầu Giấy, Hà Nội")
                .createdAt(LocalDateTime.now())
                .build();

        when(shopService.registerShop(eq(10L), any(CreateShopRequest.class))).thenReturn(shopResponse);

        mockMvc.perform(post("/api/shops/register")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.shopName").value("TechWorld Store"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
    }

    @Test
    @DisplayName("POST /api/shops/register - Validation thất bại khi thiếu thông tin bắt buộc")
    void registerShop_ValidationFailed() throws Exception {
        CreateShopRequest invalidRequest = CreateShopRequest.builder()
                .shopName("ab") // quá ngắn (< 3)
                .phoneNumber("123") // sai regex số điện thoại
                .build();

        mockMvc.perform(post("/api/shops/register")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/shops/my-shop - Lấy thông tin gian hàng cá nhân thành công")
    void getMyShop_Success() throws Exception {
        ShopResponse shopResponse = ShopResponse.builder()
                .id(1L)
                .ownerId(10L)
                .shopName("TechWorld Store")
                .status(ShopStatus.PENDING_APPROVAL)
                .build();

        when(shopService.getMyShop(10L)).thenReturn(shopResponse);

        mockMvc.perform(get("/api/shops/my-shop")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shopName").value("TechWorld Store"));
    }

    @Test
    @DisplayName("GET /api/shops/my-shop - Thất bại khi thiếu X-User-Id header")
    void getMyShop_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/shops/my-shop"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/shops/my-shop - Cập nhật gian hàng cá nhân thành công")
    void updateMyShop_Success() throws Exception {
        ShopResponse myShop = ShopResponse.builder().id(1L).ownerId(10L).shopName("TechWorld Store").build();
        ShopResponse updatedShop = ShopResponse.builder().id(1L).ownerId(10L).shopName("TechWorld Updated").build();

        UpdateShopRequest updateRequest = UpdateShopRequest.builder()
                .shopName("TechWorld Updated")
                .warehouseAddress("Địa chỉ mới")
                .build();

        when(shopService.getMyShop(10L)).thenReturn(myShop);
        when(shopService.updateShop(eq(10L), eq(1L), any(UpdateShopRequest.class))).thenReturn(updatedShop);

        mockMvc.perform(put("/api/shops/my-shop")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shopName").value("TechWorld Updated"));
    }

    @Test
    @DisplayName("GET /api/shops/{id} - Lấy chi tiết gian hàng thành công (Public API)")
    void getShopById_Success() throws Exception {
        ShopResponse shopResponse = ShopResponse.builder()
                .id(1L)
                .shopName("TechWorld Store")
                .status(ShopStatus.ACTIVE)
                .build();

        when(shopService.getShopById(1L)).thenReturn(shopResponse);

        mockMvc.perform(get("/api/shops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shopName").value("TechWorld Store"));
    }

    @Test
    @DisplayName("GET /api/shops - Lấy danh sách tất cả gian hàng (Public API)")
    void getAllShops_Success() throws Exception {
        ShopResponse shopResponse = ShopResponse.builder().id(1L).shopName("TechWorld Store").build();
        when(shopService.getAllShops()).thenReturn(List.of(shopResponse));

        mockMvc.perform(get("/api/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/approve - Phê duyệt gian hàng thành công (Admin)")
    void approveShop_Success() throws Exception {
        ShopResponse approvedShop = ShopResponse.builder()
                .id(1L)
                .status(ShopStatus.ACTIVE)
                .build();

        when(shopService.approveShop(eq(1L), any())).thenReturn(approvedShop);

        mockMvc.perform(put("/api/shops/1/approve")
                        .header("X-User-Id", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/reject - Từ chối gian hàng thành công (Admin)")
    void rejectShop_Success() throws Exception {
        ShopResponse rejectedShop = ShopResponse.builder()
                .id(1L)
                .status(ShopStatus.REJECTED)
                .rejectionReason("Không đạt yêu cầu")
                .build();

        when(shopService.rejectShop(eq(1L), any(), any())).thenReturn(rejectedShop);

        mockMvc.perform(put("/api/shops/1/reject")
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Không đạt yêu cầu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/suspend - Tạm khóa gian hàng thành công (Admin)")
    void suspendShop_Success() throws Exception {
        ShopResponse suspendedShop = ShopResponse.builder()
                .id(1L)
                .status(ShopStatus.SUSPENDED)
                .rejectionReason("Vi phạm chính sách")
                .build();

        when(shopService.suspendShop(eq(1L), any(), any())).thenReturn(suspendedShop);

        mockMvc.perform(put("/api/shops/1/suspend")
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Vi phạm chính sách\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PUT /api/shops/{id}/activate - Mở khóa gian hàng thành công (Admin)")
    void activateShop_Success() throws Exception {
        ShopResponse activatedShop = ShopResponse.builder()
                .id(1L)
                .status(ShopStatus.ACTIVE)
                .build();

        when(shopService.activateShop(eq(1L), any())).thenReturn(activatedShop);

        mockMvc.perform(put("/api/shops/1/activate")
                        .header("X-User-Id", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
