package com.quickshop.order.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.dto.voucher.ApplyVoucherResult;
import com.quickshop.order.dto.voucher.VoucherResponse;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    private VoucherServiceImpl voucherService;

    private static final Long SHOP_ID = 10L;
    private static final String SHOP_CODE = "SHOPVIP20";
    private static final String PLATFORM_CODE = "QUICKSHOP10";

    @BeforeEach
    void setUp() {
        voucherService = new VoucherServiceImpl(voucherRepository);
    }

    @Test
    @DisplayName("1. Áp dụng Voucher Shop dạng PERCENTAGE thành công, áp dụng mức trần max_discount_amount và cập nhật atomic")
    void testApplyShopVoucher_Percentage_Success() {
        // Given
        // Giảm 20% tối đa 50,000 VND cho đơn từ 100,000 VND
        // Đơn hàng: 300,000 VND -> 20% là 60,000 VND -> Capped ở 50,000 VND
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code(SHOP_CODE)
                .shopId(SHOP_ID)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(20))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .minOrderValue(BigDecimal.valueOf(100000))
                .usageLimit(100)
                .usedCount(10)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus(SHOP_CODE, VoucherStatus.ACTIVE)).thenReturn(Optional.of(voucher));
        when(voucherRepository.incrementUsageCountAtomic(eq(1L), eq(VoucherStatus.ACTIVE))).thenReturn(1);

        // When
        ApplyVoucherResult result = voucherService.applyShopVoucher(SHOP_CODE, SHOP_ID, BigDecimal.valueOf(300000));

        // Then
        assertNotNull(result);
        assertTrue(result.isApplied());
        assertEquals(SHOP_CODE, result.getCode());
        assertEquals(BigDecimal.valueOf(50000), result.getDiscountAmount());
        verify(voucherRepository, times(1)).incrementUsageCountAtomic(eq(1L), eq(VoucherStatus.ACTIVE));
    }

    @Test
    @DisplayName("2. Áp dụng Voucher Shop dạng FIXED_AMOUNT thành công, không vượt quá giá trị đơn hàng")
    void testApplyShopVoucher_FixedAmount_Success() {
        // Given
        // Giảm 100,000 VND cho đơn từ 200,000 VND
        Voucher voucher = Voucher.builder()
                .id(2L)
                .code("SHOPFIX100")
                .shopId(SHOP_ID)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(100000))
                .minOrderValue(BigDecimal.valueOf(200000))
                .usageLimit(50)
                .usedCount(5)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("SHOPFIX100", VoucherStatus.ACTIVE)).thenReturn(Optional.of(voucher));
        when(voucherRepository.incrementUsageCountAtomic(eq(2L), eq(VoucherStatus.ACTIVE))).thenReturn(1);

        // When
        ApplyVoucherResult result = voucherService.applyShopVoucher("SHOPFIX100", SHOP_ID, BigDecimal.valueOf(250000));

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(100000), result.getDiscountAmount());
        verify(voucherRepository, times(1)).incrementUsageCountAtomic(eq(2L), eq(VoucherStatus.ACTIVE));
    }

    @Test
    @DisplayName("3. Áp dụng Voucher toàn sàn (Platform Voucher, shop_id = null) thành công")
    void testApplyPlatformVoucher_Success() {
        // Given
        Voucher platformVoucher = Voucher.builder()
                .id(3L)
                .code(PLATFORM_CODE)
                .shopId(null) // Toàn sàn
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .minOrderValue(BigDecimal.valueOf(500000))
                .usageLimit(1000)
                .usedCount(100)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus(PLATFORM_CODE, VoucherStatus.ACTIVE)).thenReturn(Optional.of(platformVoucher));
        when(voucherRepository.incrementUsageCountAtomic(eq(3L), eq(VoucherStatus.ACTIVE))).thenReturn(1);

        // When: Đơn hàng toàn sàn 1,000,000 VND -> Giảm 10% = 100,000 VND
        ApplyVoucherResult result = voucherService.applyPlatformVoucher(PLATFORM_CODE, BigDecimal.valueOf(1000000));

        // Then
        assertNotNull(result);
        assertTrue(result.isApplied());
        assertEquals(BigDecimal.valueOf(100000), result.getDiscountAmount());
        assertNull(result.getShopId());
        verify(voucherRepository, times(1)).incrementUsageCountAtomic(eq(3L), eq(VoucherStatus.ACTIVE));
    }

    @Test
    @DisplayName("4. Voucher đã hết hạn sử dụng (now > end_date) -> Ném lỗi BadRequestException")
    void testApplyVoucher_ExpiredDate_ThrowsBadRequestException() {
        // Given
        Voucher expiredVoucher = Voucher.builder()
                .id(4L)
                .code("EXPIRED_VOUCHER")
                .shopId(SHOP_ID)
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().minusDays(1)) // Đã hết hạn hôm qua
                .usageLimit(100)
                .usedCount(5)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("EXPIRED_VOUCHER", VoucherStatus.ACTIVE)).thenReturn(Optional.of(expiredVoucher));

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("EXPIRED_VOUCHER", SHOP_ID, BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("đã hết hạn"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("5. Voucher chưa đến thời gian áp dụng (now < start_date) -> Ném lỗi BadRequestException")
    void testApplyVoucher_NotStartedYet_ThrowsBadRequestException() {
        // Given
        Voucher futureVoucher = Voucher.builder()
                .id(5L)
                .code("FUTURE_VOUCHER")
                .shopId(SHOP_ID)
                .startDate(LocalDateTime.now().plusDays(2)) // 2 ngày nữa mới mở
                .endDate(LocalDateTime.now().plusDays(10))
                .usageLimit(100)
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("FUTURE_VOUCHER", VoucherStatus.ACTIVE)).thenReturn(Optional.of(futureVoucher));

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("FUTURE_VOUCHER", SHOP_ID, BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("chưa tới thời gian bắt đầu"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("6. Voucher không ở trạng thái ACTIVE (INACTIVE/EXPIRED) -> Ném lỗi BadRequestException")
    void testApplyVoucher_InactiveStatus_ThrowsBadRequestException() {
        // Given
        Voucher inactiveVoucher = Voucher.builder()
                .id(6L)
                .code("INACTIVE_VOUCHER")
                .shopId(SHOP_ID)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .status(VoucherStatus.INACTIVE) // Không ACTIVE
                .build();

        when(voucherRepository.findByCodeAndStatus("INACTIVE_VOUCHER", VoucherStatus.ACTIVE)).thenReturn(Optional.empty());

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("INACTIVE_VOUCHER", SHOP_ID, BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("không tồn tại hoặc đã hết hiệu lực"));
    }

    @Test
    @DisplayName("7. Voucher shop áp dụng sai shop (Shop A áp vào Shop B) -> Ném lỗi BadRequestException")
    void testApplyShopVoucher_WrongShop_ThrowsBadRequestException() {
        // Given: Voucher thuộc Shop ID 10
        Voucher shopVoucher = Voucher.builder()
                .id(7L)
                .code("SHOP10_VOUCHER")
                .shopId(SHOP_ID) // 10L
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(20000))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("SHOP10_VOUCHER", VoucherStatus.ACTIVE)).thenReturn(Optional.of(shopVoucher));

        // When & Then: Thử áp dụng cho Shop ID 999
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("SHOP10_VOUCHER", 999L, BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("không thuộc về Shop ID 999"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("8. Dùng Voucher riêng của shop làm voucher toàn sàn -> Bị từ chối và ném lỗi BadRequestException")
    void testApplyPlatformVoucher_UsingShopVoucher_ThrowsBadRequestException() {
        // Given: Voucher có shop_id = 10
        Voucher shopVoucher = Voucher.builder()
                .id(8L)
                .code("SHOP_ONLY_CODE")
                .shopId(SHOP_ID) // Không phải toàn sàn
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("SHOP_ONLY_CODE", VoucherStatus.ACTIVE)).thenReturn(Optional.of(shopVoucher));

        // When & Then: Thử áp làm voucher toàn sàn
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyPlatformVoucher("SHOP_ONLY_CODE", BigDecimal.valueOf(1000000))
        );
        assertTrue(ex.getMessage().contains("là voucher riêng của shop"));
        assertTrue(ex.getMessage().contains("không thể dùng làm voucher toàn sàn"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("9. Dùng Voucher toàn sàn làm voucher riêng của shop -> Bị từ chối và ném lỗi BadRequestException")
    void testApplyShopVoucher_UsingPlatformVoucher_ThrowsBadRequestException() {
        // Given: Voucher có shop_id = null
        Voucher platformVoucher = Voucher.builder()
                .id(9L)
                .code("PLATFORM_ONLY")
                .shopId(null) // Toàn sàn
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("PLATFORM_ONLY", VoucherStatus.ACTIVE)).thenReturn(Optional.of(platformVoucher));

        // When & Then: Thử áp làm voucher của shop 10
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("PLATFORM_ONLY", SHOP_ID, BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("là voucher toàn sàn, không thể dùng làm voucher riêng của shop"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("10. Đơn hàng chưa đạt giá trị tối thiểu (min_order_value) -> Ném lỗi BadRequestException")
    void testApplyVoucher_BelowMinOrderValue_ThrowsBadRequestException() {
        // Given: Yêu cầu tối thiểu 500,000 VND
        Voucher voucher = Voucher.builder()
                .id(10L)
                .code("MIN500")
                .shopId(SHOP_ID)
                .minOrderValue(BigDecimal.valueOf(500000))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usedCount(0)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("MIN500", VoucherStatus.ACTIVE)).thenReturn(Optional.of(voucher));

        // When & Then: Tiền hàng chỉ có 300,000 VND
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyShopVoucher("MIN500", SHOP_ID, BigDecimal.valueOf(300000))
        );

        assertTrue(ex.getMessage().contains("chưa đạt giá trị tối thiểu"));
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("11. Cạnh tranh usage_limit: Khi Atomic Update ở CSDL trả về 0 -> Ném lỗi BadRequestException ngăn overselling")
    void testApplyVoucher_ConcurrentUsageLimitReached_ThrowsBadRequestException() {
        // Given: Voucher còn 1 lượt dùng
        Voucher voucher = Voucher.builder()
                .id(11L)
                .code("HOT_DEAL")
                .shopId(null)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(50000))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(10)
                .usedCount(9) // Còn 1 lượt
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("HOT_DEAL", VoucherStatus.ACTIVE)).thenReturn(Optional.of(voucher));
        // Mocking: Một transaction khác đã nhanh tay update trước -> update count = 0
        when(voucherRepository.incrementUsageCountAtomic(eq(11L), eq(VoucherStatus.ACTIVE))).thenReturn(0);

        // When & Then
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                voucherService.applyPlatformVoucher("HOT_DEAL", BigDecimal.valueOf(500000))
        );

        assertTrue(ex.getMessage().contains("đã hết số lượt sử dụng"));
    }

    @Test
    @DisplayName("12. Hoàn tác sử dụng voucher (Rollback khi Saga thất bại)")
    void testReleaseVoucherUsage_Success() {
        // Given
        when(voucherRepository.decrementUsageCountByCodeAtomic("ROLLBACK_VOUCHER")).thenReturn(1);

        // When
        voucherService.releaseVoucherUsage("ROLLBACK_VOUCHER");

        // Then
        verify(voucherRepository, times(1)).decrementUsageCountByCodeAtomic("ROLLBACK_VOUCHER");
    }

    @Test
    @DisplayName("13. Hoàn tác đồng thời Voucher sàn và danh sách Voucher các shop")
    void testReleaseVouchers_PlatformAndShops_Success() {
        // When
        voucherService.releaseVouchers("PLATFORM_CODE", List.of("SHOP1_CODE", "SHOP2_CODE"));

        // Then
        verify(voucherRepository, times(1)).decrementUsageCountByCodeAtomic("PLATFORM_CODE");
        verify(voucherRepository, times(1)).decrementUsageCountByCodeAtomic("SHOP1_CODE");
        verify(voucherRepository, times(1)).decrementUsageCountByCodeAtomic("SHOP2_CODE");
    }

    @Test
    @DisplayName("14. Xem trước (preview) voucher không làm tăng lượt sử dụng")
    void testPreviewVoucherDiscount_Success() {
        // Given
        Voucher voucher = Voucher.builder()
                .id(14L)
                .code("PREVIEW_CODE")
                .shopId(SHOP_ID)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10))
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .usageLimit(100)
                .usedCount(10)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCodeAndStatus("PREVIEW_CODE", VoucherStatus.ACTIVE)).thenReturn(Optional.of(voucher));

        // When
        ApplyVoucherResult result = voucherService.previewVoucherDiscount("PREVIEW_CODE", SHOP_ID, BigDecimal.valueOf(200000));

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(20000), result.getDiscountAmount());
        // Đảm bảo không gọi increment atomic khi xem trước
        verify(voucherRepository, never()).incrementUsageCountAtomic(any(), any());
    }

    @Test
    @DisplayName("15. Tra cứu voucher theo mã code tính đúng lượt dùng còn lại (remainingUsage)")
    void testGetVoucherByCode_Success() {
        // Given
        Voucher voucher = Voucher.builder()
                .id(15L)
                .code("LOOKUP_CODE")
                .usageLimit(50)
                .usedCount(15)
                .status(VoucherStatus.ACTIVE)
                .build();

        when(voucherRepository.findByCode("LOOKUP_CODE")).thenReturn(Optional.of(voucher));

        // When
        VoucherResponse response = voucherService.getVoucherByCode("LOOKUP_CODE");

        // Then
        assertNotNull(response);
        assertEquals(50, response.getUsageLimit());
        assertEquals(15, response.getUsedCount());
        assertEquals(35, response.getRemainingUsage()); // 50 - 15 = 35
    }

    @Test
    @DisplayName("16. Tra cứu voucher không tồn tại -> Ném ResourceNotFoundException")
    void testGetVoucherByCode_NotFound_ThrowsException() {
        when(voucherRepository.findByCode("UNKNOWN_CODE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                voucherService.getVoucherByCode("UNKNOWN_CODE")
        );
    }
}
