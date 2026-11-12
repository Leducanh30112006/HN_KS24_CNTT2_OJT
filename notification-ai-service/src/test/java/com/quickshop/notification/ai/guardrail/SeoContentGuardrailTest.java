package com.quickshop.notification.ai.guardrail;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.notification.ai.dto.AiSeoGenerateRequest;
import com.quickshop.notification.ai.guardrail.SeoContentGuardrail.PreparedSeoInput;
import com.quickshop.notification.ai.guardrail.SeoContentGuardrail.SanitizedSeoOutput;
import com.quickshop.notification.ai.guardrail.impl.PromptGuardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SeoContentGuardrailTest - Kiểm thử giới hạn prompt, lọc HTML/Markdown và kiểm định đầu ra SEO")
class SeoContentGuardrailTest {

    private SeoContentGuardrail guardrail;
    private PromptGuardService promptGuardService;

    @BeforeEach
    void setUp() {
        guardrail = new SeoContentGuardrail();
        promptGuardService = new PromptGuardServiceImpl();
    }

    @Test
    @DisplayName("1. Chuẩn bị prompt đầu vào từ thông số sản phẩm và cắt ngắn khi vượt giới hạn")
    void testPrepareAndLimitInput_TruncatesLongPrompt() {
        Map<String, String> specs = new LinkedHashMap<>();
        for (int i = 1; i <= 18; i++) {
            specs.put("Thông số kỹ thuật số " + i, "Giá trị chi tiết mô tả rất dài để vượt ngưỡng giới hạn ký tự prompt " + i);
        }

        AiSeoGenerateRequest req = AiSeoGenerateRequest.builder()
                .productName("Laptop QuickBook Ultra 14 Pro")
                .category("Laptop & Máy tính")
                .brand("QuickBook")
                .specifications(specs)
                .maxPromptLength(220)
                .build();

        PreparedSeoInput prepared = guardrail.prepareAndLimitInput(req, promptGuardService);

        assertTrue(prepared.isPromptTruncated());
        assertEquals(220, prepared.getMaxPromptLengthLimit());
        assertTrue(prepared.getBoundedPrompt().length() <= 220);
        assertEquals(SeoContentGuardrail.MAX_SPEC_ENTRIES, prepared.getSpecifications().size());
        assertFalse(prepared.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("2. Chặn Prompt Injection / XSS nghiêm trọng ngay ở bước kiểm tra đầu vào SEO")
    void testPrepareAndLimitInput_BlocksSevereInjection() {
        AiSeoGenerateRequest req = AiSeoGenerateRequest.builder()
                .productName("Áo Polo <script>alert('xss')</script>")
                .category("Thời trang")
                .build();

        assertThrows(BadRequestException.class,
                () -> guardrail.prepareAndLimitInput(req, promptGuardService));
    }

    @Test
    @DisplayName("3. Lọc sạch thẻ HTML độc hại, sự kiện JS và liên kết/ảnh Markdown không an toàn ở đầu ra")
    void testSanitizeOutput_StripsHtmlAndUnsafeMarkdown() {
        AiSeoGenerateRequest req = AiSeoGenerateRequest.builder()
                .productName("Tai nghe QuickPods Pro 2")
                .category("Âm thanh")
                .outputFormat("MARKDOWN")
                .build();
        PreparedSeoInput input = guardrail.prepareAndLimitInput(req, promptGuardService);

        String dirtyTitle = "<h1><b>Tai nghe QuickPods Pro 2</b></h1> **Chính hãng**";
        String dirtyDescription = "<script>alert(1)</script>"
                + "<h3>Giới thiệu</h3>"
                + "<p onclick=\"steal()\">Âm thanh chống ồn ANC vượt trội.</p>"
                + "![tracking](https://evil.example/pixel.png) "
                + "[Xem ngay](javascript:alert(1)) "
                + "### Điểm nổi bật:\n- Pin 30 giờ liên tục, sạc nhanh MagSafe tiện lợi suốt cả ngày.";

        SanitizedSeoOutput output = guardrail.sanitizeAndValidateOutput(
                dirtyTitle,
                dirtyDescription,
                "<b>Meta Title</b>",
                "<iframe src='bad'></iframe>Meta mô tả sản phẩm chuẩn SEO QuickShop chính hãng.",
                List.of("<b>tai nghe</b>", "QuickPods Pro 2"),
                input
        );

        assertTrue(output.isSanitized());
        assertTrue(output.isValid());
        assertFalse(output.getOptimizedTitle().contains("<"));
        assertFalse(output.getOptimizedTitle().contains("**"));
        assertFalse(output.getFullDescription().contains("<script"));
        assertFalse(output.getFullDescription().contains("<p"));
        assertFalse(output.getFullDescription().contains("javascript:"));
        assertFalse(output.getFullDescription().contains("![tracking]"));
        assertTrue(output.getFullDescription().contains("### Điểm nổi bật:"));
        assertTrue(output.getSlug().contains("tai-nghe-quickpods-pro-2"));
    }

    @Test
    @DisplayName("4. Chế độ PLAIN_TEXT phải loại bỏ toàn bộ ký pháp Markdown")
    void testSanitizeOutput_PlainTextStripsMarkdown() {
        AiSeoGenerateRequest req = AiSeoGenerateRequest.builder()
                .productName("Giày QuickRunner Pro")
                .category("Giày dép")
                .outputFormat("PLAIN_TEXT")
                .build();
        PreparedSeoInput input = guardrail.prepareAndLimitInput(req, promptGuardService);

        String markdownBody = "### ĐẶC ĐIỂM NỔI BẬT\n- **Đệm khí êm ái** giúp chạy bộ nhẹ nhàng và bảo vệ cổ chân bền bỉ suốt ngày dài.";
        SanitizedSeoOutput output = guardrail.sanitizeAndValidateOutput(
                "Giày **QuickRunner Pro**",
                markdownBody,
                null,
                null,
                null,
                input
        );

        assertTrue(output.isValid());
        assertFalse(output.getFullDescription().contains("###"));
        assertFalse(output.getFullDescription().contains("**"));
        assertTrue(output.getFullDescription().contains("Đệm khí êm ái"));
    }

    @Test
    @DisplayName("5. Kiểm tra đầu ra đánh dấu không hợp lệ nếu quá ngắn hoặc rò rỉ System Prompt")
    void testSanitizeOutput_DetectsInvalidOrLeakedOutput() {
        AiSeoGenerateRequest req = AiSeoGenerateRequest.builder()
                .productName("Bàn phím cơ")
                .build();
        PreparedSeoInput input = guardrail.prepareAndLimitInput(req, promptGuardService);

        SanitizedSeoOutput tooShort = guardrail.sanitizeAndValidateOutput("Bàn phím cơ", "Quá ngắn", null, null, null, input);
        assertFalse(tooShort.isValid());

        SanitizedSeoOutput leaked = guardrail.sanitizeAndValidateOutput(
                "Bàn phím cơ",
                "[ROLE & PERSONA] Đây là nội dung bị rò rỉ system_instruction dài đủ 80 ký tự trở lên để kiểm tra validator.",
                null,
                null,
                null,
                input
        );
        assertFalse(leaked.isValid());
    }
}
