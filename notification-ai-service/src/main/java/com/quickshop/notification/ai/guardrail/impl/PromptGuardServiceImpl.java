package com.quickshop.notification.ai.guardrail.impl;

import com.quickshop.notification.ai.guardrail.PromptGuardService;
import com.quickshop.notification.ai.model.PromptRiskLevel;
import com.quickshop.notification.ai.model.PromptSafetyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Triển khai kiểm tra và phòng vệ an toàn Prompt Injection đa lớp
 * FR-10: Kiểm tra Prompt Sanitization, Scope Limiting và chống Jailbreak
 */
@Slf4j
@Service
public class PromptGuardServiceImpl implements PromptGuardService {

    public static final int MAX_PROMPT_LENGTH = 500;

    // Danh sách mẫu Prompt Injection nghiêm trọng dẫn tới BLOCKED ngay lập tức
    private static final List<ThreatPattern> SEVERE_INJECTION_PATTERNS = List.of(
            new ThreatPattern(
                    Pattern.compile("(?i)(you are now|pretend you are|act as|enter)\\s+(in\\s+)?(developer\\s+mode|dan\\s+mode|jailbreak|unrestricted|god\\s+mode)", Pattern.CASE_INSENSITIVE),
                    "Jailbreak Attempt: Thử nghiệm chuyển đổi chế độ nhà phát triển (DAN/Developer Mode)"
            ),
            new ThreatPattern(
                    Pattern.compile("(?i)(bypass|disable|turn off)\\s+(all\\s+)?(safety|security|content|guardrail)\\s+(filters?|rules?|restrictions?)", Pattern.CASE_INSENSITIVE),
                    "Bypass Attempt: Cố tình yêu cầu tắt bộ lọc an toàn"
            ),
            new ThreatPattern(
                    Pattern.compile("(?i)(<script\\b[^<]*(?:(?!<\\/script>)<[^<]*)*<\\/script>|javascript:|onerror\\s*=|onload\\s*=)", Pattern.CASE_INSENSITIVE),
                    "XSS Injection Attempt: Chèn mã kịch bản độc hại"
            ),
            new ThreatPattern(
                    Pattern.compile("(?i)(union\\s+select|select\\s+.*\\s+from\\s+users|truncate\\s+table|exec\\s*\\(|eval\\s*\\()", Pattern.CASE_INSENSITIVE),
                    "SQL/Command Injection Attempt: Truy vấn bất hợp pháp hoặc thực thi mã hệ thống"
            ),
            new ThreatPattern(
                    Pattern.compile("(?i)(cat\\s+/etc/passwd|cmd\\.exe|powershell\\.exe|/bin/sh|rm\\s+-rf)", Pattern.CASE_INSENSITIVE),
                    "OS Command Injection: Thao tác lệnh shell hệ thống"
            )
    );

    // Danh sách mẫu Prompt Injection mức độ trung bình (được lọc sạch chuỗi vi phạm -> [FILTERED])
    private static final Pattern FILTER_INJECTION_REGEX = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?previous\\s+instructions|drop\\s+database|drop\\s+table|system\\s+prompt|reveal\\s+secrets|disregard\\s+previous\\s+instructions)"
    );

    // Danh sách từ khóa ngoài phạm vi thương mại điện tử (Off-topic check)
    private static final Pattern OFF_TOPIC_REGEX = Pattern.compile(
            "(?i)((viết|tạo|phát triển|hướng dẫn)\\s+.*(mã độc|virus|malware|trojan)|hack\\s+(pass|tài khoản|mật khẩu)|tấn công\\s+(ddos|máy chủ)|chế tạo vũ khí|đánh bạc online|lô đề cờ bạc|khiêu dâm|chính trị quốc gia)"
    );

    @Override
    public PromptSafetyResult inspectPrompt(String rawPrompt) {
        if (rawPrompt == null || rawPrompt.trim().isEmpty()) {
            return PromptSafetyResult.safe("", "");
        }

        // 1. Chuẩn hóa chuỗi và loại bỏ ký tự điều khiển zero-width
        String normalized = normalizeInput(rawPrompt);
        List<String> threats = new ArrayList<>();

        // 2. Kiểm tra độ dài tối đa
        if (rawPrompt.length() > MAX_PROMPT_LENGTH) {
            threats.add("Độ dài câu hỏi vượt quá giới hạn tối đa cho phép (" + MAX_PROMPT_LENGTH + " ký tự)");
        }

        // 3. Quét các mẫu tấn công nghiêm trọng (Severe Injections) -> BLOCKED
        for (ThreatPattern tp : SEVERE_INJECTION_PATTERNS) {
            if (tp.pattern.matcher(normalized).find()) {
                threats.add(tp.description);
            }
        }

        if (!threats.isEmpty()) {
            // Nếu có mẫu tấn công nghiêm trọng
            boolean isCritical = threats.stream().anyMatch(t -> t.contains("Jailbreak") || t.contains("Injection") || t.contains("Bypass"));
            if (isCritical) {
                log.warn("BLOCKED prompt injection attack detected: threats={}", threats);
                return PromptSafetyResult.blocked(
                        rawPrompt,
                        "Yêu cầu bị từ chối do vi phạm Tiêu chuẩn An toàn & Phòng chống Prompt Injection của QuickShop AI.",
                        threats
                );
            }
        }

        // 4. Kiểm tra các mẫu injection có thể lọc sạch (FILTERED)
        String sanitized = sanitize(normalized);
        boolean wasModified = !sanitized.equals(normalized);
        if (wasModified) {
            threats.add("Phát hiện và đã lọc bỏ các chỉ thị thao túng hệ thống nhạy cảm ([FILTERED])");
            log.info("SUSPICIOUS prompt sanitized: raw='{}' -> sanitized='{}'", rawPrompt, sanitized);
            return PromptSafetyResult.suspicious(rawPrompt, sanitized, threats);
        }

        return PromptSafetyResult.safe(rawPrompt, sanitized);
    }

    @Override
    public String sanitize(String rawPrompt) {
        if (rawPrompt == null) {
            return "";
        }

        // Chuẩn hóa và cắt ký tự điều khiển
        String cleaned = normalizeInput(rawPrompt);

        // Giới hạn độ dài tối đa nếu vượt mức
        if (cleaned.length() > MAX_PROMPT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_PROMPT_LENGTH).trim();
        }

        // Thay thế các mẫu injection thường gặp bằng [FILTERED] (đảm bảo tương thích ngược)
        Matcher matcher = FILTER_INJECTION_REGEX.matcher(cleaned);
        cleaned = matcher.replaceAll("[FILTERED]");

        return cleaned.trim();
    }

    @Override
    public boolean isShoppingScoped(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return true;
        }
        // Nếu phát hiện nội dung độc hại ngoài phạm vi
        if (OFF_TOPIC_REGEX.matcher(prompt).find()) {
            return false;
        }
        return true;
    }

    private String normalizeInput(String input) {
        if (input == null) return "";
        // Chuẩn hóa Unicode NFKC
        String nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC);
        // Loại bỏ ký tự zero-width, non-printable control chars ngoại trừ newline / tab
        String stripped = nfkc.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        // Chuẩn hóa khoảng trắng trùng lặp
        stripped = stripped.replaceAll("[\\t ]+", " ");
        return stripped.trim();
    }

    private record ThreatPattern(Pattern pattern, String description) {}
}
