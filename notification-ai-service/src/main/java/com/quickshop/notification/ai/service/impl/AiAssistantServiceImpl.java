package com.quickshop.notification.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickshop.notification.ai.catalog.AiCatalogSearchRequest;
import com.quickshop.notification.ai.catalog.AiCatalogSearchResult;
import com.quickshop.notification.ai.catalog.AiCatalogSearchService;
import com.quickshop.notification.ai.dto.AiChatRequest;
import com.quickshop.notification.ai.dto.AiChatResponse;
import com.quickshop.notification.ai.dto.AiSeoGenerateRequest;
import com.quickshop.notification.ai.dto.AiSeoGenerateResponse;
import com.quickshop.notification.ai.dto.PromptVerifyDto;
import com.quickshop.notification.ai.guardrail.PromptGuardService;
import com.quickshop.notification.ai.guardrail.SeoContentGuardrail;
import com.quickshop.notification.ai.guardrail.SeoContentGuardrail.PreparedSeoInput;
import com.quickshop.notification.ai.guardrail.SeoContentGuardrail.SanitizedSeoOutput;
import com.quickshop.notification.ai.model.AiShoppingIntent;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import com.quickshop.notification.ai.model.PromptSafetyResult;
import com.quickshop.notification.ai.prompt.SystemPromptBuilder;
import com.quickshop.notification.ai.service.AiAssistantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final PromptGuardService promptGuardService;
    private final SystemPromptBuilder systemPromptBuilder;
    private final ObjectMapper objectMapper;
    private final AiCatalogSearchService aiCatalogSearchService;
    private final SeoContentGuardrail seoContentGuardrail;

    public AiAssistantServiceImpl(PromptGuardService promptGuardService,
                                  SystemPromptBuilder systemPromptBuilder,
                                  ObjectMapper objectMapper) {
        this(promptGuardService, systemPromptBuilder, objectMapper, null, new SeoContentGuardrail());
    }

    @Autowired
    public AiAssistantServiceImpl(PromptGuardService promptGuardService,
                                  SystemPromptBuilder systemPromptBuilder,
                                  ObjectMapper objectMapper,
                                  @Autowired(required = false) AiCatalogSearchService aiCatalogSearchService,
                                  @Autowired(required = false) SeoContentGuardrail seoContentGuardrail) {
        this.promptGuardService = promptGuardService;
        this.systemPromptBuilder = systemPromptBuilder;
        this.objectMapper = objectMapper;
        this.aiCatalogSearchService = aiCatalogSearchService;
        this.seoContentGuardrail = seoContentGuardrail != null ? seoContentGuardrail : new SeoContentGuardrail();
    }

    @Value("${gemini.api-key:mock-gemini-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.5-flash-lite}")
    private String geminiModel;

    @Value("${gemini.api-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(15))
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

        // Trường hợp 4: Truy xuất Catalog Snippets qua Function calling / Search Service (RAG)
        List<String> catalogSnippets = Collections.emptyList();
        if (aiCatalogSearchService != null && (intent == AiShoppingIntent.PRODUCT_RECOMMENDATION || isCatalogQuery(sanitizedPrompt))) {
            try {
                BigDecimal budgetMax = extractMaxPrice(sanitizedPrompt);
                AiCatalogSearchRequest searchReq = AiCatalogSearchRequest.builder()
                        .keyword(sanitizedPrompt)
                        .maxPrice(budgetMax)
                        .limit(5)
                        .userRole("ROLE_BUYER")
                        .build();
                AiCatalogSearchResult searchRes = aiCatalogSearchService.searchProducts(searchReq);
                if (searchRes != null && searchRes.getSnippets() != null && !searchRes.getSnippets().isEmpty()) {
                    catalogSnippets = searchRes.getSnippets();
                }
            } catch (Exception e) {
                log.warn("Catalog search encountered an exception, proceeding with static catalog: {}", e.getMessage());
            }
        }

        // Trường hợp 5: Gọi Google Gemini API nếu có API key hợp lệ
        String reply = null;
        boolean fallbackUsed = false;

        if (isGeminiConfigured()) {
            reply = callGoogleGemini(sanitizedPrompt, catalogSnippets);
        }

        // Nếu Gemini không khả dụng hoặc chưa cấu hình key thật -> Fallback sang Smart Rule-based Engine
        if (reply == null || reply.isBlank()) {
            fallbackUsed = true;
            reply = generateSmartResponse(sanitizedPrompt, intent, catalogSnippets);
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
    public AiSeoGenerateResponse generateSeoDescription(AiSeoGenerateRequest request) {
        long startTime = System.currentTimeMillis();
        PreparedSeoInput input = seoContentGuardrail.prepareAndLimitInput(request, promptGuardService);

        boolean fallbackUsed = false;
        String modelUsed = geminiModel;
        SanitizedSeoOutput output = null;

        if (isGeminiConfigured()) {
            String rawGeminiJson = callGoogleGeminiRaw(
                    systemPromptBuilder.buildSeoSystemPrompt(),
                    input.getBoundedPrompt(),
                    0.5,
                    900,
                    true
            );
            if (rawGeminiJson != null && !rawGeminiJson.isBlank()) {
                output = parseAndSanitizeGeminiSeo(rawGeminiJson, input);
            }
        }

        if (output == null || !output.isValid()) {
            fallbackUsed = true;
            modelUsed = "QuickShopSeoFallbackEngine";
            output = buildFallbackSeoOutput(input);
        }

        return AiSeoGenerateResponse.builder()
                .optimizedTitle(output.getOptimizedTitle())
                .fullDescription(output.getFullDescription())
                .metaTitle(output.getMetaTitle())
                .metaDescription(output.getMetaDescription())
                .slug(output.getSlug())
                .seoKeywords(output.getSeoKeywords())
                .outputFormat(input.getOutputFormat())
                .sanitized(output.isSanitized())
                .promptTruncated(input.isPromptTruncated())
                .promptLengthUsed(input.getBoundedPrompt().length())
                .maxPromptLengthLimit(input.getMaxPromptLengthLimit())
                .outputValid(output.isValid())
                .safetyWarnings(output.getWarnings())
                .fallbackUsed(fallbackUsed)
                .modelUsed(modelUsed)
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
        policy.put("maxSeoPromptLength", SeoContentGuardrail.HARD_MAX_PROMPT_LENGTH);
        policy.put("supportedGuardrails", List.of(
                "Anti-Prompt Injection Pattern Matching",
                "Jailbreak & DAN Mode Detection",
                "XSS & SQL Injection Normalization",
                "Strict E-commerce Scope Limiting",
                "Unicode NFKC Normalization & Zero-width Filtering",
                "Inviolable System Prompt Secrecy",
                "SEO Output Validation & HTML/Markdown Filtering"
        ));
        policy.put("supportedIntents", List.of(AiShoppingIntent.values()));
        return policy;
    }

    private String callGoogleGemini(String sanitizedPrompt, List<String> catalogSnippets) {
        String systemPromptText = (catalogSnippets != null && !catalogSnippets.isEmpty())
                ? systemPromptBuilder.buildContextualPrompt(sanitizedPrompt, catalogSnippets)
                : systemPromptBuilder.buildSystemPrompt();
        return callGoogleGeminiRaw(systemPromptText, sanitizedPrompt, 0.7, 800, false);
    }

    /**
     * Gọi Google Gemini AI v1beta generateContent API dùng chung cho Chat và SEO Generator
     */
    private String callGoogleGeminiRaw(String systemPromptText, String userPrompt, double temperature, int maxOutputTokens, boolean jsonMode) {
        try {
            String uri = String.format("%s/%s:generateContent?key=%s", geminiApiUrl, geminiModel, geminiApiKey);

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", maxOutputTokens);
            if (jsonMode) {
                generationConfig.put("responseMimeType", "application/json");
            }

            Map<String, Object> payload = Map.of(
                    "system_instruction", Map.of(
                            "parts", List.of(Map.of("text", systemPromptText))
                    ),
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", userPrompt))
                            )
                    ),
                    "generationConfig", generationConfig
            );

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(25))
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Google Gemini API call interrupted, falling back to local engine: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Google Gemini API call failed, falling back to local engine: {}", e.getMessage());
        }
        return null;
    }

    private String stripCodeFence(String rawText) {
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                int idx = 3;
                while (idx < trimmed.length() && Character.isLetter(trimmed.charAt(idx))) {
                    idx++;
                }
                trimmed = trimmed.substring(idx);
            }
            trimmed = trimmed.trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }

    private SanitizedSeoOutput parseAndSanitizeGeminiSeo(String rawJson, PreparedSeoInput input) {
        try {
            String trimmed = stripCodeFence(rawJson);
            JsonNode node = objectMapper.readTree(trimmed);
            String title = node.path("optimizedTitle").asText("");
            String metaTitle = node.path("metaTitle").asText("");
            String metaDesc = node.path("metaDescription").asText("");
            String fullDesc = node.path("fullDescription").asText("");

            List<String> aiKeywords = new java.util.ArrayList<>();
            JsonNode kwNode = node.path("seoKeywords");
            if (kwNode.isArray()) {
                kwNode.forEach(item -> {
                    if (item.isTextual() && !item.asText().isBlank()) {
                        aiKeywords.add(item.asText());
                    }
                });
            }
            return seoContentGuardrail.sanitizeAndValidateOutput(title, fullDesc, metaTitle, metaDesc, aiKeywords, input);
        } catch (Exception e) {
            log.warn("Failed to parse Gemini SEO JSON output, falling back: {}", e.getMessage());
            return null;
        }
    }

    private SanitizedSeoOutput buildFallbackSeoOutput(PreparedSeoInput input) {
        String title = buildFallbackSeoTitle(input);
        StringBuilder desc = new StringBuilder();

        String introHighlight = switch (input.getTone()) {
            case "TRE_TRUNG" -> "lựa chọn cực chất cho phong cách năng động và tiện lợi mỗi ngày";
            case "SANG_TRONG" -> "tuyệt tác thiết kế tinh xảo khẳng định đẳng cấp người sở hữu";
            default -> "giải pháp tối ưu kết hợp hiệu năng bền bỉ và thiết kế hiện đại";
        };

        desc.append("### GIỚI THIỆU SẢN PHẨM: ").append(input.getProductName()).append("\n\n");
        desc.append("**").append(input.getProductName()).append("** ");
        if (!input.getBrand().isBlank()) {
            desc.append("đến từ thương hiệu **").append(input.getBrand()).append("** ");
        }
        desc.append("thuộc ngành hàng **").append(input.getCategory()).append("** là ").append(introHighlight).append(".\n\n");

        if (!input.getSpecifications().isEmpty() || !input.getSpecificationsText().isBlank()) {
            desc.append("#### THÔNG SỐ KỸ THUẬT NỔI BẬT:\n");
            input.getSpecifications().forEach((k, v) -> desc.append("- **").append(k).append(":** ").append(v).append("\n"));
            if (!input.getSpecificationsText().isBlank()) {
                desc.append("- **Chi tiết cấu hình:** ").append(input.getSpecificationsText()).append("\n");
            }
            desc.append("\n");
        }

        desc.append("#### LÝ DO NÊN CHỌN MUA TẠI QUICKSHOP:\n");
        if (!input.getPrice().isBlank()) {
            desc.append("- **Giá ưu đãi tham khảo:** ").append(input.getPrice()).append("đ kèm nhiều voucher giảm giá theo đơn hàng.\n");
        }
        desc.append("- **Cam kết chính hãng 100%:** Kiểm định nguồn gốc rõ ràng, bảo hành minh bạch.\n");
        desc.append("- **Giao hàng nhanh toàn quốc:** Đóng gói chống sốc tiêu chuẩn, hỗ trợ đổi trả trong 7 ngày nếu có lỗi từ nhà sản xuất.\n");
        if (!input.getKeywords().isEmpty()) {
            desc.append("- **Từ khóa nổi bật:** ").append(String.join(", ", input.getKeywords())).append(".");
        }

        String metaTitle = input.getProductName() + " | Chính hãng QuickShop";
        String metaDesc = input.getProductName() + " (" + input.getCategory() + ") chính hãng giá tốt tại QuickShop, giao nhanh toàn quốc, bảo hành uy tín.";

        return seoContentGuardrail.sanitizeAndValidateOutput(title, desc.toString(), metaTitle, metaDesc, input.getKeywords(), input);
    }

    private String buildFallbackSeoTitle(PreparedSeoInput input) {
        StringBuilder sb = new StringBuilder("[Chính Hãng] ");
        sb.append(input.getProductName());
        if (!input.getBrand().isBlank() && !input.getProductName().toLowerCase().contains(input.getBrand().toLowerCase())) {
            sb.append(" - ").append(input.getBrand());
        }
        if (!input.getCategory().isBlank()) {
            sb.append(" | ").append(input.getCategory());
        }
        return sb.toString();
    }

    private boolean isGeminiConfigured() {
        return geminiApiKey != null &&
                !geminiApiKey.isBlank() &&
                !geminiApiKey.equalsIgnoreCase("mock-gemini-key") &&
                !geminiApiKey.equalsIgnoreCase("mock-key");
    }

    private AiShoppingIntent detectIntent(String input) {
        String lower = input.toLowerCase();

        // 1. Nhận diện các yêu cầu tìm kiếm, lọc sản phẩm theo ngân sách hoặc mức giá
        // Ví dụ: "hãy lọc cho tôi đơn hàng dưới 100k", "sản phẩm dưới 100k", "tìm đồ dưới 500k", "gợi ý hàng dưới 30 triệu"
        boolean hasPriceFilter = lower.matches(".*(dưới|nhỏ hơn|<|tầm|khoảng|giá|ngân sách)\\s*\\d+.*")
                || lower.matches(".*\\d+\\s*(k|tr|triệu|nghìn|ngàn|đ|vnd|đồng).*");
        boolean hasProductQuery = lower.contains("lọc") || lower.contains("tìm") || lower.contains("sản phẩm")
                || lower.contains("mặt hàng") || lower.contains("món đồ") || lower.contains("đồ") || lower.contains("hàng")
                || lower.contains("gợi ý") || lower.contains("tư vấn") || lower.contains("mua") || lower.contains("xem");

        if (hasPriceFilter && (hasProductQuery || lower.contains("đơn hàng"))) {
            return AiShoppingIntent.PRODUCT_RECOMMENDATION;
        }

        if (lower.contains("voucher") || lower.contains("giảm giá") || lower.contains("khuyến mãi") || lower.contains("freeship") || lower.contains("ưu đãi")) {
            return AiShoppingIntent.PROMOTION_DISCOUNT;
        }

        // 2. Tra cứu đơn hàng đã đặt (chỉ khi hỏi về tình trạng đơn hàng mua của mình)
        if (lower.contains("tra cứu đơn") || lower.contains("kiểm tra đơn") || lower.contains("tình trạng đơn")
                || lower.contains("đơn hàng của tôi") || lower.contains("mã vận đơn") || lower.contains("tracking")
                || lower.contains("bao giờ nhận") || lower.contains("giao đến đâu") || lower.contains("hủy đơn")
                || (lower.contains("vận chuyển") && !lower.contains("giá") && !lower.contains("phí"))
                || (lower.contains("đơn hàng") && (lower.contains("của tôi") || lower.contains("đã đặt") || lower.contains("ở đâu") || lower.contains("chưa nhận")))) {
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

    private boolean isCatalogQuery(String prompt) {
        if (prompt == null) return false;
        String lower = prompt.toLowerCase();
        return lower.contains("mua") || lower.contains("tìm") || lower.contains("giá") ||
                lower.contains("sản phẩm") || lower.contains("mã") || lower.contains("hàng") ||
                lower.contains("shop") || lower.contains("gợi ý") || lower.contains("tư vấn") ||
                lower.contains("điện thoại") || lower.contains("laptop") || lower.contains("quần") ||
                lower.contains("áo") || lower.contains("giày") || lower.contains("tai nghe") ||
                lower.contains("mỹ phẩm") || lower.contains("robot") || lower.contains("lọc") ||
                lower.contains("dưới") || lower.contains("khoảng") || lower.contains("tầm") ||
                lower.contains("k") || lower.contains("triệu");
    }

    private String generateSmartResponse(String sanitizedPrompt, AiShoppingIntent intent) {
        return generateSmartResponse(sanitizedPrompt, intent, Collections.emptyList());
    }

    private BigDecimal extractMaxPrice(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();

        // 1. Pattern k / nghìn / ngàn: e.g. "dưới 100k", "< 100k", "100k", "500 nghìn"
        java.util.regex.Matcher mK = java.util.regex.Pattern.compile("(?:dưới|<|nhỏ hơn|tầm|khoảng|giá)?\\s*(\\d+)\\s*(?:k|nghìn|ngàn)").matcher(lower);
        if (mK.find()) {
            try {
                long val = Long.parseLong(mK.group(1));
                return BigDecimal.valueOf(val * 1000L);
            } catch (Exception ignored) {}
        }

        // 2. Pattern tr / triệu: e.g. "dưới 30 triệu", "dưới 30tr", "10 triệu"
        java.util.regex.Matcher mTr = java.util.regex.Pattern.compile("(?:dưới|<|nhỏ hơn|tầm|khoảng|giá)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:tr|triệu)").matcher(lower);
        if (mTr.find()) {
            try {
                double val = Double.parseDouble(mTr.group(1).replace(",", "."));
                return BigDecimal.valueOf((long) (val * 1000000L));
            } catch (Exception ignored) {}
        }

        // 3. Pattern đồng / đ / vnd: e.g. "dưới 100.000", "dưới 100000đ"
        java.util.regex.Matcher mDong = java.util.regex.Pattern.compile("(?:dưới|<|nhỏ hơn|tầm|khoảng|giá)?\\s*(\\d{1,3}(?:\\.\\d{3})+|\\d+)\\s*(?:đ|vnd|đồng)?").matcher(lower);
        if (mDong.find()) {
            try {
                String numStr = mDong.group(1).replace(".", "");
                long val = Long.parseLong(numStr);
                if (val >= 1000) {
                    return BigDecimal.valueOf(val);
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "100.000đ";
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return nf.format(amount) + "đ";
    }

    private String generateSmartResponse(String sanitizedPrompt, AiShoppingIntent intent, List<String> catalogSnippets) {
        String lower = sanitizedPrompt.toLowerCase();

        // 0. Tư vấn sản phẩm theo ngân sách / Giá rẻ / Dưới 100k / Dưới 500k
        BigDecimal maxPrice = extractMaxPrice(lower);
        if (maxPrice != null && maxPrice.compareTo(new BigDecimal("150000")) <= 0) {
            return "Dưới đây là các sản phẩm và phụ kiện tiện ích giá tốt nhất dưới " + formatVnd(maxPrice) + " trên QuickShop:\n" +
                    "- **Cáp Sạc Nhanh Type-C to Type-C 60W Bọc Dù Chống Đứt**: Hỗ trợ sạc nhanh PD 60W, truyền dữ liệu tốc độ cao, bọc sợi nylon bện siêu bền - Giá chỉ **49.000đ**.\n" +
                    "- **Bộ 5 Đôi Tất Cổ Ngắn Cotton Khử Mùi Thoáng Khí**: Chất liệu cotton tự nhiên co giãn 4 chiều, kháng khuẩn khử mùi chân - Giá ưu đãi **59.000đ**.\n" +
                    "- **Sổ Tay Bìa Da A5 QuickNote 200 Trang Chống Lóa**: Giấy dầy dặn chống thấm mực, bìa dập vân sang trọng - Giá chỉ **75.000đ**.\n" +
                    "- **Bình Nước Thể Thao 800ml Nhựa Tritan Chống Tràn**: Nhựa an toàn BPA Free chịu nhiệt, có vạch đo lượng nước tiện lợi - Giá chỉ **89.000đ**.\n" +
                    "Bạn có thể bấm trực tiếp vào thẻ sản phẩm bên dưới để xem chi tiết hoặc thêm vào giỏ hàng ngay!";
        }

        if (maxPrice != null && maxPrice.compareTo(new BigDecimal("500000")) <= 0) {
            return "Dưới đây là các sản phẩm chất lượng trong tầm giá dưới " + formatVnd(maxPrice) + " tại QuickShop:\n" +
                    "- **Áo Thun Cotton Compact 100%**: Mát nhẹ thấm hút mồ hôi tối đa - Giá **189.000đ**.\n" +
                    "- **Tinh Chất Serum Niacinamide 10% + Zinc 1% Dưỡng Sáng Đều Màu Da**: Làm mờ vết thâm mụn, kiềm dầu dịu nhẹ - Giá **199.000đ**.\n" +
                    "- **Áo Polo Nam QuickStyle Premium Vải Cafe Khử Mùi Thoáng Khí**: Co giãn 4 chiều, chống nhăn chống tia UV - Giá chỉ **289.000đ**.\n" +
                    "- **Sách Tâm Lý Học Về Tiền (The Psychology of Money)**: Tái bản mới nhất - Giá chỉ **165.000đ**.";
        }

        if (maxPrice != null && maxPrice.compareTo(new BigDecimal("35000000")) <= 0 && (lower.contains("điện thoại") || lower.contains("phone") || lower.contains("smartphone"))) {
            return "Gợi ý điện thoại hàng đầu phân khúc dưới " + formatVnd(maxPrice) + " tại QuickShop:\n" +
                    "- **Điện thoại thông minh QuickPhone Pro Max 256GB - Bản Quốc Tế**: Flagship chip AI 3nm, màn hình OLED 120Hz, camera zoom 5x - Giá từ **29.990.000đ**.\n" +
                    "- **Samsung Galaxy S24 Ultra 256GB**: Khung titan, bút S-Pen quyền năng, camera 200MP - Giá từ **28.490.000đ**.\n" +
                    "Nhập mã **QUICKPHONE** để giảm ngay 500.000đ khi thanh toán!";
        }

        // 1. Tư vấn Quần áo & Thời trang
        if (lower.contains("quần") || lower.contains("áo") || lower.contains("thời trang") || lower.contains("mặc") || lower.contains("polo") || lower.contains("sơ mi") || lower.contains("váy") || lower.contains("jean")) {
            return "Các mẫu thời trang & quần áo bán chạy nhất trên QuickShop hiện nay:\n" +
                    "- **Áo Polo Nam QuickStyle Premium**: Vải dệt sợi bã cafe tự nhiên khử mùi, co giãn 4 chiều, chống nhăn và chống tia UV - Giá chỉ **289.000đ**.\n" +
                    "- **Quần Tây / Quần Jean QuickStyle**: Thiết kế dáng Slim-fit thanh lịch, thoải mái vận động - Giá từ **320.000đ**.\n" +
                    "- **Áo Thun Cotton Compact 100%**: Mát nhẹ thấm hút mồ hôi tối đa - Giá **189.000đ**.\n" +
                    "🎁 Đừng quên áp mã **FASHION20** để được giảm ngay 20% khi thanh toán bạn nhé!";
        }

        // 2. Tư vấn Giày dép & Phụ kiện
        if (lower.contains("giày") || lower.contains("dép") || lower.contains("sneaker") || lower.contains("túi")) {
            return "Gợi ý giày dép & túi xách hot nhất QuickShop:\n" +
                    "- **Giày Sneaker QuickRunner Pro**: Đế đệm khí êm nhẹ trợ lực, thân vải dệt Flyknit thoáng khí kháng khuẩn - Giá ưu đãi **890.000đ**.\n" +
                    "- **Túi xách & Balo chống nước**: Ngăn chống sốc bảo vệ laptop 14-16 inch - Giá từ **250.000đ**.";
        }

        // 3. Tư vấn Laptop
        if (lower.contains("laptop") || lower.contains("máy tính") || lower.contains("pc")) {
            if (lower.contains("sinh viên") || lower.contains("cntt") || lower.contains("lập trình") || lower.contains("code") || lower.contains("đồ họa")) {
                return "Dành riêng cho sinh viên CNTT, lập trình viên và thiết kế đồ họa:\n" +
                        "- **QuickBook Ultra 14 inch M3 Pro**: Chip 11-core CPU, 14-core GPU, RAM 18GB, SSD 512GB, màn hình Liquid Retina XDR 120Hz siêu nét. Cực mượt khi build code Docker, Android Studio, IntelliJ IDEA và Photoshop/Premiere - Giá: **45.990.000đ**.\n" +
                        "- Ngoài ra có dòng **QuickBook Air 16GB/512GB** mỏng nhẹ pin 18 tiếng, giá mềm chỉ từ **19.500.000đ**.\n" +
                        "Tất cả đều hỗ trợ trả góp 0% và tặng kèm balo chống sốc cao cấp!";
            }
            return "Bạn có thể tham khảo **QuickBook Ultra 14 inch M3 Pro** cấu hình 18GB/512GB siêu mượt cho lập trình viên và đồ họa nhé!";
        }

        // 4. Tư vấn Điện thoại & Tai nghe
        if (lower.contains("điện thoại") || lower.contains("phone") || lower.contains("smartphone") || lower.contains("tai nghe")) {
            return "Gợi ý thiết bị di động hàng đầu tại QuickShop:\n" +
                    "- **QuickPhone Pro Max 256GB**: Flagship chip AI 3nm, màn hình OLED 120Hz, camera zoom quang học 5x - Giá từ **29.990.000đ**.\n" +
                    "- **Tai nghe Bluetooth QuickPods Pro 2**: Khử tiếng ồn chủ động ANC thế hệ mới, pin 30h - Giá **4.990.000đ**.\n" +
                    "Nhập mã **QUICKPHONE** để giảm ngay 500.000đ tại bước thanh toán!";
        }

        // 5. Tư vấn Mỹ phẩm & Đời sống gia đình
        if (lower.contains("mỹ phẩm") || lower.contains("serum") || lower.contains("da") || lower.contains("robot") || lower.contains("hút bụi")) {
            return "Sản phẩm chăm sóc sắc đẹp & nhà cửa tiện ích:\n" +
                    "- **Serum Niacinamide 10% + Zinc 1%**: Làm mờ vết thâm, kiềm dầu, dưỡng sáng da sau 14 ngày - Giá **199.000đ**.\n" +
                    "- **Robot Hút Bụi Lau Nhà QuickBot Omni 3000Pa**: Cảm biến laser LiDAR 360 độ, tự động giặt sấy giẻ lau - Giá **8.490.000đ**.";
        }

        // 6. Tư vấn Voucher & Khuyến mãi
        if (intent == AiShoppingIntent.PROMOTION_DISCOUNT) {
            return "Hiện tại QuickShop đang có các chương trình ưu đãi độc quyền:\n" +
                    "- **QUICKSHOP50K**: Giảm ngay 50.000đ cho đơn hàng từ 500k.\n" +
                    "- **FREESHIPMAX**: Giảm 25.000đ phí vận chuyển toàn quốc.\n" +
                    "- **MALL100K**: Giảm 100.000đ cho sản phẩm thuộc gian hàng chính hãng QuickShop Mall.\n" +
                    "- **FASHION20**: Giảm 20% cho toàn bộ sản phẩm thời trang.\n" +
                    "Bạn chỉ cần chọn mã ngay tại bước thanh toán để áp dụng tự động nhé!";
        }

        // 7. Tra cứu đơn hàng & vận chuyển
        if (intent == AiShoppingIntent.ORDER_INQUIRY) {
            return "Để theo dõi tình trạng đơn hàng của bạn:\n" +
                    "1. Truy cập mục **Đơn Mua** trên thanh menu người dùng.\n" +
                    "2. Kiểm tra mã vận đơn tương ứng của các đối tác vận chuyển liên kết (GHN, GHTK, Viettel Post).\n" +
                    "Hệ thống QuickShop luôn cập nhật trạng thái đơn hàng thời gian thực qua Webhook!";
        }

        // 8. Chính sách đổi trả & bảo hành
        if (intent == AiShoppingIntent.SHIPPING_POLICY) {
            return "Chính sách mua sắm an tâm tại QuickShop:\n" +
                    "- **Đổi trả miễn phí 7 ngày**: Áp dụng cho các sản phẩm có lỗi từ nhà sản xuất hoặc không đúng mô tả.\n" +
                    "- **Bảo hành chính hãng 12-24 tháng** theo tiêu chuẩn của từng thương hiệu phân phối.\n" +
                    "- **Đảm bảo hoàn tiền 100%**: Tiền thanh toán sẽ được giữ tại Ví Ký Quỹ (Escrow) cho đến khi bạn xác nhận đã nhận hàng nguyên vẹn.";
        }

        // Nếu có catalogSnippets được tìm thấy từ Catalog Service / Fallback
        if (catalogSnippets != null && !catalogSnippets.isEmpty()) {
            StringBuilder sb = new StringBuilder("Dưới đây là một số sản phẩm nổi bật trên QuickShop phù hợp với nhu cầu của bạn:\n");
            for (String snippet : catalogSnippets) {
                sb.append("- ").append(snippet).append("\n");
            }
            sb.append("\nBạn cần mình tư vấn chi tiết hơn về thông số kỹ thuật hay ưu đãi của sản phẩm nào không?");
            return sb.toString();
        }

        // Phản hồi chào mừng và gợi ý các ngành hàng
        return "Chào bạn! Tôi là Trợ lý Mua sắm QuickShop AI. Bạn cần tư vấn sản phẩm (Điện thoại, Laptop, Quần áo thời trang, Mỹ phẩm), kiểm tra mã giảm giá hay theo dõi đơn hàng hôm nay?";
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
