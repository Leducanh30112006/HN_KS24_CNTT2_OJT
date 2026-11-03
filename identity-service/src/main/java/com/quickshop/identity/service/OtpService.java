package com.quickshop.identity.service;

public interface OtpService {

    /**
     * Tạo mã OTP 6 chữ số ngẫu nhiên, lưu vào Redis với TTL 5 phút (300 giây)
     * và thiết lập giới hạn gửi lại (cooldown 60 giây).
     *
     * @param email Địa chỉ email nhận OTP
     * @return Mã OTP 6 chữ số vừa tạo
     */
    String generateAndSaveOtp(String email);

    /**
     * Xác thực mã OTP người dùng gửi lên so với mã trong Redis.
     * Nếu khớp, xóa OTP khỏi Redis để tránh sử dụng lại.
     *
     * @param email Địa chỉ email xác thực
     * @param otpCode Mã OTP cần kiểm tra
     */
    void verifyOtp(String email, String otpCode);

    /**
     * Yêu cầu gửi lại OTP cho email, kiểm tra giới hạn cooldown 60 giây.
     *
     * @param email Địa chỉ email
     * @return Mã OTP mới
     */
    String resendOtp(String email);

    /**
     * Lấy số giây còn lại của thời gian chờ gửi lại (cooldown).
     *
     * @param email Địa chỉ email
     * @return Số giây còn lại (0 nếu đã hết cooldown)
     */
    long getRemainingCooldown(String email);

    /**
     * Lấy thời gian sống còn lại (TTL) của mã OTP hiện tại tính bằng giây.
     *
     * @param email Địa chỉ email
     * @return Số giây còn lại (0 nếu không tồn tại hoặc đã hết hạn)
     */
    long getRemainingOtpTtl(String email);
}
