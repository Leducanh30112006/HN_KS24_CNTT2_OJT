package com.quickshop.notification.service;

import com.quickshop.notification.dto.AiChatResponse;
import com.quickshop.notification.dto.AiSeoRequest;
import com.quickshop.notification.dto.AiSeoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AiAssistantService {

    public AiChatResponse processShoppingChat(String userInput) {
        String input = userInput != null ? userInput : "";
        String sanitizedPrompt = sanitizePrompt(input);
        log.info("Processing shopping consultation query: {}", sanitizedPrompt);

        String reply;
        List<String> suggestions = new ArrayList<>();
        String lower = sanitizedPrompt.toLowerCase();

        if (lower.contains("điện thoại") || lower.contains("phone") || lower.contains("iphone")) {
            reply = "Chào bạn! Nếu bạn đang tìm smartphone cao cấp, mình gợi ý siêu phẩm **QuickPhone Pro Max 256GB** (màu Titan Tự Nhiên) với chip AI 3nm và camera 48MP zoom 5x cực đỉnh tại gian hàng TechWorld Official Store. Hiện đang có mã giảm ngay 500.000đ và FreeShip Xtra toàn quốc!";
            suggestions = Arrays.asList("Xem chi tiết QuickPhone Pro Max", "Có những màu nào?", "Chính sách bảo hành ra sao?");
        } else if (lower.contains("laptop") || lower.contains("máy tính") || lower.contains("macbook")) {
            reply = "Dành cho công việc và lập trình chuyên nghiệp, bạn hãy tham khảo **QuickBook Ultra 14 inch M3 Pro** (18GB RAM / 512GB SSD). Máy mỏng nhẹ, pin 18 tiếng liên tục và màn hình Liquid Retina XDR 120Hz sắc nét chuẩn màu đồ họa.";
            suggestions = Arrays.asList("Xem giá QuickBook Ultra", "Có hỗ trợ trả góp không?", "Phụ kiện đi kèm gồm những gì?");
        } else if (lower.contains("tai nghe") || lower.contains("pods") || lower.contains("âm thanh")) {
            reply = "Bạn có thể trải nghiệm **Tai nghe True Wireless QuickPods Pro 2** với công nghệ Chống ồn chủ động ANC thế hệ mới, kháng nước IP54 và âm thanh vòm Spatial Audio cực sống động.";
            suggestions = Arrays.asList("Giá tai nghe QuickPods Pro 2", "Thời lượng pin bao lâu?", "Khám phá danh mục Điện tử");
        } else if (lower.contains("thời trang") || lower.contains("áo") || lower.contains("quần") || lower.contains("polo")) {
            reply = "Về thời trang, mẫu **Áo Polo Nam QuickStyle Premium** dệt từ sợi cafe sinh học kháng khuẩn, chống tia UV 50+ và chống nhăn đang là Best-Seller tuần này với giá chỉ 289.000đ!";
            suggestions = Arrays.asList("Xem bảng size áo Polo", "Xem thêm sản phẩm Thời trang", "Mã giảm giá cho đơn đầu tiên");
        } else if (lower.contains("mã giảm giá") || lower.contains("voucher") || lower.contains("khuyến mãi")) {
            reply = "Hôm nay QuickShop có các ưu đãi cực hot: \n• Mã **FREESHIPMAX**: Miễn phí vận chuyển toàn quốc cho đơn từ 99K\n• Mã **QUICKSHOP50K**: Giảm 50.000đ cho đơn từ 300K\n• Mã **AIASSISTANT**: Giảm thêm 5% tối đa 100K khi mua sản phẩm công nghệ!";
            suggestions = Arrays.asList("Áp dụng mã giảm giá", "Săn Flash Sale hôm nay", "Top sản phẩm bán chạy");
        } else if (lower.contains("giao hàng") || lower.contains("vận chuyển") || lower.contains("ship")) {
            reply = "QuickShop liên kết trực tiếp với các đơn vị vận chuyển hàng đầu GHN, GHTK, Viettel Post. Giao hàng Hỏa tốc 2 giờ nội thành Hà Nội & TP.HCM, và 1-2 ngày đối với các tỉnh thành khác. Mọi đơn hàng đều được cập nhật hành trình tự động theo thời gian thực!";
            suggestions = Arrays.asList("Kiểm tra phí giao hàng", "Chính sách đồng kiểm khi nhận", "Theo dõi đơn hàng của tôi");
        } else {
            reply = "Chào bạn! Tôi là Trợ lý Mua sắm QuickShop AI thông minh. Tôi có thể tư vấn chọn sản phẩm, so sánh cấu hình thông số, cập nhật mã giảm giá và hỗ trợ thông tin đơn hàng 24/7. Bạn đang quan tâm đến ngành hàng nào hôm nay?";
            suggestions = Arrays.asList("Gợi ý điện thoại dưới 30 triệu", "Laptop cho sinh viên CNTT", "Mã giảm giá hôm nay", "Sản phẩm Flash Sale");
        }

        return AiChatResponse.builder()
                .reply(reply)
                .originalPrompt(userInput)
                .sanitizedPrompt(sanitizedPrompt)
                .suggestions(suggestions)
                .build();
    }

    public AiSeoResponse generateSeoContent(AiSeoRequest req) {
        String name = req.getProductName() != null && !req.getProductName().isBlank() ? req.getProductName().trim() : "Sản phẩm QuickShop";
        String cat = req.getCategory() != null ? req.getCategory().trim() : "Thời trang & Đời sống";
        String kw = req.getKeywords() != null ? req.getKeywords().trim() : "chính hãng, giá tốt, cao cấp";

        String optimizedTitle = String.format("[CHÍNH HÃNG 100%%] %s - Chuẩn Xuất Khẩu Cao Cấp, Bảo Hành Đổi Mới 12 Tháng", name);

        List<String> highlights = Arrays.asList(
                "Thiết kế hiện đại, tinh xảo theo xu hướng mới nhất 2026",
                "Chất liệu cao cấp, độ bền vượt trội và an toàn tuyệt đối cho người dùng",
                "Tiết kiệm điện năng / tối ưu hiệu suất sử dụng hàng ngày",
                "Được kiểm duyệt chất lượng nghiêm ngặt trước khi xuất xưởng",
                "Cam kết hoàn tiền 200% nếu phát hiện hàng giả, hàng nhái"
        );

        String metaDescription = String.format("Mua ngay %s chính hãng giá tốt nhất tại QuickShop Marketplace. Miễn phí vận chuyển toàn quốc, đổi trả 7 ngày linh hoạt và bảo hành chính hãng.", name);

        List<String> tags = Arrays.asList(
                name.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-"),
                cat.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", "-"),
                "quickshop-official",
                "hang-chinh-hang",
                "freeship-xtra"
        );

        String fullDescription = String.format(
                "### GIỚI THIỆU SẢN PHẨM: %s\n\n" +
                "Sản phẩm **%s** thuộc ngành hàng **%s** là sự lựa chọn hoàn hảo mang đến trải nghiệm tuyệt vời cho bạn. Với thiết kế tinh xảo và công nghệ tiên tiến, sản phẩm đáp ứng hoàn hảo mọi nhu cầu sử dụng thực tế.\n\n" +
                "#### ĐẶC ĐIỂM NỔI BẬT:\n" +
                "- 100%% Hàng chính hãng phân phối tại QuickShop Mall.\n" +
                "- Tối ưu hóa hiệu năng, độ bền vượt trội so với các sản phẩm cùng phân khúc.\n" +
                "- Đóng gói chuẩn quy cách chống sốc an toàn tuyệt đối khi vận chuyển.\n\n" +
                "#### CHÍNH SÁCH BẢO HÀNH & ĐỔI TRẢ:\n" +
                "- Bảo hành chính hãng 12 tháng tại các trung tâm trên toàn quốc.\n" +
                "- Đổi mới miễn phí trong vòng 7 ngày nếu có lỗi từ nhà sản xuất.\n" +
                "- Hỗ trợ kỹ thuật và chăm sóc khách hàng 24/7.",
                name, name, cat
        );

        return AiSeoResponse.builder()
                .optimizedTitle(optimizedTitle)
                .highlights(highlights)
                .metaDescription(metaDescription)
                .tags(tags)
                .fullDescription(fullDescription)
                .build();
    }

    public String sanitizePrompt(String input) {
        if (input == null) return "";
        // FR-10: Loại bỏ Prompt Injection, SQL Injection, Script Injection
        String cleaned = input.replaceAll("(?i)(ignore previous instructions|drop database|system prompt|reveal secrets|delete from|exec\\(|eval\\(|<script>)", "[FILTERED]");
        return cleaned.trim();
    }
}
