package com.quickshop.identity.service;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.identity.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService - Quản lý tạo mã OTP qua Redis TTL 5 phút, giới hạn gửi lại 60s và xác thực OTP")
class OtpServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpServiceImpl otpService;

    private final String email = "buyer@quickshop.vn";
    private final String otpKey = OtpServiceImpl.OTP_KEY_PREFIX + email;
    private final String cooldownKey = OtpServiceImpl.COOLDOWN_KEY_PREFIX + email;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Nghiệp vụ Tạo & Lưu OTP (generateAndSaveOtp)")
    class GenerateOtpTests {

        @Test
        @DisplayName("Tạo OTP thành công khi hết cooldown: mã 6 chữ số, TTL 5 phút (300s), cooldown 60s")
        void generateAndSaveOtp_Success() {
            when(stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(-2L); // Key không tồn tại

            String otp = otpService.generateAndSaveOtp(email);

            assertThat(otp).isNotNull().matches("^\\d{6}$");

            // Kiểm tra lưu OTP vào Redis với TTL 5 phút
            verify(valueOperations, times(1)).set(eq(otpKey), eq(otp), eq(Duration.ofMinutes(5)));

            // Kiểm tra thiết lập Cooldown 60 giây
            verify(valueOperations, times(1)).set(eq(cooldownKey), anyString(), eq(Duration.ofSeconds(60)));
        }

        @Test
        @DisplayName("Tạo OTP thất bại khi cooldown vẫn còn hiệu lực (bị rate limit)")
        void generateAndSaveOtp_CooldownActive_ThrowsBadRequest() {
            when(stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(45L); // Còn 45 giây

            assertThatThrownBy(() -> otpService.generateAndSaveOtp(email))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Vui lòng đợi 45 giây trước khi yêu cầu gửi lại mã OTP");

            // Không được ghi thêm OTP vào Redis
            verify(valueOperations, never()).set(eq(otpKey), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Xác thực OTP (verifyOtp)")
    class VerifyOtpTests {

        @Test
        @DisplayName("Xác thực thành công khi mã OTP trùng khớp và xóa mã khỏi Redis")
        void verifyOtp_Success() {
            when(valueOperations.get(otpKey)).thenReturn("123456");

            otpService.verifyOtp(email, "123456");

            verify(stringRedisTemplate, times(1)).delete(otpKey);
        }

        @Test
        @DisplayName("Xác thực thất bại khi mã OTP đã hết hạn hoặc không tồn tại trong Redis")
        void verifyOtp_ExpiredOrNotFound_ThrowsBadRequest() {
            when(valueOperations.get(otpKey)).thenReturn(null);

            assertThatThrownBy(() -> otpService.verifyOtp(email, "123456"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã OTP đã hết hạn hoặc không tồn tại");

            verify(stringRedisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Xác thực thất bại khi mã OTP nhập không chính xác")
        void verifyOtp_WrongOtp_ThrowsBadRequest() {
            when(valueOperations.get(otpKey)).thenReturn("123456");

            assertThatThrownBy(() -> otpService.verifyOtp(email, "654321"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã OTP không chính xác");

            verify(stringRedisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Xác thực thất bại khi mã OTP rỗng hoặc khoảng trắng")
        void verifyOtp_EmptyOtp_ThrowsBadRequest() {
            assertThatThrownBy(() -> otpService.verifyOtp(email, ""))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Mã OTP không được để trống");
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ Gửi lại OTP & Kiểm tra TTL/Cooldown")
    class ResendAndTtlTests {

        @Test
        @DisplayName("resendOtp thực thi generateAndSaveOtp thành công")
        void resendOtp_Success() {
            when(stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(0L);

            String otp = otpService.resendOtp(email);

            assertThat(otp).isNotNull().matches("^\\d{6}$");
            verify(valueOperations, times(1)).set(eq(otpKey), eq(otp), eq(Duration.ofMinutes(5)));
        }

        @Test
        @DisplayName("getRemainingCooldown trả về số giây còn lại chính xác")
        void getRemainingCooldown_ReturnsSeconds() {
            when(stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(35L);

            long remaining = otpService.getRemainingCooldown(email);

            assertThat(remaining).isEqualTo(35L);
        }

        @Test
        @DisplayName("getRemainingCooldown trả về 0 khi key đã hết hạn")
        void getRemainingCooldown_Expired_ReturnsZero() {
            when(stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)).thenReturn(-2L);

            long remaining = otpService.getRemainingCooldown(email);

            assertThat(remaining).isEqualTo(0L);
        }

        @Test
        @DisplayName("getRemainingOtpTtl trả về số giây còn lại của mã OTP")
        void getRemainingOtpTtl_ReturnsSeconds() {
            when(stringRedisTemplate.getExpire(otpKey, TimeUnit.SECONDS)).thenReturn(280L);

            long ttl = otpService.getRemainingOtpTtl(email);

            assertThat(ttl).isEqualTo(280L);
        }
    }
}
