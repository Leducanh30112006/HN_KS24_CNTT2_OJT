package com.quickshop.order.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.order.config.TestIntegrationConfig;
import com.quickshop.order.entity.Voucher;
import com.quickshop.order.entity.enums.VoucherDiscountType;
import com.quickshop.order.entity.enums.VoucherStatus;
import com.quickshop.order.repository.MasterOrderRepository;
import com.quickshop.order.repository.SubOrderRepository;
import com.quickshop.order.repository.VoucherRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8083",
                "spring.main.allow-bean-definition-overriding=true",
                "catalog.service.mock-enabled=true",
                "catalog.service.url=http://localhost:8082"
        }
)
@ActiveProfiles("test")
@Import(TestIntegrationConfig.class)
@DisplayName("QuickShop Order Service - Live HTTP Server for k6 Load Testing")
public class OrderServiceLoadServerRunner {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private MasterOrderRepository masterOrderRepository;

    @Autowired
    private SubOrderRepository subOrderRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Khởi động Order Service trên port 8083 và giữ server chạy cho k6 bắn tải")
    void runServerForLoadTesting() throws InterruptedException {
        log.info("================================================================================");
        log.info("  QUICKSHOP ORDER SERVICE - LOAD TESTING HTTP SERVER STARTED ON PORT 8083");
        log.info("================================================================================");

        // 1. Dọn dẹp store in-memory
        TestIntegrationConfig.clearAll();

        // 2. Chuẩn bị dữ liệu kiểm thử (Vouchers, Catalog variants)
        seedTestData();

        log.info(">> Đã nạp thành công Voucher kiểm thử & Mock Catalog Data.");
        log.info(">> Sẵn sàng tiếp nhận lưu lượng k6 (Mục tiêu: 1.000 RPS).");
        log.info(">> Server sẽ chạy trong 10 phút (600s) để thực hiện các bài test tải...");

        // Chỉ giữ server chạy nếu có cờ -Dquickshop.loadtest.server=true
        if (!Boolean.parseBoolean(System.getProperty("quickshop.loadtest.server", "false"))) {
            log.info(">> Bỏ qua giữ server vì không kích hoạt cờ -Dquickshop.loadtest.server=true.");
            return;
        }

        // Giữ server chạy tối đa 10 phút hoặc cho đến khi bài kiểm tra kết thúc
        long startTime = System.currentTimeMillis();
        long maxDurationMs = 600_000L; // 10 minutes

        while (System.currentTimeMillis() - startTime < maxDurationMs) {
            Thread.sleep(2000);
        }

        log.info("Order Service Load Test Server kết thúc sau 10 phút chạy.");
    }

    private void seedTestData() {
        voucherRepository.deleteAll();

        // 1. Voucher toàn sàn (shop_id = null)
        Voucher platformVoucher1 = Voucher.builder()
                .code("QUICKSHOP10")
                .shopId(null)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00")) // 10%
                .minOrderValue(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher platformVoucher2 = Voucher.builder()
                .code("FREESHIP_MAX")
                .shopId(null)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("30000.00"))
                .minOrderValue(new BigDecimal("150000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher platformVoucher3 = Voucher.builder()
                .code("QUICKSHOP50K")
                .shopId(null)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderValue(new BigDecimal("300000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        // 2. Voucher riêng của từng Shop
        Voucher shop1Voucher1 = Voucher.builder()
                .code("SHOP1_SALE10")
                .shopId(1L)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderValue(new BigDecimal("50000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher shop1Voucher2 = Voucher.builder()
                .code("SHOP1_VOUCHER20K")
                .shopId(1L)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("20000.00"))
                .minOrderValue(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("20000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher shop2Voucher1 = Voucher.builder()
                .code("SHOP2_SALE15")
                .shopId(2L)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("15.00"))
                .minOrderValue(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher shop2Voucher2 = Voucher.builder()
                .code("SHOP2_VOUCHER30K")
                .shopId(2L)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("30000.00"))
                .minOrderValue(new BigDecimal("200000.00"))
                .maxDiscountAmount(new BigDecimal("30000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher shop3Voucher1 = Voucher.builder()
                .code("TECH_SALE10")
                .shopId(3L)
                .discountType(VoucherDiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .minOrderValue(new BigDecimal("100000.00"))
                .maxDiscountAmount(new BigDecimal("40000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        Voucher shop3Voucher2 = Voucher.builder()
                .code("GADGET_50K")
                .shopId(3L)
                .discountType(VoucherDiscountType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000.00"))
                .minOrderValue(new BigDecimal("250000.00"))
                .maxDiscountAmount(new BigDecimal("50000.00"))
                .usageLimit(1_000_000)
                .usedCount(0)
                .startDate(LocalDateTime.now().minusDays(30))
                .endDate(LocalDateTime.now().plusYears(2))
                .status(VoucherStatus.ACTIVE)
                .build();

        voucherRepository.saveAll(List.of(
                platformVoucher1, platformVoucher2, platformVoucher3,
                shop1Voucher1, shop1Voucher2,
                shop2Voucher1, shop2Voucher2,
                shop3Voucher1, shop3Voucher2
        ));
    }
}
