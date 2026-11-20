package com.quickshop.identity.service.impl;

import com.quickshop.identity.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    @Async
    public void sendOtpEmail(String recipientEmail, String recipientName, String otpCode, String purpose) {
        log.info("===> [OTP EMAIL] Gửi mã OTP [{}] tới email: {} ({})", otpCode, recipientEmail, purpose);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !StringUtils.hasText(fromEmail)) {
            log.info("[OTP SIMULATION] SMTP chưa được cấu hình mail sender hoặc username. Mã OTP cho {}: {}", recipientEmail, otpCode);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "QuickShop Marketplace");
            helper.setTo(recipientEmail);
            helper.setSubject("[QuickShop] Mã xác thực " + purpose + ": " + otpCode);

            String htmlBody = buildOtpEmailHtml(recipientName, otpCode, purpose);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Đã gửi email OTP thực tế thành công tới: {}", recipientEmail);
        } catch (Exception ex) {
            log.warn("Không thể gửi email thực tế qua SMTP tới {}: {}. Mã OTP hợp lệ là: {}", recipientEmail, ex.getMessage(), otpCode);
        }
    }

    private String buildOtpEmailHtml(String recipientName, String otpCode, String purpose) {
        String displayName = StringUtils.hasText(recipientName) ? recipientName : "Quý khách";
        return "<!DOCTYPE html>"
                + "<html lang='vi'>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:24px;background-color:#F8FAFC;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#1E293B;'>"
                + "  <div style='max-width:540px;margin:0 auto;background:#FFFFFF;border-radius:24px;border:1px solid #E2E8F0;box-shadow:0 10px 25px -5px rgba(0,0,0,0.05);overflow:hidden;'>"
                + "    <div style='background:linear-gradient(135deg, #F97316 0%, #EA580C 100%);padding:28px 32px;text-align:center;color:#FFFFFF;'>"
                + "      <h1 style='margin:0;font-size:24px;font-weight:800;letter-spacing:-0.5px;'>QuickShop</h1>"
                + "      <p style='margin:6px 0 0;font-size:13px;opacity:0.95;'>Sàn Thương Mại Điện Tử Đa Gian Hàng Chuẩn Quốc Gia</p>"
                + "    </div>"
                + "    <div style='padding:32px;'>"
                + "      <h2 style='margin:0 0 16px;font-size:18px;font-weight:700;color:#0F172A;'>Mã Xác Thực " + purpose + "</h2>"
                + "      <p style='margin:0 0 12px;font-size:14px;line-height:1.6;color:#475569;'>Xin chào <strong>" + displayName + "</strong>,</p>"
                + "      <p style='margin:0 0 20px;font-size:14px;line-height:1.6;color:#475569;'>Bạn vừa yêu cầu mã xác thực cho thao tác <strong>" + purpose + "</strong> trên hệ thống QuickShop. Vui lòng sử dụng mã xác thực bên dưới để hoàn tất:</p>"
                + "      <div style='background:#FFF7ED;border:2px dashed #F97316;border-radius:16px;padding:20px;text-align:center;margin:24px 0;'>"
                + "        <div style='font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:1.5px;color:#C2410C;margin-bottom:8px;'>Mã xác thực OTP (6 chữ số)</div>"
                + "        <div style='font-family:monospace;font-size:36px;font-weight:800;letter-spacing:8px;color:#EA580C;'>" + otpCode + "</div>"
                + "      </div>"
                + "      <p style='margin:0 0 8px;font-size:13px;color:#64748B;'>• Mã có hiệu lực trong vòng <strong>5 phút</strong> kể từ thời điểm gửi.</p>"
                + "      <p style='margin:0 0 20px;font-size:13px;color:#64748B;'>• Vì mục đích an toàn, <strong>tuyệt đối không chia sẻ mã này</strong> cho bất kỳ ai khác.</p>"
                + "      <div style='border-top:1px solid #F1F5F9;padding-top:20px;margin-top:24px;font-size:12px;color:#94A3B8;text-align:center;'>"
                + "        Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận hỗ trợ QuickShop.<br>"
                + "        © 2026 QuickShop Marketplace • Hệ thống bảo mật định danh đa phân hệ"
                + "      </div>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
