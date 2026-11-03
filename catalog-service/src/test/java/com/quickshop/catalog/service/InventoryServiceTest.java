package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.StockReserveItem;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService - Quản lý tồn kho thực tế, giữ kho (Reserve) và giải phóng (Release/Commit)")
class InventoryServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CatalogCacheService cacheService;

    @InjectMocks
    private InventoryService inventoryService;

    private ProductVariant variantA;
    private ProductVariant variantB;

    @BeforeEach
    void setUp() {
        variantA = ProductVariant.builder()
                .id(1L)
                .productId(10L)
                .sku("PHONE-PRO-256")
                .price(new BigDecimal("20000000"))
                .stock(10)
                .reservedStock(2)
                .build();

        variantB = ProductVariant.builder()
                .id(2L)
                .productId(10L)
                .sku("PHONE-PRO-512")
                .price(new BigDecimal("25000000"))
                .stock(5)
                .reservedStock(0)
                .build();
    }

    @Nested
    @DisplayName("1. Giữ tồn kho tạm thời (reserveStock)")
    class ReserveStockTests {

        @Test
        @DisplayName("Giữ tồn kho thành công: trừ stock, tăng reservedStock và xóa cache")
        void reserveStock_Success() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(1L).sku("PHONE-PRO-256").quantity(3).build(),
                    StockReserveItem.builder().variantId(2L).sku("PHONE-PRO-512").quantity(2).build()
            );

            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));
            when(variantRepository.findById(2L)).thenReturn(Optional.of(variantB));

            boolean result = inventoryService.reserveStock("ORDER-123", items);

            assertThat(result).isTrue();
            // Variant A: stock ban đầu 10 -> trừ 3 còn 7; reservedStock: 2 + 3 = 5
            assertThat(variantA.getStock()).isEqualTo(7);
            assertThat(variantA.getReservedStock()).isEqualTo(5);

            // Variant B: stock ban đầu 5 -> trừ 2 còn 3; reservedStock: 0 + 2 = 2
            assertThat(variantB.getStock()).isEqualTo(3);
            assertThat(variantB.getReservedStock()).isEqualTo(2);

            verify(cacheService, times(2)).evictProductCaches(10L);
        }

        @Test
        @DisplayName("Ném IllegalStateException khi số lượng yêu cầu vượt quá tồn kho (Không cho tồn kho âm)")
        void reserveStock_ThrowsWhenInsufficientStock() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(2L).sku("PHONE-PRO-512").quantity(6).build() // Chỉ có 5
            );

            when(variantRepository.findById(2L)).thenReturn(Optional.of(variantB));

            assertThatThrownBy(() -> inventoryService.reserveStock("ORDER-123", items))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("chỉ còn 5 trong kho");

            // Không bị trừ kho
            assertThat(variantB.getStock()).isEqualTo(5);
            assertThat(variantB.getReservedStock()).isEqualTo(0);
        }

        @Test
        @DisplayName("Ném BadRequestException khi số lượng đặt mua nhỏ hơn hoặc bằng 0")
        void reserveStock_ThrowsWhenQuantityInvalid() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(1L).quantity(0).build()
            );

            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));

            assertThatThrownBy(() -> inventoryService.reserveStock("ORDER-123", items))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số lượng đặt mua phải lớn hơn 0");
        }

        @Test
        @DisplayName("Ném IllegalStateException khi không tìm thấy biến thể theo ID/SKU")
        void reserveStock_ThrowsWhenVariantNotFound() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(999L).sku("UNKNOWN-SKU").quantity(1).build()
            );

            when(variantRepository.findById(999L)).thenReturn(Optional.empty());
            when(variantRepository.findBySku("UNKNOWN-SKU")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.reserveStock("ORDER-123", items))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Không tìm thấy biến thể");
        }
    }

    @Nested
    @DisplayName("2. Giải phóng & Xác nhận hoàn tất kho (releaseStock & commitStock)")
    class ReleaseAndCommitStockTests {

        @Test
        @DisplayName("Giải phóng tồn kho: hoàn trả stock và giảm reservedStock an toàn không để âm")
        void releaseStock_RestoresStockAndReducesReserved() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(1L).quantity(2).build()
            );

            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));

            inventoryService.releaseStock("ORDER-123", items);

            // Variant A: stock ban đầu 10 -> hoàn 2 = 12; reserved ban đầu 2 -> giảm 2 = 0
            assertThat(variantA.getStock()).isEqualTo(12);
            assertThat(variantA.getReservedStock()).isEqualTo(0);
            verify(cacheService).evictProductCaches(10L);
        }

        @Test
        @DisplayName("Xác nhận trừ kho sau thanh toán thành công: giảm reservedStock")
        void commitStock_ReducesReservedStock() {
            List<StockReserveItem> items = List.of(
                    StockReserveItem.builder().variantId(1L).quantity(2).build()
            );

            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));

            inventoryService.commitStock("ORDER-123", items);

            // Reserved stock giảm từ 2 xuống 0
            assertThat(variantA.getReservedStock()).isEqualTo(0);
            verify(variantRepository).save(variantA);
        }
    }

    @Nested
    @DisplayName("3. Điều chỉnh kho & kiểm tra tồn khả dụng")
    class StockAdjustmentTests {

        @Test
        @DisplayName("Điều chỉnh tồn kho bằng adjustStock thành công")
        void adjustStock_Success() {
            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

            ProductVariant updated = inventoryService.adjustStock(1L, 15);

            assertThat(updated.getStock()).isEqualTo(25); // 10 + 15
            verify(cacheService).evictProductCaches(10L);
        }

        @Test
        @DisplayName("Ném BadRequestException khi adjustStock làm tồn kho bị âm")
        void adjustStock_ThrowsWhenResultNegative() {
            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));

            assertThatThrownBy(() -> inventoryService.adjustStock(1L, -15)) // 10 - 15 = -5
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số lượng tồn kho không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Kiểm tra tồn kho khả dụng isStockAvailable chính xác")
        void isStockAvailable_ChecksCorrectly() {
            when(variantRepository.findById(1L)).thenReturn(Optional.of(variantA));

            assertThat(inventoryService.isStockAvailable(1L, 10)).isTrue();
            assertThat(inventoryService.isStockAvailable(1L, 11)).isFalse();
        }
    }
}

