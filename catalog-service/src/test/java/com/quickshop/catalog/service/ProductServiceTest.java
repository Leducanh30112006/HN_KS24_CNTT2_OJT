package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.*;
import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.exception.ForbiddenException;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Quản lý CRUD Sản phẩm theo Shop, Vòng đời Trạng thái & Phân quyền")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private CatalogCacheService cacheService;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private ProductVariant sampleVariant;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(100L)
                .shopId(10L)
                .categoryId(1L)
                .name("Bàn phím cơ không dây QuickKey")
                .description("Bàn phím cao cấp 3 mode kết nối")
                .mainImage("https://example.com/keyboard.jpg")
                .status(ProductStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleVariant = ProductVariant.builder()
                .id(200L)
                .productId(100L)
                .sku("KEY-75-WHITE")
                .color("Trắng Retro")
                .size("75%")
                .price(new BigDecimal("1200000"))
                .stock(50)
                .reservedStock(5)
                .build();
    }

    @Nested
    @DisplayName("1. Đăng bán sản phẩm (Create Product)")
    class CreateProductTests {

        @Test
        @DisplayName("Người bán (ROLE_SELLER) tạo sản phẩm mặc định trạng thái PENDING")
        void createProduct_AsSeller_DefaultsToPending() {
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("Tai nghe chống ồn QuickSound")
                    .description("Tai nghe Bluetooth chống ồn chủ động")
                    .mainImage("https://example.com/headphone.jpg")
                    .isDraft(false)
                    .variants(List.of(
                            VariantCreateRequest.builder()
                                    .sku("HEAD-ANC-BLK")
                                    .color("Đen")
                                    .size("Over-ear")
                                    .price(new BigDecimal("990000"))
                                    .stock(30)
                                    .build()
                    ))
                    .build();

            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(101L);
                return p;
            });
            when(variantRepository.findByProductId(101L)).thenReturn(List.of());
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().name("Điện tử").build()));

            ProductDto result = productService.createProduct(request, 10L, "ROLE_SELLER");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(101L);
            assertThat(result.getShopId()).isEqualTo(10L);
            assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING);
            verify(cacheService).evictProductCaches(101L);
        }

        @Test
        @DisplayName("Người bán tạo sản phẩm lưu nháp (isDraft = true) nhận trạng thái DRAFT")
        void createProduct_AsDraft_SetsStatusDraft() {
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("Sản phẩm đang soạn nháp")
                    .isDraft(true)
                    .build();

            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(102L);
                return p;
            });
            when(variantRepository.findByProductId(102L)).thenReturn(List.of());

            ProductDto result = productService.createProduct(request, 10L, "ROLE_SELLER");

            assertThat(result.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("Người mua (ROLE_BUYER) cố gắng tạo sản phẩm bị ném ForbiddenException 403")
        void createProduct_AsBuyer_ThrowsForbidden() {
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .categoryId(1L)
                    .name("Sp gian lận")
                    .build();

            assertThatThrownBy(() -> productService.createProduct(request, 99L, "ROLE_BUYER"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Chỉ người bán (ROLE_SELLER) hoặc quản trị viên (ROLE_ADMIN)");
        }
    }

    @Nested
    @DisplayName("2. Cập nhật sản phẩm & Phân quyền theo Shop (Update Product)")
    class UpdateProductTests {

        @Test
        @DisplayName("Chính chủ Shop cập nhật thông tin thành công")
        void updateProduct_ByOwnerShop_Success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("Bàn phím cơ không dây QuickKey V2")
                    .build();

            ProductDto updated = productService.updateProduct(100L, request, 10L, "ROLE_SELLER");

            assertThat(updated).isNotNull();
            assertThat(sampleProduct.getName()).isEqualTo("Bàn phím cơ không dây QuickKey V2");
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Shop khác cố tình cập nhật sản phẩm không thuộc sở hữu bị từ chối 403 Forbidden")
        void updateProduct_ByDifferentShop_ThrowsForbidden() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("Cố ý thay đổi giá và tên")
                    .build();

            // Shop 99 cố sửa sản phẩm của Shop 10
            assertThatThrownBy(() -> productService.updateProduct(100L, request, 99L, "ROLE_SELLER"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Bạn không có quyền chỉnh sửa sản phẩm thuộc gian hàng khác");
        }

        @Test
        @DisplayName("Quản trị viên (ROLE_ADMIN) có thể cập nhật sản phẩm của bất kỳ Shop nào")
        void updateProduct_ByAdmin_SuccessEvenDifferentShop() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("Tên đã được Admin chuẩn hóa")
                    .build();

            ProductDto updated = productService.updateProduct(100L, request, 999L, "ROLE_ADMIN");

            assertThat(updated).isNotNull();
            assertThat(sampleProduct.getName()).isEqualTo("Tên đã được Admin chuẩn hóa");
        }
    }

    @Nested
    @DisplayName("3. Xóa sản phẩm theo Shop (Delete Product)")
    class DeleteProductTests {

        @Test
        @DisplayName("Chính chủ Shop xóa sản phẩm thành công")
        void deleteProduct_ByOwnerShop_Success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            productService.deleteProduct(100L, 10L, "ROLE_SELLER");

            verify(variantRepository).deleteByProductId(100L);
            verify(productRepository).delete(sampleProduct);
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Shop khác cố tình xóa sản phẩm bị ném 403 Forbidden")
        void deleteProduct_ByOtherShop_ThrowsForbidden() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.deleteProduct(100L, 99L, "ROLE_SELLER"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Bạn không có quyền xóa sản phẩm thuộc gian hàng khác");

            verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        @DisplayName("Admin có thể xóa sản phẩm vi phạm của bất kỳ shop nào")
        void deleteProduct_ByAdmin_Success() {
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            productService.deleteProduct(100L, 888L, "ROLE_ADMIN");

            verify(productRepository).delete(sampleProduct);
        }
    }

    @Nested
    @DisplayName("4. Luồng Nộp duyệt & Kiểm duyệt sản phẩm (Submit & Moderation)")
    class WorkflowAndModerationTests {

        @Test
        @DisplayName("Người bán nộp duyệt sản phẩm DRAFT chuyển sang trạng thái PENDING")
        void submitForApproval_FromDraft_TransitionsToPending() {
            sampleProduct.setStatus(ProductStatus.DRAFT);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto result = productService.submitForApproval(100L, 10L, "ROLE_SELLER");

            assertThat(result.getStatus()).isEqualTo(ProductStatus.PENDING);
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Nộp duyệt sản phẩm đã APPROVED ném BadRequestException 400")
        void submitForApproval_AlreadyApproved_ThrowsBadRequest() {
            sampleProduct.setStatus(ProductStatus.APPROVED);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            assertThatThrownBy(() -> productService.submitForApproval(100L, 10L, "ROLE_SELLER"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Sản phẩm đã được phê duyệt");
        }

        @Test
        @DisplayName("Admin phê duyệt sản phẩm PENDING sang APPROVED thành công")
        void updateProductStatus_ByAdmin_ApprovesProduct() {
            sampleProduct.setStatus(ProductStatus.PENDING);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto result = productService.updateProductStatus(100L, ProductStatus.APPROVED, null, "ROLE_ADMIN");

            assertThat(result.getStatus()).isEqualTo(ProductStatus.APPROVED);
            assertThat(sampleProduct.getRejectionReason()).isNull();
            verify(cacheService).evictProductCaches(100L);
        }

        @Test
        @DisplayName("Admin từ chối sản phẩm PENDING kèm lý do chuyển sang REJECTED")
        void updateProductStatus_ByAdmin_RejectsWithReason() {
            sampleProduct.setStatus(ProductStatus.PENDING);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto result = productService.updateProductStatus(
                    100L, ProductStatus.REJECTED, "Ảnh mờ, thông tin chưa rõ ràng", "ROLE_ADMIN");

            assertThat(result.getStatus()).isEqualTo(ProductStatus.REJECTED);
            assertThat(sampleProduct.getRejectionReason()).isEqualTo("Ảnh mờ, thông tin chưa rõ ràng");
        }

        @Test
        @DisplayName("Admin từ chối sản phẩm nhưng thiếu lý do ném BadRequestException 400")
        void updateProductStatus_RejectWithoutReason_ThrowsBadRequest() {
            assertThatThrownBy(() -> productService.updateProductStatus(100L, ProductStatus.REJECTED, "", "ROLE_ADMIN"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Vui lòng cung cấp lý do từ chối");
        }

        @Test
        @DisplayName("Người không phải Admin cố kiểm duyệt sản phẩm bị ném 403 Forbidden")
        void updateProductStatus_BySeller_ThrowsForbidden() {
            assertThatThrownBy(() -> productService.updateProductStatus(100L, ProductStatus.APPROVED, null, "ROLE_SELLER"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Chỉ Quản trị viên hệ thống (ROLE_ADMIN)");
        }
    }

    @Nested
    @DisplayName("5. Quyền xem chi tiết sản phẩm theo trạng thái")
    class ViewProductDetailsAccessControlTests {

        @Test
        @DisplayName("Sản phẩm đã APPROVED: Khách mua hàng công khai xem được")
        void getProductById_Approved_PublicCanView() {
            sampleProduct.setStatus(ProductStatus.APPROVED);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto dto = productService.getProductById(100L, null, null);

            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Sản phẩm DRAFT: Người ngoài cố xem bị ném 403 Forbidden")
        void getProductById_Draft_StrangerCannotView() {
            sampleProduct.setStatus(ProductStatus.DRAFT);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            // Người dùng vãng lai hoặc shop khác (shop 99)
            assertThatThrownBy(() -> productService.getProductById(100L, 99L, "ROLE_SELLER"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Bạn không có quyền truy cập sản phẩm chưa được công khai");
        }

        @Test
        @DisplayName("Sản phẩm DRAFT: Chính chủ Shop sở hữu xem được")
        void getProductById_Draft_OwnerShopCanView() {
            sampleProduct.setStatus(ProductStatus.DRAFT);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto dto = productService.getProductById(100L, 10L, "ROLE_SELLER");

            assertThat(dto).isNotNull();
            assertThat(dto.getStatus()).isEqualTo(ProductStatus.DRAFT);
        }

        @Test
        @DisplayName("Sản phẩm PENDING: Admin xem được để kiểm duyệt")
        void getProductById_Pending_AdminCanView() {
            sampleProduct.setStatus(ProductStatus.PENDING);
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(100L)).thenReturn(List.of(sampleVariant));

            ProductDto dto = productService.getProductById(100L, 999L, "ROLE_ADMIN");

            assertThat(dto).isNotNull();
            assertThat(dto.getStatus()).isEqualTo(ProductStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("6. API Xác thực biến thể cho Order Service")
    class OrderServiceContractValidationTests {

        @Test
        @DisplayName("Tra cứu biến thể SKU thành công tính toán đúng availableStock = stock - reservedStock")
        void getVariantValidation_Success() {
            when(variantRepository.findById(200L)).thenReturn(Optional.of(sampleVariant));
            when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

            CatalogVariantValidationDto validation = productService.getVariantValidation(200L);

            assertThat(validation).isNotNull();
            assertThat(validation.getVariantId()).isEqualTo(200L);
            assertThat(validation.getProductId()).isEqualTo(100L);
            assertThat(validation.getShopId()).isEqualTo(10L);
            assertThat(validation.getStock()).isEqualTo(50);
            assertThat(validation.getReservedStock()).isEqualTo(5);
            assertThat(validation.getAvailableStock()).isEqualTo(45); // 50 - 5 = 45
            assertThat(validation.isActive()).isTrue();
        }

        @Test
        @DisplayName("Tra cứu biến thể không tồn tại ném ResourceNotFoundException 404")
        void getVariantValidation_NotFound() {
            when(variantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getVariantValidation(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
