package com.quickshop.order.service;

import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.order.dto.voucher.*;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.VoucherUsage;
import com.quickshop.order.repository.VoucherRepository;
import com.quickshop.order.repository.VoucherUsageRepository;
import com.quickshop.order.service.impl.VoucherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceTest {

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherUsageRepository voucherUsageRepository;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    private Voucher platformPercentageVoucher;
    private Voucher shopFixedVoucher;

    @BeforeEach
    void setUp() {
        platformPercentageVoucher = Voucher.builder()
                .id(1L)
                .code("PLATFORM10")
                .shopId(null) // Voucher toàn sàn
                .discountType("PERCENTAGE")
                .discountValue(BigDecimal.valueOf(10)) // 10%
                .minOrderValue(BigDecimal.valueOf(100000))
                .maxDiscountAmount(BigDecimal.valueOf(30000)) // Tối đa 30k
                .usageLimit(100)
                .usedCount(5)
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(10))
                .status("ACTIVE")
                .build();

        shopFixedVoucher = Voucher.builder()
                .id(2L)
                .code("SHOP20K")
                .shopId(10L) // Voucher của Shop 10
                .discountType("FIXED_AMOUNT")
                .discountValue(BigDecimal.valueOf(20000)) // 20k
                .minOrderValue(BigDecimal.valueOf(80000))
                .usageLimit(50)
                .usedCount(10)
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(5))
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("validate: Voucher toàn sàn giảm % hợp lệ khi đạt điều kiện và không vượt trần")
    void testValidate_PlatformVoucher_Percentage_Success() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        // Subtotal = 200,000 -> 10% = 20,000 (dưới trần 30,000)
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(200000));

        assertNotNull(response);
        assertTrue(response.isValid());
        assertTrue(response.isPlatformVoucher());
        assertEquals("PLATFORM10", response.getCode());
        assertEquals(BigDecimal.valueOf(20000), response.getDiscountAmount());
        assertEquals("Áp dụng voucher thành công", response.getMessage());
    }

    @Test
    @DisplayName("validate: Voucher toàn sàn giảm % bị chặn trần khi mức giảm vượt quá maxDiscountAmount")
    void testValidate_PlatformVoucher_Percentage_CappedAtMax() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        // Subtotal = 500,000 -> 10% = 50,000 (vượt trần 30,000 -> bị chặn ở 30,000)
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(500000));

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(BigDecimal.valueOf(30000), response.getDiscountAmount());
    }

    @Test
    @DisplayName("validate: Voucher của Shop số tiền cố định áp dụng thành công cho đúng Shop")
    void testValidate_ShopVoucher_FixedAmount_Success() {
        when(voucherRepository.findByCode("SHOP20K")).thenReturn(Optional.of(shopFixedVoucher));

        // Shop 10, Subtotal = 100,000 (đạt min 80,000) -> giảm 20,000
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "SHOP20K", 10L, BigDecimal.valueOf(100000));

        assertNotNull(response);
        assertTrue(response.isValid());
        assertFalse(response.isPlatformVoucher());
        assertEquals(10L, response.getShopId());
        assertEquals(BigDecimal.valueOf(20000), response.getDiscountAmount());
    }

    @Test
    @DisplayName("validate: Voucher của Shop số tiền cố định không giảm vượt quá tiền hàng (cận dưới)")
    void testValidate_ShopVoucher_CappedToSubtotal() {
        shopFixedVoucher.setMinOrderValue(BigDecimal.valueOf(10000));
        when(voucherRepository.findByCode("SHOP20K")).thenReturn(Optional.of(shopFixedVoucher));

        // Shop 10, Subtotal = 15,000 mà voucher giảm 20,000 -> chỉ giảm tối đa 15,000
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "SHOP20K", 10L, BigDecimal.valueOf(15000));

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(BigDecimal.valueOf(15000), response.getDiscountAmount());
    }

    @Test
    @DisplayName("validate: Thất bại khi áp dụng Voucher của Shop này cho Shop khác")
    void testValidate_ShopVoucher_Fails_WhenAppliedToWrongShop() {
        when(voucherRepository.findByCode("SHOP20K")).thenReturn(Optional.of(shopFixedVoucher));

        // Voucher của Shop 10 nhưng áp dụng cho Shop 99
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "SHOP20K", 99L, BigDecimal.valueOf(100000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
        assertTrue(response.getMessage().contains("chỉ áp dụng cho Shop #10"));
    }

    @Test
    @DisplayName("validate: Thất bại khi nhập Voucher Shop vào ô Voucher toàn sàn")
    void testValidate_ShopVoucher_Fails_WhenAppliedAsPlatformVoucher() {
        when(voucherRepository.findByCode("SHOP20K")).thenReturn(Optional.of(shopFixedVoucher));

        // shopId = null nhưng voucher lại thuộc shop 10
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "SHOP20K", null, BigDecimal.valueOf(100000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("không thể áp dụng cho toàn sàn"));
    }

    @Test
    @DisplayName("validate: Thất bại khi nhập Voucher Sàn vào ô Voucher của Shop")
    void testValidate_PlatformVoucher_Fails_WhenAppliedAsShopVoucher() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        // shopId = 10L nhưng voucher lại là voucher sàn
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", 10L, BigDecimal.valueOf(100000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("vui lòng nhập vào ô Voucher Sàn"));
    }

    @Test
    @DisplayName("validate: Thất bại khi voucher đã hết hạn sử dụng (endDate quá khứ)")
    void testValidate_Fails_WhenVoucherExpired() {
        platformPercentageVoucher.setEndDate(LocalDateTime.now().minusDays(1));
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(200000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("đã hết hạn sử dụng"));
    }

    @Test
    @DisplayName("validate: Thất bại khi voucher chưa tới thời gian áp dụng (startDate tương lai)")
    void testValidate_Fails_WhenVoucherNotStarted() {
        platformPercentageVoucher.setStartDate(LocalDateTime.now().plusDays(2));
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(200000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("chưa tới thời gian áp dụng"));
    }

    @Test
    @DisplayName("validate: Thất bại khi voucher đã hết lượt sử dụng (usedCount >= usageLimit)")
    void testValidate_Fails_WhenUsageLimitReached() {
        platformPercentageVoucher.setUsageLimit(10);
        platformPercentageVoucher.setUsedCount(10);
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(200000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("hết lượt sử dụng"));
    }

    @Test
    @DisplayName("validate: Thất bại khi đơn hàng chưa đạt giá trị tối thiểu (minOrderValue)")
    void testValidate_Fails_WhenMinOrderValueNotMet() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        // minOrderValue = 100,000 nhưng subtotal = 50,000
        VoucherValidationResponse response = voucherService.validateAndCalculateVoucher(
                "PLATFORM10", null, BigDecimal.valueOf(50000));

        assertNotNull(response);
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("chưa đạt giá trị tối thiểu"));
    }

    @Test
    @DisplayName("validate: Thất bại khi voucher không tồn tại hoặc mã bị rỗng")
    void testValidate_Fails_WhenNotFoundOrBlank() {
        VoucherValidationResponse res1 = voucherService.validateAndCalculateVoucher("", null, BigDecimal.valueOf(100000));
        assertFalse(res1.isValid());

        when(voucherRepository.findByCode("NOTEXIST")).thenReturn(Optional.empty());
        VoucherValidationResponse res2 = voucherService.validateAndCalculateVoucher("NOTEXIST", null, BigDecimal.valueOf(100000));
        assertFalse(res2.isValid());
        assertTrue(res2.getMessage().contains("không tồn tại"));
    }

    @Test
    @DisplayName("calculateDiscounts: Tính toán thành công combo Voucher kép (Sàn + Shop)")
    void testCalculateDiscounts_ComboSuccess() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));
        when(voucherRepository.findByCode("SHOP20K")).thenReturn(Optional.of(shopFixedVoucher));

        VoucherApplyRequest request = VoucherApplyRequest.builder()
                .platformVoucherCode("PLATFORM10")
                .shopVouchers(Map.of(10L, "SHOP20K"))
                .shopSubtotals(Map.of(
                        10L, BigDecimal.valueOf(100000),
                        20L, BigDecimal.valueOf(200000)
                ))
                .build();

        VoucherApplyResponse response = voucherService.calculateDiscounts(request);

        assertNotNull(response);
        // Shop 10: 100k -> giảm 20k
        assertEquals(BigDecimal.valueOf(20000), response.getTotalShopDiscount());
        // Platform: tổng 300k -> 10% = 30k (chạm trần 30k)
        assertEquals(BigDecimal.valueOf(30000), response.getTotalPlatformDiscount());
        // Tổng giảm = 20k + 30k = 50k
        assertEquals(BigDecimal.valueOf(50000), response.getTotalDiscount());
    }

    @Test
    @DisplayName("recordUsage: Ghi nhận lịch sử sử dụng, tăng usedCount và chuyển trạng thái EXHAUSTED khi hết lượt")
    void testRecordUsage_Success() {
        platformPercentageVoucher.setUsageLimit(6);
        platformPercentageVoucher.setUsedCount(5);
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));

        voucherService.recordUsage("PLATFORM10", 1L, 1001L, null, BigDecimal.valueOf(30000));

        // Verify save VoucherUsage
        ArgumentCaptor<VoucherUsage> usageCaptor = ArgumentCaptor.forClass(VoucherUsage.class);
        verify(voucherUsageRepository, times(1)).save(usageCaptor.capture());
        VoucherUsage savedUsage = usageCaptor.getValue();
        assertEquals("PLATFORM10", savedUsage.getVoucherCode());
        assertEquals(1L, savedUsage.getUserId());
        assertEquals(1001L, savedUsage.getMasterOrderId());
        assertNull(savedUsage.getSubOrderId());
        assertEquals(BigDecimal.valueOf(30000), savedUsage.getDiscountAmount());

        // Verify voucher usedCount incremented to 6 and status became EXHAUSTED
        assertEquals(6, platformPercentageVoucher.getUsedCount());
        assertEquals("EXHAUSTED", platformPercentageVoucher.getStatus());
        verify(voucherRepository, times(1)).save(platformPercentageVoucher);
    }

    @Test
    @DisplayName("getApplicableVouchers: Lọc chính xác các voucher khả dụng")
    void testGetApplicableVouchers_Success() {
        when(voucherRepository.findByShopIdIsNullAndStatus("ACTIVE"))
                .thenReturn(List.of(platformPercentageVoucher));

        List<VoucherResponse> result = voucherService.getApplicableVouchers(null, BigDecimal.valueOf(150000));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PLATFORM10", result.get(0).getCode());
    }

    @Test
    @DisplayName("getVoucherByCode: Tra cứu thành công hoặc ném ResourceNotFoundException")
    void testGetVoucherByCode() {
        when(voucherRepository.findByCode("PLATFORM10")).thenReturn(Optional.of(platformPercentageVoucher));
        VoucherResponse res = voucherService.getVoucherByCode("PLATFORM10");
        assertNotNull(res);
        assertEquals("PLATFORM10", res.getCode());

        when(voucherRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> voucherService.getVoucherByCode("UNKNOWN"));
    }
}
