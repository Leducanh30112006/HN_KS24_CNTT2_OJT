package com.quickshop.notification.ai.guardrail;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.notification.ai.dto.AiSeoGenerateRequest;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import com.quickshop.notification.ai.model.PromptSafetyResult;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kiểm soát an toàn prompt đầu vào và lọc HTML/Markdown, kiểm định chất lượng đầu ra
 * cho tính năng sinh mô tả sản phẩm chuẩn SEO (FR-11).
 */
@Component
public class SeoContentGuardrail {

    public static final int MIN_PROMPT_LENGTH = 100;
    public static final int DEFAULT_MAX_PROMPT_LENGTH = 800;
    public static final int HARD_MAX_PROMPT_LENGTH = 800;

    public static final int MIN_OUTPUT_LENGTH = 80;
    public static final int DEFAULT_MAX_OUTPUT_LENGTH = 1800;
    public static final int HARD_MAX_OUTPUT_LENGTH = 2500;

    public static final int MAX_SPEC_ENTRIES = 15;
    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_META_TITLE_LENGTH = 70;
    public static final int MAX_META_DESCRIPTION_LENGTH = 160;

    private static final Pattern DANGEROUS_BLOCK_HTML = Pattern.compile(
            "(?is)<(script|style|iframe|object|embed|svg|form)[^>]*>.*?</\\1\\s*>|<!--.*?-->"
    );
    private static final Pattern ANY_HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern DANGEROUS_URI_SCHEME = Pattern.compile("(?i)(javascript|vbscript|data\\s*:\\s*text/html)\\s*:");
    private static final Pattern INLINE_EVENT_HANDLER = Pattern.compile("(?i)\\bon[a-z]{2,20}\\s*=\\s*(['\"]).*?\\1");
    private static final Pattern MARKDOWN_IMAGE = Pattern.compile("!\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern CODE_FENCE = Pattern.compile("(?m)^\\s*```[a-zA-Z0-9_-]*\\s*$");
    private static final Pattern SYSTEM_LEAK_MARKER = Pattern.compile(
            "(?i)(\\[ROLE & PERSONA\\]|\\[CORE SECURITY|system_instruction|inviolable system prompt)"
    );

    public PreparedSeoInput prepareAndLimitInput(AiSeoGenerateRequest request, PromptGuardService promptGuardService) {
        List<String> warnings = new ArrayList<>();
        boolean[] sanitizedFlag = new boolean[]{false};

        String productName = validateAndCleanField("productName", request.getProductName(), promptGuardService, warnings, sanitizedFlag);
        if (productName.isBlank()) {
            throw new BadRequestException("Tên sản phẩm không được để trống sau khi làm sạch ký tự không hợp lệ.");
        }

        String category = validateAndCleanField("category", defaultIfBlank(request.getCategory(), "Sản phẩm QuickShop"),
                promptGuardService, warnings, sanitizedFlag);
        String brand = validateAndCleanField("brand", request.getBrand(), promptGuardService, warnings, sanitizedFlag);
        String price = validateAndCleanField("price", request.getPrice(), promptGuardService, warnings, sanitizedFlag);
        String specsText = validateAndCleanField("specificationsText", request.getSpecificationsText(),
                promptGuardService, warnings, sanitizedFlag);
        String keywordsRaw = validateAndCleanField("keywords", request.getKeywords(), promptGuardService, warnings, sanitizedFlag);
        String tone = normalizeTone(request.getTone());
        String outputFormat = normalizeOutputFormat(request.getOutputFormat(), warnings);

        Map<String, String> cleanedSpecs = new LinkedHashMap<>();
        if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
            int count = 0;
            for (Map.Entry<String, String> entry : request.getSpecifications().entrySet()) {
                if (count >= MAX_SPEC_ENTRIES) {
                    warnings.add("Danh sách thông số kỹ thuật vượt quá " + MAX_SPEC_ENTRIES + " mục và đã được giới hạn.");
                    break;
                }
                String cleanKey = validateAndCleanField("specKey", entry.getKey(), promptGuardService, warnings, sanitizedFlag);
                String cleanVal = validateAndCleanField("specValue", entry.getValue(), promptGuardService, warnings, sanitizedFlag);
                if (!cleanKey.isBlank() && !cleanVal.isBlank()) {
                    cleanedSpecs.put(truncateCleanly(cleanKey, 60), truncateCleanly(cleanVal, 160));
                    count++;
                }
            }
        }

        int promptLimit = resolveLimit(request.getMaxPromptLength(), MIN_PROMPT_LENGTH, HARD_MAX_PROMPT_LENGTH, DEFAULT_MAX_PROMPT_LENGTH,
                "Giới hạn prompt được chuẩn hóa trong khoảng [" + MIN_PROMPT_LENGTH + ", " + HARD_MAX_PROMPT_LENGTH + "] ký tự.", warnings);
        int outputLimit = resolveLimit(request.getMaxOutputLength(), 300, HARD_MAX_OUTPUT_LENGTH, DEFAULT_MAX_OUTPUT_LENGTH,
                null, warnings);

        String rawPrompt = buildSpecificationPrompt(productName, category, brand, price, cleanedSpecs, specsText, keywordsRaw, tone, outputFormat);
        boolean truncated = false;
        String boundedPrompt = rawPrompt;
        if (rawPrompt.length() > promptLimit) {
            boundedPrompt = truncateCleanly(rawPrompt, promptLimit);
            truncated = true;
            warnings.add("Prompt thông số đầu vào đã vượt ngưỡng " + promptLimit + " ký tự và được rút gọn an toàn.");
        }

        List<String> parsedKeywords = parseKeywords(keywordsRaw, productName, category);

        return PreparedSeoInput.builder()
                .productName(productName)
                .category(category)
                .brand(brand)
                .price(price)
                .specifications(cleanedSpecs)
                .specificationsText(specsText)
                .keywords(parsedKeywords)
                .tone(tone)
                .outputFormat(outputFormat)
                .maxPromptLengthLimit(promptLimit)
                .maxOutputLengthLimit(outputLimit)
                .boundedPrompt(boundedPrompt)
                .promptTruncated(truncated)
                .sanitized(sanitizedFlag[0])
                .warnings(warnings)
                .build();
    }

    public SanitizedSeoOutput sanitizeAndValidateOutput(
            String rawTitle,
            String rawDescription,
            String rawMetaTitle,
            String rawMetaDescription,
            List<String> rawKeywords,
            PreparedSeoInput input) {

        List<String> warnings = new ArrayList<>(input.getWarnings());
        boolean modified = input.isSanitized();

        String cleanTitle = stripMarkdownSyntax(stripHtmlUnsafe(rawTitle, warnings, () -> {}));
        if (rawTitle != null && !cleanTitle.equals(rawTitle.trim())) {
            modified = true;
        }
        cleanTitle = truncateCleanly(cleanTitle.replaceAll("[\\r\\n]+", " ").trim(), MAX_TITLE_LENGTH);

        boolean allowMarkdown = "MARKDOWN".equals(input.getOutputFormat());
        boolean[] descModified = new boolean[]{false};
        String cleanDesc = sanitizeDescriptionBody(rawDescription, allowMarkdown, warnings, () -> descModified[0] = true);
        if (descModified[0]) {
            modified = true;
        }
        if (cleanDesc.length() > input.getMaxOutputLengthLimit()) {
            cleanDesc = truncateCleanly(cleanDesc, input.getMaxOutputLengthLimit());
            warnings.add("Mô tả SEO đầu ra đã được giới hạn trong " + input.getMaxOutputLengthLimit() + " ký tự.");
        }

        boolean valid = isValidOutput(cleanTitle, cleanDesc, warnings);

        String cleanMetaTitle = stripMarkdownSyntax(stripHtmlUnsafe(
                rawMetaTitle != null && !rawMetaTitle.isBlank() ? rawMetaTitle : cleanTitle,
                warnings, () -> {}));
        cleanMetaTitle = truncateCleanly(cleanMetaTitle.replaceAll("[\\r\\n]+", " ").trim(), MAX_META_TITLE_LENGTH);

        String fallbackMetaSource = !cleanDesc.isBlank() ? stripMarkdownSyntax(cleanDesc) : cleanTitle;
        String cleanMetaDesc = stripMarkdownSyntax(stripHtmlUnsafe(
                rawMetaDescription != null && !rawMetaDescription.isBlank() ? rawMetaDescription : fallbackMetaSource,
                warnings, () -> {}));
        cleanMetaDesc = truncateCleanly(cleanMetaDesc.replaceAll("\\s+", " ").trim(), MAX_META_DESCRIPTION_LENGTH);

        Set<String> keywords = new LinkedHashSet<>();
        if (rawKeywords != null) {
            for (String kw : rawKeywords) {
                String cleanKw = stripMarkdownSyntax(stripHtmlUnsafe(kw, warnings, () -> {})).trim();
                if (!cleanKw.isBlank()) {
                    keywords.add(truncateCleanly(cleanKw, 50));
                }
            }
        }
        keywords.addAll(input.getKeywords());

        return SanitizedSeoOutput.builder()
                .optimizedTitle(cleanTitle)
                .fullDescription(cleanDesc)
                .metaTitle(cleanMetaTitle)
                .metaDescription(cleanMetaDesc)
                .slug(toSlug(cleanTitle.isBlank() ? input.getProductName() : cleanTitle))
                .seoKeywords(new ArrayList<>(keywords))
                .sanitized(modified)
                .valid(valid)
                .warnings(warnings)
                .build();
    }

    public String sanitizeDescriptionBody(String rawText, boolean allowSafeMarkdown, List<String> warnings, Runnable onModified) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String working = stripHtmlUnsafe(rawText, warnings, onModified);

        // Loại bỏ code fences (```)
        String withoutFences = CODE_FENCE.matcher(working).replaceAll("");
        if (!withoutFences.equals(working)) {
            onModified.run();
            warnings.add("Đã loại bỏ khối code fence Markdown không phù hợp trong mô tả sản phẩm.");
            working = withoutFences;
        }

