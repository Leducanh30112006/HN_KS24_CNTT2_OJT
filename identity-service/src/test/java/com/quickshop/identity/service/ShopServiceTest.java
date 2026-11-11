package com.quickshop.identity.service;

import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.dto.CreateShopRequest;
import com.quickshop.identity.dto.ShopResponse;
import com.quickshop.identity.dto.UpdateShopRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.producer.ShopEventProducer;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.service.AuditLogService;
import com.quickshop.identity.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopService - Quản lý Shop Onboarding KYC, Hồ sơ gian hàng và Phê duyệt")
class ShopServiceTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShopEventProducer shopEventProducer;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ShopServiceImpl shopService;

    private User sampleUser;
    private Shop sampleShop;
    private CreateShopRequest createShopRequest;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(10L)
                .username("seller_user")
                .email("seller@quickshop.vn")
                .fullName("Nguyễn Văn Bán")
                .role(Role.ROLE_BUYER)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();

        sampleShop = Shop.builder()
                .id(1L)
                .ownerId(10L)
                .shopName("TechWorld Store")
                .warehouseAddress("123 Cầu Giấy, Hà Nội")
                .phoneNumber("0988776655")
                .taxCode("0108998877")
                .citizenId("001200001234")
                .businessType("BUSINESS")
                .description("Phụ kiện chính hãng")
                .logoUrl("https://img.jpg")
                .bankCode("VCB")
                .bankAccountNumber("0011004567890")
                .bankAccountName("NGUYEN VAN BAN")
                .status(ShopStatus.PENDING_APPROVAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createShopRequest = CreateShopRequest.builder()
                .shopName("TechWorld Store")
                .warehouseAddress("123 Cầu Giấy, Hà Nội")
                .phoneNumber("0988776655")
                .taxCode("0108998877")
                .citizenId("001200001234")
                .businessType("BUSINESS")
                .description("Phụ kiện chính hãng")
                .logoUrl("https://img.jpg")
                .bankCode("VCB")
                .bankAccountNumber("0011004567890")
                .bankAccountName("NGUYEN VAN BAN")
                .build();
    }

    @Test
    @DisplayName("Đăng ký gian hàng Onboarding thành công - Trạng thái mặc định là PENDING_APPROVAL")
    void registerShop_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(shopRepository.existsByOwnerId(10L)).thenReturn(false);
        when(shopRepository.existsByShopName("TechWorld Store")).thenReturn(false);
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);

        ShopResponse response = shopService.registerShop(10L, createShopRequest);

        assertThat(response).isNotNull();
        assertThat(response.getShopName()).isEqualTo("TechWorld Store");
        assertThat(response.getStatus()).isEqualTo(ShopStatus.PENDING_APPROVAL);
        assertThat(response.getOwnerId()).isEqualTo(10L);
        verify(shopRepository, times(1)).save(any(Shop.class));
    }

    @Test
    @DisplayName("Đăng ký gian hàng thất bại - Thiếu ownerId")
    void registerShop_MissingOwnerId_ThrowsBadRequest() {
        assertThatThrownBy(() -> shopService.registerShop(null, createShopRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa được xác thực danh tính");
    }

    @Test
    @DisplayName("Đăng ký gian hàng thất bại - Tài khoản người dùng không tồn tại")
    void registerShop_UserNotFound_ThrowsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.registerShop(99L, createShopRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Đăng ký gian hàng thất bại - Tài khoản người dùng bị khóa")
    void registerShop_UserBlocked_ThrowsBadRequest() {
        sampleUser.setStatus(UserStatus.BLOCKED);
        sampleUser.setActive(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> shopService.registerShop(10L, createShopRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bị khóa hoặc chưa được kích hoạt");
    }

    @Test
    @DisplayName("Đăng ký gian hàng thất bại - Tài khoản đã có hồ sơ gian hàng trước đó")
    void registerShop_OwnerAlreadyHasShop_ThrowsBadRequest() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(shopRepository.existsByOwnerId(10L)).thenReturn(true);

        assertThatThrownBy(() -> shopService.registerShop(10L, createShopRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã có hồ sơ gian hàng trên hệ thống");
    }

    @Test
    @DisplayName("Đăng ký gian hàng thất bại - Trùng tên gian hàng đã tồn tại")
    void registerShop_DuplicateShopName_ThrowsBadRequest() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(shopRepository.existsByOwnerId(10L)).thenReturn(false);
        when(shopRepository.existsByShopName("TechWorld Store")).thenReturn(true);

        assertThatThrownBy(() -> shopService.registerShop(10L, createShopRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã được sử dụng");
    }

    @Test
    @DisplayName("Cập nhật thông tin gian hàng thành công")
    void updateShop_Success() {
        UpdateShopRequest updateRequest = UpdateShopRequest.builder()
                .warehouseAddress("456 Trần Duy Hưng, Cầu Giấy")
                .phoneNumber("0911223344")
                .description("Mô tả mới")
                .build();

        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);

        ShopResponse response = shopService.updateShop(10L, 1L, updateRequest);

        assertThat(response).isNotNull();
        verify(shopRepository, times(1)).save(sampleShop);
    }

    @Test
    @DisplayName("Cập nhật gian hàng khi bị REJECTED - Tự động đưa lại về PENDING_APPROVAL để chờ duyệt lại")
    void updateShop_RejectedStatus_ResetsToPendingApproval() {
        sampleShop.setStatus(ShopStatus.REJECTED);
        UpdateShopRequest updateRequest = UpdateShopRequest.builder()
                .warehouseAddress("Địa chỉ kho mới đã khắc phục")
                .build();

        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);

        ShopResponse response = shopService.updateShop(10L, 1L, updateRequest);

        assertThat(sampleShop.getStatus()).isEqualTo(ShopStatus.PENDING_APPROVAL);
        verify(shopRepository, times(1)).save(sampleShop);
    }

    @Test
    @DisplayName("Cập nhật gian hàng thất bại - Không phải chủ gian hàng và không phải Admin")
    void updateShop_NotOwner_ThrowsBadRequest() {
        User otherBuyer = User.builder().id(20L).role(Role.ROLE_BUYER).build();
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(userRepository.findById(20L)).thenReturn(Optional.of(otherBuyer));

        UpdateShopRequest updateRequest = UpdateShopRequest.builder().phoneNumber("0900000000").build();

        assertThatThrownBy(() -> shopService.updateShop(20L, 1L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không có quyền chỉnh sửa");
    }

    @Test
    @DisplayName("Lấy thông tin gian hàng cá nhân (getMyShop) thành công")
    void getMyShop_Success() {
        when(shopRepository.findByOwnerId(10L)).thenReturn(Optional.of(sampleShop));

        ShopResponse response = shopService.getMyShop(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getShopName()).isEqualTo("TechWorld Store");
    }

    @Test
    @DisplayName("Lấy thông tin gian hàng cá nhân thất bại khi chưa có gian hàng")
    void getMyShop_NotFound_ThrowsResourceNotFound() {
        when(shopRepository.findByOwnerId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.getMyShop(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Admin phê duyệt gian hàng (approveShop) - Chuyển sang ACTIVE, nâng quyền ROLE_SELLER, phát ShopApprovedEvent và ghi audit log")
    void approveShop_Success() {
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));

        ShopResponse response = shopService.approveShop(1L, 99L);

        assertThat(sampleShop.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        assertThat(sampleUser.getRole()).isEqualTo(Role.ROLE_SELLER);
        verify(userRepository, times(1)).save(sampleUser);
        verify(shopRepository, times(1)).save(sampleShop);
        verify(shopEventProducer, times(1)).publishShopApproved(any(ShopApprovedEvent.class));
        verify(auditLogService, times(1)).logSuccess(eq(99L), any(), eq("SHOP_APPROVED"), eq("Shop"), eq(1L), any());
    }

    @Test
    @DisplayName("Admin từ chối gian hàng (rejectShop) - Chuyển sang REJECTED, phát ShopRejectedEvent và ghi audit log")
    void rejectShop_Success() {
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));

        ShopResponse response = shopService.rejectShop(1L, 99L, "Thông tin CCCD không rõ ràng");

        assertThat(sampleShop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(sampleShop.getRejectionReason()).isEqualTo("Thông tin CCCD không rõ ràng");
        verify(shopRepository, times(1)).save(sampleShop);
        verify(shopEventProducer, times(1)).publishShopRejected(any(ShopRejectedEvent.class));
        verify(auditLogService, times(1)).logSuccess(eq(99L), any(), eq("SHOP_REJECTED"), eq("Shop"), eq(1L), any());
    }

    @Test
    @DisplayName("Admin tạm khóa gian hàng (suspendShop) - Chuyển sang SUSPENDED và ghi audit log")
    void suspendShop_Success() {
        sampleShop.setStatus(ShopStatus.ACTIVE);
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);

        ShopResponse response = shopService.suspendShop(1L, 99L, "Vi phạm quy định bán hàng");

        assertThat(response.getStatus()).isEqualTo(ShopStatus.SUSPENDED);
        assertThat(sampleShop.getRejectionReason()).isEqualTo("Vi phạm quy định bán hàng");
        verify(auditLogService, times(1)).logSuccess(eq(99L), any(), eq("SHOP_SUSPENDED"), eq("Shop"), eq(1L), any());
    }

    @Test
    @DisplayName("Admin mở khóa gian hàng (activateShop) - Khôi phục ACTIVE, xóa lý do khóa và ghi audit log")
    void activateShop_Success() {
        sampleShop.setStatus(ShopStatus.SUSPENDED);
        sampleShop.setRejectionReason("Tạm khóa trước đó");
        when(shopRepository.findById(1L)).thenReturn(Optional.of(sampleShop));
        when(shopRepository.save(any(Shop.class))).thenReturn(sampleShop);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));

        ShopResponse response = shopService.activateShop(1L, 99L);

        assertThat(response.getStatus()).isEqualTo(ShopStatus.ACTIVE);
        assertThat(sampleShop.getRejectionReason()).isNull();
        verify(auditLogService, times(1)).logSuccess(eq(99L), any(), eq("SHOP_ACTIVATED"), eq("Shop"), eq(1L), any());
    }
}
