package com.quickshop.catalog.config;

import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductReview;
import com.quickshop.catalog.entity.ProductStatus;
import com.quickshop.catalog.entity.ProductVariant;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.catalog.repository.ProductReviewRepository;
import com.quickshop.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogDataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductReviewRepository reviewRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedInitialCatalogData() {
        if (categoryRepository.count() == 0) {
            Category electronics = categoryRepository.save(Category.builder()
                    .name("Điện thoại & Phụ kiện")
                    .slug("dien-thoai-phu-kien")
                    .icon("Smartphone")
                    .build());

            Category laptop = categoryRepository.save(Category.builder()
                    .name("Laptop & Máy tính")
                    .slug("laptop-may-tinh")
                    .icon("Laptop")
                    .build());

            Category fashion = categoryRepository.save(Category.builder()
                    .name("Thời trang nam nữ")
                    .slug("thoi-trang")
                    .icon("Shirt")
                    .build());

            Category home = categoryRepository.save(Category.builder()
                    .name("Nhà cửa & Đời sống")
                    .slug("nha-cua-doi-song")
                    .icon("Home")
                    .build());

            Category beauty = categoryRepository.save(Category.builder()
                    .name("Mỹ phẩm & Làm đẹp")
                    .slug("my-pham-lam-dep")
                    .icon("Sparkles")
                    .build());

            Category shoes = categoryRepository.save(Category.builder()
                    .name("Giày dép & Túi xách")
                    .slug("giay-dep-tui-xach")
                    .icon("ShoppingBag")
                    .build());

            log.info("Initialized 6 marketplace categories");

            // 1. Điện thoại thông minh QuickPhone Pro Max
            Product phone = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(electronics.getId())
                    .name("Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế")
                    .description("Flagship hàng đầu với chip xử lý AI 3nm mạnh mẽ, màn hình Super Retina XDR OLED 120Hz siêu mượt mà, cụm 3 camera 48MP zoom quang học 5x và thời lượng pin trâu lên đến 29 giờ liên tục.")
                    .mainImage("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(phone.getId())
                    .sku("QP-PRO-TITAN-256")
                    .color("Titan Tự Nhiên")
                    .size("256GB")
                    .price(new BigDecimal("29990000"))
                    .stock(45)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(phone.getId())
                    .sku("QP-PRO-BLACK-512")
                    .color("Đen Không Gian")
                    .size("512GB")
                    .price(new BigDecimal("34990000"))
                    .stock(25)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(phone.getId())
                    .sku("QP-PRO-DESERT-1TB")
                    .color("Titan Sa Mạc")
                    .size("1TB")
                    .price(new BigDecimal("41990000"))
                    .stock(15)
                    .reservedStock(0)
                    .build());

            reviewRepository.save(ProductReview.builder()
                    .productId(phone.getId())
                    .subOrderId(101L)
                    .userId(2L)
                    .userName("Trần Minh Quân")
                    .rating(5)
                    .comment("Máy cầm rất đầm tay, màn hình 120Hz mượt mà không có độ trễ. Giao hàng nhanh bọc chống sốc cẩn thận!")
                    .build());

            reviewRepository.save(ProductReview.builder()
                    .productId(phone.getId())
                    .subOrderId(102L)
                    .userId(3L)
                    .userName("Nguyễn Thu Trang")
                    .rating(5)
                    .comment("Màu titan tự nhiên ở ngoài đẹp xuất sắc, chụp ảnh thiếu sáng cực kỳ sắc nét. 10 sao cho QuickShop!")
                    .build());

            // 2. Laptop QuickBook Ultra M3
            Product mac = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(laptop.getId())
                    .name("Laptop QuickBook Ultra 14 inch M3 Pro - 18GB/512GB")
                    .description("Chiếc máy trạm di động mỏng nhẹ đẳng cấp nhất: màn hình Liquid Retina XDR 1000 nits, chip 11-core CPU, 14-core GPU xử lý đồ họa mượt mà, pin dùng cả ngày không cần sạc.")
                    .mainImage("https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(mac.getId())
                    .sku("QB-ULTRA-SPACEBLACK-18-512")
                    .color("Đen Không Gian")
                    .size("18GB / 512GB")
                    .price(new BigDecimal("45990000"))
                    .stock(20)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(mac.getId())
                    .sku("QB-ULTRA-SILVER-36-1TB")
                    .color("Bạc Ánh Kim")
                    .size("36GB / 1TB")
                    .price(new BigDecimal("57990000"))
                    .stock(10)
                    .reservedStock(0)
                    .build());

            reviewRepository.save(ProductReview.builder()
                    .productId(mac.getId())
                    .subOrderId(103L)
                    .userId(4L)
                    .userName("Lê Hoàng Nam")
                    .rating(5)
                    .comment("Biên dịch code microservices Docker chạy êm ru không một tiếng quạt. Màn hình chuẩn màu đồ họa.")
                    .build());

            // 3. Tai nghe không dây chống ồn
            Product buds = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(electronics.getId())
                    .name("Tai nghe Bluetooth True Wireless QuickPods Pro 2 Chống Ồn Chủ Động")
                    .description("Âm thanh vòm cá nhân hóa chuẩn phòng thu, công nghệ khử tiếng ồn chủ động ANC thế hệ 2 gấp đôi hiệu quả, kháng nước IP54 và hộp sạc hỗ trợ sạc MagSafe không dây.")
                    .mainImage("https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(buds.getId())
                    .sku("QPODS-PRO-WHITE")
                    .color("Trắng Tinh Khôi")
                    .size("Tiêu chuẩn")
                    .price(new BigDecimal("4990000"))
                    .stock(80)
                    .reservedStock(0)
                    .build());

            reviewRepository.save(ProductReview.builder()
                    .productId(buds.getId())
                    .subOrderId(104L)
                    .userId(5L)
                    .userName("Vũ Đình Khoa")
                    .rating(5)
                    .comment("Chống ồn đi xe bus hay ngồi quán cafe yên tĩnh tuyệt đối, bass sâu tròn trịa.")
                    .build());

            // 4. Áo Polo Nam QuickStyle
            Product polo = productRepository.save(Product.builder()
                    .shopId(2L)
                    .categoryId(fashion.getId())
                    .name("Áo Polo Nam QuickStyle Premium Vải Cafe Khử Mùi Thoáng Khí")
                    .description("Chất liệu dệt sợi từ bã cafe tự nhiên siêu nhẹ, chống tia UV 50+, co giãn 4 chiều vận động thoải mái và chống nhăn tự nhiên không cần ủi.")
                    .mainImage("https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(polo.getId())
                    .sku("POLO-NAVY-M")
                    .color("Xanh Navy")
                    .size("M")
                    .price(new BigDecimal("289000"))
                    .stock(100)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(polo.getId())
                    .sku("POLO-NAVY-L")
                    .color("Xanh Navy")
                    .size("L")
                    .price(new BigDecimal("289000"))
                    .stock(85)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(polo.getId())
                    .sku("POLO-WHITE-M")
                    .color("Trắng Sữa")
                    .size("M")
                    .price(new BigDecimal("289000"))
                    .stock(70)
                    .reservedStock(0)
                    .build());

            reviewRepository.save(ProductReview.builder()
                    .productId(polo.getId())
                    .subOrderId(105L)
                    .userId(6L)
                    .userName("Đỗ Quốc Huy")
                    .rating(5)
                    .comment("Form áo đẹp, cổ đứng không bị quăn khi giặt máy. Vải mặc rất mát mẻ.")
                    .build());

            // 5. Robot Hút Bụi Lau Nhà Thông Minh
            Product robot = productRepository.save(Product.builder()
                    .shopId(2L)
                    .categoryId(home.getId())
                    .name("Robot Hút Bụi Lau Nhà Tự Động QuickBot Omni 3000Pa Cảm Biến LiDAR")
                    .description("Hệ thống định vị laser LDS 360 độ lập bản đồ 3D cực chuẩn, lực hút bão 3000Pa gom sạch lông thú cưng, tự động giặt giẻ lau sấy khí nóng diệt khuẩn 99.9%.")
                    .mainImage("https://images.unsplash.com/photo-1558317374-067fb5f30001?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(robot.getId())
                    .sku("ROBOT-OMNI-WHITE")
                    .color("Trắng Tuyết")
                    .size("Bản Quốc Tế")
                    .price(new BigDecimal("8490000"))
                    .stock(30)
                    .reservedStock(0)
                    .build());

            // 6. Serum Dưỡng Sáng Da & Mờ Thâm
            Product serum = productRepository.save(Product.builder()
                    .shopId(3L)
                    .categoryId(beauty.getId())
                    .name("Tinh Chất Serum Niacinamide 10% + Zinc 1% Dưỡng Sáng Đều Màu Da")
                    .description("Công thức lành tính dịu nhẹ kiểm soát bã nhờn, làm thông thoáng lỗ chân lông, làm mờ vết thâm mụn chỉ sau 14 ngày sử dụng đều đặn.")
                    .mainImage("https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(serum.getId())
                    .sku("SERUM-30ML")
                    .color("Trong suốt")
                    .size("30ml")
                    .price(new BigDecimal("199000"))
                    .stock(150)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(serum.getId())
                    .sku("SERUM-60ML")
                    .color("Trong suốt")
                    .size("60ml")
                    .price(new BigDecimal("349000"))
                    .stock(90)
                    .reservedStock(0)
                    .build());

            // 7. Giày Sneaker Thể Thao Cao Cấp
            Product sneaker = productRepository.save(Product.builder()
                    .shopId(3L)
                    .categoryId(shoes.getId())
                    .name("Giày Sneaker Thể Thao QuickRunner Pro Đệm Khí Êm Ái Chống Trượt")
                    .description("Đế đệm khí êm nhẹ trợ lực khi chạy bộ, thân giày dệt lưới Flyknit thoáng khí kháng khuẩn không lo hôi chân, thiết kế trẻ trung phong cách Hàn Quốc.")
                    .mainImage("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.ACTIVE)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(sneaker.getId())
                    .sku("SNEAKER-RED-40")
                    .color("Đỏ Rực Rỡ")
                    .size("40")
                    .price(new BigDecimal("890000"))
                    .stock(40)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(sneaker.getId())
                    .sku("SNEAKER-RED-41")
                    .color("Đỏ Rực Rỡ")
                    .size("41")
                    .price(new BigDecimal("890000"))
                    .stock(45)
                    .reservedStock(0)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(sneaker.getId())
                    .sku("SNEAKER-RED-42")
                    .color("Đỏ Rực Rỡ")
                    .size("42")
                    .price(new BigDecimal("890000"))
                    .stock(35)
                    .reservedStock(0)
                    .build());

            // 8. Sản phẩm chờ duyệt PENDING_APPROVAL (cho Seller & Admin Moderation)
            Product pendingItem = productRepository.save(Product.builder()
                    .shopId(1L)
                    .categoryId(electronics.getId())
                    .name("Bàn Phím Cơ Không Dây QuickKey 75% Switch Quang Học")
                    .description("Bàn phím cơ cao cấp 3 chế độ kết nối (Bluetooth 5.3, Wireless 2.4G, Type-C), keycap PBT doubleshot bền bỉ và pin 4000mAh dùng 2 tháng.")
                    .mainImage("https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80")
                    .status(ProductStatus.PENDING_APPROVAL)
                    .build());

            productVariantRepository.save(ProductVariant.builder()
                    .productId(pendingItem.getId())
                    .sku("KEY-75-WHITE")
                    .color("Trắng Retro")
                    .size("75% - 82 phím")
                    .price(new BigDecimal("1450000"))
                    .stock(50)
                    .reservedStock(0)
                    .build());

            log.info("Initialized 8 sample marketplace products across 6 categories with multi-variants and customer reviews");
        }
    }
}
