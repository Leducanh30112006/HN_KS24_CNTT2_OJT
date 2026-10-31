package com.quickshop.catalog.config;

import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogDataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialCatalogData() {
        if (categoryRepository.count() == 0) {
            Category electronics = categoryRepository.save(Category.builder().name("Điện tử & Công nghệ").slug("dien-tu-cong-nghe").build());
            Category fashion = categoryRepository.save(Category.builder().name("Thời trang nam nữ").slug("thoi-trang").build());
            Category home = categoryRepository.save(Category.builder().name("Nhà cửa & Đời sống").slug("nha-cua-doi-song").build());

            log.info("Initialized default categories");

            // Seed sample product for TechWorld Official Store (shopId = 1)
            Product phone = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(electronics.getId())
                    .name("Điện thoại thông minh QuickPhone Pro Max 256GB")
                    .description("Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình OLED 120Hz siêu nét và pin 5000mAh hỗ trợ sạc nhanh 100W.")
                    .mainImage("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(phone.getId())
                    .sku("QP-PRO-TITAN-256")
                    .color("Titan Tự Nhiên")
                    .size("256GB")
                    .price(new BigDecimal("24990000"))
                    .stock(50)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(phone.getId())
                    .sku("QP-PRO-BLACK-256")
                    .color("Đen Không Gian")
                    .size("256GB")
                    .price(new BigDecimal("24990000"))
                    .stock(30)
                    .reservedStock(0)
                    .build());

            Product laptop = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(electronics.getId())
                    .name("Laptop QuickBook Ultra 14 inch M3 Pro")
                    .description("Laptop mỏng nhẹ hiệu năng khủng dành cho kỹ sư phần mềm và nhà sáng tạo nội dung.")
                    .mainImage("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(laptop.getId())
                    .sku("QB-ULTRA-16-512")
                    .color("Bạc Ánh Kim")
                    .size("16GB/512GB")
                    .price(new BigDecimal("32990000"))
                    .stock(20)
                    .reservedStock(0)
                    .build());

            log.info("Initialized default products and variants for TechWorld Official Store");
        }
    }
}
