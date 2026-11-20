package com.quickshop.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.identity.dto.AddressRequest;
import com.quickshop.identity.dto.AddressResponse;
import com.quickshop.identity.exception.GlobalExceptionHandler;
import com.quickshop.identity.security.JwtTokenProvider;
import com.quickshop.identity.service.UserAddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserAddressControllerTest - Sổ địa chỉ, kiểm tra số điện thoại & hỗ trợ PUT default")
class UserAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAddressService userAddressService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("TC-ADDR-03: Gửi SĐT '123' không hợp lệ -> Trả về HTTP 400 Bad Request thay vì 201 Created")
    void testCreateAddress_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyễn Văn A")
                .phoneNumber("123") // SĐT không hợp lệ
                .streetAddress("Số 10 Phạm Hùng")
                .ward("Mễ Trì")
                .district("Nam Từ Liêm")
                .city("Hà Nội")
                .latitude(new BigDecimal("21.028500"))
                .longitude(new BigDecimal("105.854200"))
                .build();

        mockMvc.perform(post("/api/users/addresses")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.phoneNumber").exists());
    }

    @Test
    @DisplayName("TC-ADDR-03: Gửi SĐT 10 số hợp lệ -> Trả về HTTP 201 Created")
    void testCreateAddress_ValidPhoneNumber_ReturnsCreated() throws Exception {
        AddressRequest request = AddressRequest.builder()
                .recipientName("Nguyễn Văn A")
                .phoneNumber("0912345678") // SĐT 10 số hợp lệ
                .streetAddress("Số 10 Phạm Hùng")
                .ward("Mễ Trì")
                .district("Nam Từ Liêm")
                .city("Hà Nội")
                .latitude(new BigDecimal("21.028500"))
                .longitude(new BigDecimal("105.854200"))
                .build();

        AddressResponse response = AddressResponse.builder()
                .id(1L)
                .userId(2L)
                .recipientName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .streetAddress("Số 10 Phạm Hùng")
                .fullAddress("Số 10 Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội")
                .build();

        when(userAddressService.createAddress(eq(2L), any(AddressRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/addresses")
                        .header("X-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phoneNumber").value("0912345678"));
    }

    @Test
    @DisplayName("TC-ADDR-04: Gọi PUT /api/users/addresses/2/default -> Thành công HTTP 200 OK")
    void testSetDefaultAddress_PutMethod_ReturnsOk() throws Exception {
        AddressResponse response = AddressResponse.builder()
                .id(2L)
                .userId(2L)
                .isDefault(true)
                .build();

        when(userAddressService.setDefaultAddress(2L, 2L)).thenReturn(response);

        mockMvc.perform(put("/api/users/addresses/2/default")
                        .header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("TC-ADDR-04: Gọi PATCH /api/users/addresses/2/default -> Vẫn thành công HTTP 200 OK")
    void testSetDefaultAddress_PatchMethod_ReturnsOk() throws Exception {
        AddressResponse response = AddressResponse.builder()
                .id(2L)
                .userId(2L)
                .isDefault(true)
                .build();

        when(userAddressService.setDefaultAddress(2L, 2L)).thenReturn(response);

        mockMvc.perform(patch("/api/users/addresses/2/default")
                        .header("X-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }
}
