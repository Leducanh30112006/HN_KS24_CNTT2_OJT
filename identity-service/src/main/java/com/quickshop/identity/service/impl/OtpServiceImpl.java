package com.quickshop.identity.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    public static final String OTP_KEY_PREFIX = "identity:otp:";
    public static final String COOLDOWN_KEY_PREFIX = "identity:otp:cooldown:";
    public static final Duration OTP_TTL = Duration.ofMinutes(5); // 5 phút = 300 giây
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60); // 60 giây cooldown

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndSaveOtp(String email) {
        checkCooldown(email);

        // Sinh mã OTP 6 chữ số bảo mật từ 100000 đến 999999
        int randomCode = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(randomCode);

        String otpKey = OTP_KEY_PREFIX + email;
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;

        // Lưu OTP vào Redis với TTL 5 phút
        stringRedisTemplate.opsForValue().set(otpKey, otpCode, OTP_TTL);

        // Thiết lập cooldown 60 giây ngăn spam gửi lại liên tục
        stringRedisTemplate.opsForValue().set(cooldownKey, String.valueOf(System.currentTimeMillis()), RESEND_COOLDOWN);

        log.info("Đã tạo OTP thành công cho email: {} [OTP: {}] (TTL 5 phút, cooldown 60s)", email, otpCode);
        return otpCode;
    }

    @Override
    public void verifyOtp(String email, String otpCode) {
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new BadRequestException("Mã OTP không được để trống");
        }

        String otpKey = OTP_KEY_PREFIX + email;
        String storedOtp = stringRedisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            throw new BadRequestException("Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu mã OTP mới");
        }

        if (!storedOtp.equals(otpCode.trim())) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại");
        }

        // Xác thực thành công: xóa mã OTP khỏi Redis để ngăn tái sử dụng
        stringRedisTemplate.delete(otpKey);
        log.info("Xác thực OTP thành công cho email: {}", email);
    }

    @Override
    public String resendOtp(String email) {
        return generateAndSaveOtp(email);
    }

    @Override
    public long getRemainingCooldown(String email) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        Long expire = stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        return (expire != null && expire > 0) ? expire : 0L;
    }

    @Override
    public long getRemainingOtpTtl(String email) {
        String otpKey = OTP_KEY_PREFIX + email;
        Long expire = stringRedisTemplate.getExpire(otpKey, TimeUnit.SECONDS);
        return (expire != null && expire > 0) ? expire : 0L;
    }

    private void checkCooldown(String email) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        Long remainingCooldown = stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);

        if (remainingCooldown != null && remainingCooldown > 0) {
            log.warn("Từ chối gửi lại OTP cho email: {} do cooldown còn: {}s", email, remainingCooldown);
            throw new BadRequestException("Bạn thao tác quá nhanh. Vui lòng đợi " + remainingCooldown + " giây trước khi yêu cầu gửi lại mã OTP");
        }
    }
}
