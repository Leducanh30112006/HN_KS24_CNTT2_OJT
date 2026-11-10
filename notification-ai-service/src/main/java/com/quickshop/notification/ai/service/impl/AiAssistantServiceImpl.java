package com.quickshop.notification.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
import com.quickshop.notification.ai.dto.PromptVerifyDto;
import com.quickshop.notification.ai.guardrail.PromptGuardService;
import com.quickshop.notification.ai.model.AiShoppingIntent;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import com.quickshop.notification.ai.model.PromptSafetyResult;
import com.quickshop.notification.ai.prompt.SystemPromptBuilder;
import com.quickshop.notification.ai.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final PromptGuardService promptGuardService;
    private final SystemPromptBuilder systemPromptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:mock-gemini-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        long startTime = System.currentTimeMillis();
        String rawMessage = request.getMessage() != null ? request.getMessage() : "";

        // 1. Thẩm định bảo mật an toàn Prompt Injection qua Guardrail
        PromptSafetyResult safetyResult = promptGuardService.inspectPrompt(rawMessage);

        // Trường hợp 1: Prompt bị Chặn (BLOCKED) do tấn công nghiêm trọng
        if (safetyResult.getRiskLevel() == PromptRiskLevel.BLOCKED) {
            log.warn("Blocking potential prompt injection attempt: user={}, message='{}'", request.getUserId(), rawMessage);
            return AiChatResponse.builder()
                    .reply("Xin lỗi quý khách! Yêu cầu của bạn chứa các cụm từ hoặc cấu trúc không được phép theo Tiêu chuẩn An toàn & Bảo mật của QuickShop AI Assistant. Vui lòng đặt các câu hỏi liên quan đến sản phẩm, voucher hoặc đơn hàng mua sắm.")
                    .sanitizedPrompt("")
                    .riskLevel(PromptRiskLevel.BLOCKED)
                    .intent(AiShoppingIntent.SECURITY_VIOLATION)
                    .suggestedQuestions(List.of(
                            "Tư vấn điện thoại thông minh bán chạy nhất",
                            "Xem các mã giảm giá và voucher hôm nay",
                            "Chính sách bảo hành và đổi trả hàng"
                    ))
                    .fallbackUsed(true)
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
                    .build();
        }

        String sanitizedPrompt = safetyResult.getSanitizedPrompt();

        // Trường hợp 2: Kiểm tra Scope Limiting (Nằm ngoài phạm vi e-commerce)
        if (!promptGuardService.isShoppingScoped(sanitizedPrompt)) {
            log.info("Off-topic request filtered: '{}'", sanitizedPrompt);
            return AiChatResponse.builder()
                    .reply("QuickShop AI là trợ lý ảo chuyên trách tư vấn mua sắm, sản phẩm và dịch vụ trên sàn QuickShop. Mình xin phép không hỗ trợ các câu hỏi ngoài phạm vi thương mại điện tử. Bạn có muốn mình hỗ trợ tìm món đồ nào hôm nay không?")
                    .sanitizedPrompt(sanitizedPrompt)
                    .riskLevel(safetyResult.getRiskLevel())
                    .intent(AiShoppingIntent.OFF_TOPIC)
                    .suggestedQuestions(List.of(
                            "Gợi ý quà tặng sinh nhật bạn gái",
                            "Các mã giảm giá FreeShip Max",
                            "Top laptop cấu hình mạnh giá tốt"
                    ))
                    .fallbackUsed(true)
                    .latencyMs(System.currentTimeMillis() - startTime)
                    .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
                    .build();
        }

        // Trường hợp 3: Nhận diện Intent mua sắm
        AiShoppingIntent intent = detectIntent(sanitizedPrompt);
        List<String> suggestedQuestions = generateSuggestedQuestions(intent);

        // Trường hợp 4: Gọi Google Gemini API nếu có API key hợp lệ
        String reply = null;
        boolean fallbackUsed = false;

        if (isGeminiConfigured()) {
            reply = callGoogleGemini(sanitizedPrompt);
        }

        // Nếu Gemini không khả dụng hoặc chưa cấu hình key thật -> Fallback sang Smart Rule-based Engine
        if (reply == null || reply.isBlank()) {
            fallbackUsed = true;
            reply = generateSmartResponse(sanitizedPrompt, intent);
        }

        return AiChatResponse.builder()
                .reply(reply)
                .sanitizedPrompt(sanitizedPrompt)
                .riskLevel(safetyResult.getRiskLevel())
                .intent(intent)
                .suggestedQuestions(suggestedQuestions)
                .fallbackUsed(fallbackUsed)
                .latencyMs(System.currentTimeMillis() - startTime)
                .timestamp(LocalDateTime.now().format(ISO_FORMATTER))
                .build();
    }

    @Override
    public PromptVerifyDto.Response verifyPrompt(PromptVerifyDto.Request request) {
        PromptSafetyResult safety = promptGuardService.inspectPrompt(request.getPrompt());

        String explanation = switch (safety.getRiskLevel()) {
            case SAFE -> "Prompt hoàn toàn an toàn, nằm trong phạm vi tư vấn mua sắm QuickShop.";
            case SUSPICIOUS -> "Prompt chứa mẫu thao túng nhạy cảm đã được làm sạch an toàn bằng bộ lọc [FILTERED].";
            case BLOCKED -> "Prompt bị từ chối tuyệt đối do phát hiện hành vi tấn công Prompt Injection / Jailbreak / Exploit.";
        };

        return PromptVerifyDto.Response.builder()
                .originalPrompt(safety.getOriginalPrompt())
                .sanitizedPrompt(safety.getSanitizedPrompt())
                .riskLevel(safety.getRiskLevel())
                .allowed(safety.isSafe())
                .detectedThreats(safety.getDetectedThreats())
                .explanation(explanation)
                .inspectedAt(System.currentTimeMillis())
                .build();
    }

    @Override
    public Map<String, Object> getSafetyPolicy() {
        Map<String, Object> policy = new HashMap<>();
        policy.put("assistantName", SystemPromptBuilder.ASSISTANT_NAME);
        policy.put("assistantRole", SystemPromptBuilder.ASSISTANT_ROLE);
        policy.put("activeAiModel", isGeminiConfigured() ? geminiModel : "SmartRuleFallbackEngine");
        policy.put("provider", isGeminiConfigured() ? "Google Gemini AI" : "Local QuickShop Core");
        policy.put("maxPromptLength", 500);
        policy.put("supportedGuardrails", List.of(
                "Anti-Prompt Injection Pattern Matching",
                "Jailbreak & DAN Mode Detection",
                "XSS & SQL Injection Normalization",
                "Strict E-commerce Scope Limiting",
                "Unicode NFKC Normalization & Zero-width Filtering",
                "Inviolable System Prompt Secrecy"
        ));
        policy.put("supportedIntents", List.of(AiShoppingIntent.values()));
        return policy;
    }

    /**
     * Gọi Google Gemini AI v1beta generateContent API
     */
    private String callGoogleGemini(String sanitizedPrompt) {
        try {
            String uri = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, geminiModel, geminiApiKey);

            Map<String, Object> payload = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPromptBuilder.buildSystemPrompt()))
                    ),
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", sanitizedPrompt))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 800
                    )
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                        log.info("Successfully received response from Google Gemini AI (model: {})", geminiModel);
                        return textNode.asText().trim();
                    }
                }
            } else {
                log.warn("Google Gemini API returned status {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Google Gemini API call failed, falling back to smart rule engine: {}", e.getMessage());
        }
        return null;
    }

    private boolean isGeminiConfigured() {
        return geminiApiKey != null &&
                !geminiApiKey.isBlank() &&
                !geminiApiKey.equalsIgnoreCase("mock-gemini-key") &&
                !geminiApiKey.equalsIgnoreCase("mock-key");
    }

    private AiShoppingIntent detectIntent(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("voucher") || lower.contains("giảm giá") || lower.contains("khuyến mãi") || lower.contains("freeship") || lower.contains("ưu đãi")) {
            return AiShoppingIntent.PROMOTION_DISCOUNT;
        }
        if (lower.contains("đơn hàng") || lower.contains("giao hàng") || lower.contains("vận chuyển") || lower.contains("bao giờ nhận") || lower.contains("tracking")) {
            return AiShoppingIntent.ORDER_INQUIRY;
        }
        if (lower.contains("phí ship") || lower.contains("đổi trả") || lower.contains("hoàn tiền") || lower.contains("bảo hành") || lower.contains("chính sách")) {
            return AiShoppingIntent.SHIPPING_POLICY;
        }
        if (lower.contains("khiếu nại") || lower.contains("hỗ trợ") || lower.contains("hotline") || lower.contains("liên hệ") || lower.contains("tổng đài")) {
            return AiShoppingIntent.CUSTOMER_SUPPORT;
        }
        if (lower.contains("điện thoại") || lower.contains("phone") || lower.contains("laptop") || lower.contains("máy tính") ||
                lower.contains("tai nghe") || lower.contains("áo") || lower.contains("quần") || lower.contains("giày") || lower.contains("sản phẩm") || lower.contains("tư vấn") || lower.contains("mua")) {
            return AiShoppingIntent.PRODUCT_RECOMMENDATION;
        }
        if (lower.contains("chào") || lower.contains("hi") || lower.contains("hello") || lower.contains("bạn là ai")) {
            return AiShoppingIntent.GENERAL_GREETING;
        }
        return AiShoppingIntent.PRODUCT_RECOMMENDATION;
    }

    private String generateSmartResponse(String sanitizedPrompt, AiShoppingIntent intent) {
        String lower = sanitizedPrompt.toLowerCase();

        // 1. Tư vấn điện thoại
        if (lower.contains("điện thoại") || lower.contains("phone")) {
            return "Chào bạn! Mình gợi ý bạn tham khảo dòng **QuickPhone Pro Max 256GB** tại gian hàng TechWorld Official Store, hiện đang có sẵn màu Titan Tự Nhiên với giá ưu đãi cực tốt!";
        }

        // 2. Tư vấn laptop
        if (lower.contains("laptop") || lower.contains("máy tính")) {
            return "Bạn có thể tham khảo **QuickBook Ultra 14 inch M3 Pro** cấu hình 16GB/512GB siêu mượt cho lập trình viên và đồ họa nhé!";
        }

        // 3. Tư vấn Voucher & Khuyến mãi
        if (intent == AiShoppingIntent.PROMOTION_DISCOUNT) {
            return "Hiện tại QuickShop đang có các chương trình ưu đãi độc quyền:\n" +
                    "- **QUICKSHOP50K**: Giảm ngay 50.000đ cho đơn hàng từ 500k.\n" +
                    "- **FREESHIPMAX**: Giảm 25.000đ phí vận chuyển toàn quốc.\n" +
                    "- **MALL100K**: Giảm 100.000đ cho sản phẩm thuộc gian hàng chính hãng QuickShop Mall.\n" +
                    "Bạn chỉ cần chọn mã ngay tại bước thanh toán để áp dụng tự động nhé!";
        }

        // 4. Tra cứu đơn hàng & vận chuyển
        if (intent == AiShoppingIntent.ORDER_INQUIRY) {
            return "Để theo dõi tình trạng đơn hàng của bạn:\n" +
                    "1. Truy cập mục **Đơn Mua** trên thanh menu người dùng.\n" +
                    "2. Kiểm tra mã vận đơn tương ứng của các đối tác vận chuyển liên kết (GHN, GHTK, Viettel Post).\n" +
                    "Hệ thống QuickShop luôn cập nhật trạng thái đơn hàng thời gian thực qua Webhook!";
        }

        // 5. Chính sách đổi trả & bảo hành
        if (intent == AiShoppingIntent.SHIPPING_POLICY) {
            return "Chính sách mua sắm an tâm tại QuickShop:\n" +
                    "- **Đổi trả miễn phí 7 ngày**: Áp dụng cho các sản phẩm có lỗi từ nhà sản xuất hoặc không đúng mô tả.\n" +
                    "- **Bảo hành chính hãng 12-24 tháng** theo tiêu chuẩn của từng thương hiệu phân phối.\n" +
                    "- **Đảm bảo hoàn tiền 100%**: Tiền thanh toán sẽ được giữ tại Ví Ký Quỹ (Escrow) cho đến khi bạn xác nhận đã nhận hàng nguyên vẹn.";
        }

        // Phản hồi chào mừng mặc định
        return "Chào bạn! Tôi là Trợ lý Mua sắm QuickShop AI. Bạn cần tư vấn sản phẩm, kiểm tra khuyến mãi hay theo dõi đơn hàng hôm nay?";
    }

    private List<String> generateSuggestedQuestions(AiShoppingIntent intent) {
        return switch (intent) {
            case PRODUCT_RECOMMENDATION -> List.of(
                    "Xem đánh giá chi tiết của người mua",
                    "Sản phẩm này có được freeship không?",
                    "So sánh cấu hình và giá các phiên bản"
            );
            case PROMOTION_DISCOUNT -> List.of(
                    "Cách kết hợp voucher sàn và voucher shop",
                    "Điều kiện áp dụng mã MALL100K là gì?",
                    "Khi nào có đợt Flash Sale tiếp theo?"
            );
            case ORDER_INQUIRY -> List.of(
                    "Thời gian giao hàng tiêu chuẩn là mấy ngày?",
                    "Làm sao để đổi địa chỉ nhận hàng?",
                    "Liên hệ nhân viên giao hàng bằng cách nào?"
            );
            case SHIPPING_POLICY -> List.of(
                    "Quy trình yêu cầu trả hàng và hoàn tiền",
                    "Địa chỉ trung tâm bảo hành ở đâu?",
                    "Hàng đổi trả có mất phí ship không?"
            );
            default -> List.of(
                    "Gợi ý điện thoại chụp ảnh đẹp dưới 30 triệu",
                    "Top laptop mỏng nhẹ pin trâu cho sinh viên",
                    "Hôm nay có voucher giảm giá nào tốt nhất?"
            );
        };
    }
}