        // Chặn Markdown image embed ![alt](url) để tránh chèn ảnh ngoài không kiểm duyệt
        Matcher imgMatcher = MARKDOWN_IMAGE.matcher(working);
        StringBuffer imgBuf = new StringBuffer();
        boolean imgFound = false;
        while (imgMatcher.find()) {
            imgFound = true;
            String alt = imgMatcher.group(1) != null ? imgMatcher.group(1).trim() : "";
            imgMatcher.appendReplacement(imgBuf, Matcher.quoteReplacement(alt));
        }
        imgMatcher.appendTail(imgBuf);
        if (imgFound) {
            onModified.run();
            warnings.add("Đã lọc bỏ thẻ ảnh Markdown ngoài luồng tải ảnh MinIO.");
            working = imgBuf.toString();
        }

        // Lọc các Markdown link có protocol nguy hiểm javascript:/data:/vbscript:
        Matcher linkMatcher = MARKDOWN_LINK.matcher(working);
        StringBuffer linkBuf = new StringBuffer();
        boolean unsafeLinkFound = false;
        while (linkMatcher.find()) {
            String label = linkMatcher.group(1);
            String target = linkMatcher.group(2) != null ? linkMatcher.group(2).trim() : "";
            if (DANGEROUS_URI_SCHEME.matcher(target).find() || !allowSafeMarkdown) {
                if (DANGEROUS_URI_SCHEME.matcher(target).find()) {
                    unsafeLinkFound = true;
                }
                linkMatcher.appendReplacement(linkBuf, Matcher.quoteReplacement(label));
            } else {
                linkMatcher.appendReplacement(linkBuf, Matcher.quoteReplacement("[" + label + "](" + target + ")"));
            }
        }
        linkMatcher.appendTail(linkBuf);
        if (unsafeLinkFound) {
            onModified.run();
            warnings.add("Đã loại bỏ liên kết Markdown chứa scheme không an toàn.");
        }
        working = linkBuf.toString();

