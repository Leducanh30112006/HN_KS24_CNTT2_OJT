package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductVariantService - Quản lý SKU, biến thể, giá bán, tồn kho thực tế và reserved_stock")
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogCacheService cacheService;

    @InjectMocks
    private ProductVariantService variantService;

    private Product sampleProduct;
    private ProductVariant variant1;
    private ProductVariant variant2;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(100L)
                .shopId(1L)
                .categoryId(2L)
                .name("Bàn phím cơ QuickKey Pro")
                .description("Bàn phím cơ cao cấp switch quang học")
                .status(ProductStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        variant1 = ProductVariant.builder()
                .id(201L)
                .productId(100L)
                .sku("QK-PRO-RED-75")
                .color("Đỏ Nhám")
                .size("75%")
                .price(new BigDecimal("1500000"))
                .stock(50)
                .reservedStock(5)
                .build();

        variant2 = ProductVariant.builder()
                .id(202L)
                .productId(100L)
                .sku("QK-PRO-BLUE-75")
                .color("Xanh Biển")
                .size("75%")
                .price(new BigDecimal("1550000"))
                .stock(30)
                .reservedStock(0)
                .build();
    }

    @Nested
    @DisplayName("1. Truy vấn danh sách & chi tiết biến thể")
    class QueryVariantTests {

        @Test
        @DisplayName("Lấy danh sách biến thể theo Product ID thành công")
        void getVariantsByProductId_Success() {
            when(productRepository.existsById(100L)).thenReturn(true);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(variant1, variant2));

            List<VariantDto> result = variantService.getVariantsByProductId(100L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSku()).isEqualTo("QK-PRO-RED-75");
            assertThat(result.get(0).getAvailableStock()).isEqualTo(45); // 50 - 5
            assertThat(result.get(1).getAvailableStock()).isEqualTo(30); // 30 - 0
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException nếu Product ID không tồn tại")
        void getVariantsByProductId_ThrowsWhenProductNotFound() {
            when(productRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> variantService.getVariantsByProductId(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Lấy chi tiết biến thể theo ID thành công")
        void getVariantById_Success() {
            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));

            VariantDto result = variantService.getVariantById(201L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(201L);
            assertThat(result.getPrice()).isEqualByComparingTo("1500000");
        }

        @Test
        @DisplayName("Tra cứu biến thể theo mã SKU thành công")
        void getVariantBySku_Success() {
            when(variantRepository.findBySku("QK-PRO-RED-75")).thenReturn(Optional.of(variant1));

            VariantDto result = variantService.getVariantBySku("QK-PRO-RED-75");

            assertThat(result).isNotNull();
            assertThat(result.getSku()).isEqualTo("QK-PRO-RED-75");
        }
    }

    @Nested
    @DisplayName("2. Thêm biến thể SKU mới (addVariant)")
    class AddVariantTests {

        @Test
        @DisplayName("Thêm biến thể SKU mới thành công và xóa cache Redis của sản phẩm")
        void addVariant_Success() {
            VariantCreateRequest request = VariantCreateRequest.builder()
                    .sku("QK-PRO-BROWN-75")
                    .color("Nâu Trầm")
                    .size("75%")
                    .price(new BigDecimal("1600000"))
                    .stock(40)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("QK-PRO-BROWN-75")).thenReturn(false);
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
                ProductVariant v = invocation.getArgument(0);
                v.setId(203L);
                return v;
            });

            VariantDto result = variantService.addVariant(100L, request, 1L, "ROLE_SELLER");

            assertThat(result).isNotNull();
            assertThat(result.getSku()).isEqualTo("QK-PRO-BROWN-75");
            assertThat(result.getStock()).isEqualTo(40);
            assertThat(result.getReservedStock()).isEqualTo(0);
            assertThat(result.getAvailableStock()).isEqualTo(40);
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Ném BadRequestException khi thêm biến thể có giá âm")
        void addVariant_ThrowsWhenNegativePrice() {
            VariantCreateRequest request = VariantCreateRequest.builder()
                    .sku("QK-PRO-BROWN-75")
                    .price(new BigDecimal("-1000"))
                    .stock(10)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.addVariant(100L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Giá bán không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Ném BadRequestException khi thêm biến thể có tồn kho âm")
        void addVariant_ThrowsWhenNegativeStock() {
            VariantCreateRequest request = VariantCreateRequest.builder()
                    .sku("QK-PRO-BROWN-75")
                    .price(new BigDecimal("100000"))
                    .stock(-5)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.addVariant(100L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số lượng tồn kho không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Ném BadRequestException khi mã SKU đã tồn tại trên hệ thống")
        void addVariant_ThrowsWhenDuplicateSku() {
            VariantCreateRequest request = VariantCreateRequest.builder()
                    .sku("QK-PRO-RED-75")
                    .price(new BigDecimal("1500000"))
                    .stock(10)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("QK-PRO-RED-75")).thenReturn(true);

            assertThatThrownBy(() -> variantService.addVariant(100L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("đã tồn tại trên hệ thống");
        }

        @Test
        @DisplayName("Ném ForbiddenException khi người bán khác cố thêm biến thể vào Shop này")
        void addVariant_ThrowsWhenUnauthorizedShop() {
            VariantCreateRequest request = VariantCreateRequest.builder()
                    .sku("NEW-SKU")
                    .price(new BigDecimal("100000"))
                    .stock(10)
                    .build();

            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.addVariant(100L, request, 999L, "ROLE_SELLER"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("3. Cập nhật biến thể & điều chỉnh giá/kho (Update, Price, Stock)")
    class UpdateVariantTests {

        @Test
        @DisplayName("Cập nhật thông tin biến thể thành công")
        void updateVariant_Success() {
            VariantUpdateRequest request = VariantUpdateRequest.builder()
                    .sku("QK-PRO-RED-75-V2")
                    .color("Đỏ Nhám Cao Cấp")
                    .size("75% Wireless")
                    .price(new BigDecimal("1650000"))
                    .stock(60)
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findBySku("QK-PRO-RED-75-V2")).thenReturn(Optional.empty());
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

            VariantDto result = variantService.updateVariant(201L, request, 1L, "ROLE_SELLER");

            assertThat(result.getSku()).isEqualTo("QK-PRO-RED-75-V2");
            assertThat(result.getColor()).isEqualTo("Đỏ Nhám Cao Cấp");
            assertThat(result.getPrice()).isEqualByComparingTo("1650000");
            assertThat(result.getStock()).isEqualTo(60);
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Ném BadRequestException khi cập nhật giá âm")
        void updateVariant_ThrowsWhenNegativePrice() {
            VariantUpdateRequest request = VariantUpdateRequest.builder()
                    .price(new BigDecimal("-50000"))
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.updateVariant(201L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Giá bán không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Ném BadRequestException khi cập nhật tồn kho âm")
        void updateVariant_ThrowsWhenNegativeStock() {
            VariantUpdateRequest request = VariantUpdateRequest.builder()
                    .stock(-10)
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.updateVariant(201L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số lượng tồn kho không được nhỏ hơn 0");
        }

        @Test
        @DisplayName("Cập nhật giá bán biến thể độc lập thành công")
        void updatePrice_Success() {
            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

            VariantDto result = variantService.updatePrice(201L, new BigDecimal("1700000"), 1L, "ROLE_SELLER");

            assertThat(result.getPrice()).isEqualByComparingTo("1700000");
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Điều chỉnh tồn kho bằng số lượng tuyệt đối (newStock) thành công")
        void updateStock_WithNewStock_Success() {
            VariantStockUpdateRequest request = VariantStockUpdateRequest.builder()
                    .newStock(120)
                    .reason("Nhập thêm 70 chiếc từ kho")
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

            VariantDto result = variantService.updateStock(201L, request, 1L, "ROLE_SELLER");

            assertThat(result.getStock()).isEqualTo(120);
            assertThat(result.getAvailableStock()).isEqualTo(115); // 120 - 5
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Điều chỉnh tồn kho bằng số lượng tương đối (+/-) thành công")
        void updateStock_WithAdjustment_Success() {
            VariantStockUpdateRequest request = VariantStockUpdateRequest.builder()
                    .quantityAdjustment(-10) // Tồn ban đầu 50 -> còn 40
                    .reason("Xuất mẫu thử")
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(i -> i.getArgument(0));

            VariantDto result = variantService.updateStock(201L, request, 1L, "ROLE_SELLER");

            assertThat(result.getStock()).isEqualTo(40);
        }

        @Test
        @DisplayName("Ném BadRequestException khi điều chỉnh trừ kho dẫn đến tồn kho âm")
        void updateStock_ThrowsWhenResultingStockNegative() {
            VariantStockUpdateRequest request = VariantStockUpdateRequest.builder()
                    .quantityAdjustment(-60) // Tồn hiện tại 50 -> 50 - 60 = -10 (âm)
                    .reason("Giảm quá mức")
                    .build();

            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> variantService.updateStock(201L, request, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Số lượng tồn kho không được nhỏ hơn 0");
        }
    }

    @Nested
    @DisplayName("4. Xóa biến thể SKU (deleteVariant)")
    class DeleteVariantTests {

        @Test
        @DisplayName("Xóa biến thể thành công khi sản phẩm còn nhiều hơn 1 biến thể và không có hàng giữ chỗ")
        void deleteVariant_Success() {
            List<ProductVariant> variants = new ArrayList<>(List.of(variant1, variant2));

            when(variantRepository.findById(202L)).thenReturn(Optional.of(variant2));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(variants);

            variantService.deleteVariant(202L, 1L, "ROLE_SELLER");

            verify(variantRepository).delete(variant2);
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Ném BadRequestException khi cố xóa biến thể duy nhất còn lại của sản phẩm")
        void deleteVariant_ThrowsWhenLastVariant() {
            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(variant1)); // Chỉ có 1

            assertThatThrownBy(() -> variantService.deleteVariant(201L, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Không thể xóa biến thể duy nhất");
        }

        @Test
        @DisplayName("Ném BadRequestException khi xóa biến thể đang có đơn giữ kho (reservedStock > 0)")
        void deleteVariant_ThrowsWhenReservedStockGreaterThanZero() {
            when(variantRepository.findById(201L)).thenReturn(Optional.of(variant1)); // variant1 có reservedStock = 5
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(variant1, variant2));

            assertThatThrownBy(() -> variantService.deleteVariant(201L, 1L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("sản phẩm được giữ kho trong đơn hàng");
        }
    }
}