        if (!allowSafeMarkdown) {
            String plain = stripMarkdownSyntax(working);
            if (!plain.equals(working)) {
                onModified.run();
                working = plain;
            }
        } else {
            // Chuẩn hóa heading quá sâu (##### -> ####)
            working = working.replaceAll("(?m)^#{5,}\\s+", "#### ");
        }

        working = working.replaceAll("\\r\\n?", "\n")
                .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        return working;
    }

    public String stripHtmlUnsafe(String rawText, List<String> warnings, Runnable onModified) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String result = rawText.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        String noDangerousBlocks = DANGEROUS_BLOCK_HTML.matcher(result).replaceAll(" ");
        if (!noDangerousBlocks.equals(result)) {
            onModified.run();
            if (warnings != null && !warnings.contains("Đã phát hiện và lọc bỏ thẻ HTML/script nguy hiểm.")) {
                warnings.add("Đã phát hiện và lọc bỏ thẻ HTML/script nguy hiểm.");
            }
            result = noDangerousBlocks;
        }

        String noEvents = INLINE_EVENT_HANDLER.matcher(result).replaceAll("");
        if (!noEvents.equals(result)) {
            onModified.run();
            result = noEvents;
        }

        String noSchemes = DANGEROUS_URI_SCHEME.matcher(result).replaceAll("");
        if (!noSchemes.equals(result)) {
            onModified.run();
            result = noSchemes;
        }

        String noTags = ANY_HTML_TAG.matcher(result).replaceAll(" ");
        if (!noTags.equals(result)) {
            onModified.run();
            if (warnings != null && !warnings.contains("Đã lọc bỏ thẻ HTML khỏi nội dung đầu ra.")) {
                warnings.add("Đã lọc bỏ thẻ HTML khỏi nội dung đầu ra.");
            }
            result = noTags;
        }

        return result.replaceAll("[ \\t]+", " ").trim();
    }

    public String stripMarkdownSyntax(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text;
        cleaned = MARKDOWN_IMAGE.matcher(cleaned).replaceAll("$1");
        cleaned = MARKDOWN_LINK.matcher(cleaned).replaceAll("$1");
        cleaned = cleaned.replaceAll("(?m)^\\s*#{1,6}\\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*>+\\s*", "");
        cleaned = cleaned.replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", "");
        cleaned = cleaned.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("__([^_]+)__", "$1");
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1");
        cleaned = cleaned.replaceAll("(?<!\\w)\\*([^*\\n]+)\\*(?!\\w)", "$1");
        cleaned = cleaned.replaceAll("[ \\t]+", " ")
                .replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
        return cleaned.trim();
    }

    public String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "san-pham-quickshop";
        }
        String replaced = input.replace('đ', 'd').replace('Đ', 'D');
        String normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "san-pham-quickshop" : truncateCleanly(normalized, 100).replaceAll("-+$", "");
    }

    public String truncateCleanly(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        int cut = trimmed.lastIndexOf(' ', maxLen);
        if (cut < maxLen / 2) {
            cut = maxLen;
        }
        return trimmed.substring(0, cut).trim();
    }

    private String validateAndCleanField(
            String fieldName,
            String value,
            PromptGuardService promptGuardService,
            List<String> warnings,
            boolean[] sanitizedFlag) {
        if (value == null || value.isBlank()) {
            return "";
        }

        PromptSafetyResult safety = promptGuardService.inspectPrompt(value);
        if (safety.getRiskLevel() == PromptRiskLevel.BLOCKED) {
            throw new BadRequestException("Trường '" + fieldName + "' chứa nội dung không an toàn hoặc vi phạm kiểm soát prompt: "
                    + String.join("; ", safety.getDetectedThreats()));
        }

        String cleaned = safety.getSanitizedPrompt();
        if (safety.getRiskLevel() == PromptRiskLevel.SUSPICIOUS) {
            sanitizedFlag[0] = true;
            warnings.add("Trường '" + fieldName + "' chứa cụm từ thao túng và đã được làm sạch.");
        }

        String htmlStripped = stripMarkdownSyntax(stripHtmlUnsafe(cleaned, warnings, () -> sanitizedFlag[0] = true));
        if (!htmlStripped.equals(cleaned)) {
            sanitizedFlag[0] = true;
        }
        return htmlStripped;
    }

    private boolean isValidOutput(String title, String description, List<String> warnings) {
        if (title == null || title.isBlank()) {
            warnings.add("Tiêu đề SEO đầu ra không hợp lệ (rỗng).");
            return false;
        }
        if (description == null || description.length() < MIN_OUTPUT_LENGTH) {
            warnings.add("Mô tả SEO đầu ra quá ngắn (< " + MIN_OUTPUT_LENGTH + " ký tự).");
            return false;
        }
        if (SYSTEM_LEAK_MARKER.matcher(description).find() || SYSTEM_LEAK_MARKER.matcher(title).find()) {
            warnings.add("Phát hiện dấu hiệu rò rỉ chỉ thị hệ thống trong đầu ra AI.");
            return false;
        }
        return true;
    }

    private String buildSpecificationPrompt(
            String productName,
            String category,
            String brand,
            String price,
            Map<String, String> specs,
            String specsText,
            String keywords,
            String tone,
            String outputFormat) {
        StringBuilder sb = new StringBuilder();
        sb.append("San pham: ").append(productName).append("\n");
        sb.append("Danh muc: ").append(category).append("\n");
        if (!brand.isBlank()) {
            sb.append("Thuong hieu: ").append(brand).append("\n");
        }
        if (!price.isBlank()) {
            sb.append("Gia tham khao: ").append(price).append("\n");
        }
        if (!specs.isEmpty()) {
            sb.append("Thong so ky thuat:\n");
            specs.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        if (!specsText.isBlank()) {
            sb.append("Chi tiet bo sung: ").append(specsText).append("\n");
        }
        if (!keywords.isBlank()) {
            sb.append("Tu khoa SEO: ").append(keywords).append("\n");
        }
        sb.append("Phong cach: ").append(tone).append(" | Dinh dang: ").append(outputFormat);
        return sb.toString().trim();
    }

    private List<String> parseKeywords(String keywordsRaw, String productName, String category) {
        Set<String> kws = new LinkedHashSet<>();
        if (keywordsRaw != null && !keywordsRaw.isBlank()) {
            for (String part : keywordsRaw.split("[,;|]")) {
                String kw = part.trim();
                if (!kw.isBlank() && kws.size() < 10) {
                    kws.add(truncateCleanly(kw, 50));
                }
            }
        }
        kws.add(truncateCleanly(productName, 60));
        if (!category.isBlank()) {
            kws.add(truncateCleanly(category, 50));
        }
        kws.add("chính hãng QuickShop");
        return new ArrayList<>(kws);
    }

    private int resolveLimit(Integer requested, int min, int max, int defaultVal, String warningIfClamped, List<String> warnings) {
        if (requested == null) {
            return defaultVal;
        }
        if (requested < min) {
            if (warningIfClamped != null) warnings.add(warningIfClamped);
            return min;
        }
        if (requested > max) {
            if (warningIfClamped != null) warnings.add(warningIfClamped);
            return max;
        }
        return requested;
    }

    private String normalizeTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return "CHUYEN_NGHIEP";
        }
        String upper = tone.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("TRE") || upper.contains("CASUAL")) {
            return "TRE_TRUNG";
        }
        if (upper.contains("SANG") || upper.contains("LUXURY") || upper.contains("PREMIUM")) {
            return "SANG_TRONG";
        }
        return "CHUYEN_NGHIEP";
    }

    private String normalizeOutputFormat(String format, List<String> warnings) {
        if (format == null || format.isBlank()) {
            return "MARKDOWN";
        }
        String upper = format.trim().toUpperCase(Locale.ROOT);
        if ("PLAIN_TEXT".equals(upper) || "TEXT".equals(upper)) {
            return "PLAIN_TEXT";
        }
        if ("HTML".equals(upper)) {
            warnings.add("Đầu ra HTML thô không được phép; hệ thống tự động chuyển về MARKDOWN an toàn.");
        }
        return "MARKDOWN";
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    @Getter
    @Builder
    public static class PreparedSeoInput {
        private final String productName;
        private final String category;
        private final String brand;
        private final String price;
        private final Map<String, String> specifications;
        private final String specificationsText;
        private final List<String> keywords;
        private final String tone;
        private final String outputFormat;
        private final int maxPromptLengthLimit;
        private final int maxOutputLengthLimit;
        private final String boundedPrompt;
        private final boolean promptTruncated;
        private final boolean sanitized;
        private final List<String> warnings;
    }

    @Getter
    @Builder
    public static class SanitizedSeoOutput {
        private final String optimizedTitle;
        private final String fullDescription;
        private final String metaTitle;
        private final String metaDescription;
        private final String slug;
        private final List<String> seoKeywords;
        private final boolean sanitized;
        private final boolean valid;
        private final List<String> warnings;
    }
}
