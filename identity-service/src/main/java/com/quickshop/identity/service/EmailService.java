package com.quickshop.identity.service;

public interface EmailService {

    /**
     * Gửi mã OTP xác thực tới địa chỉ email người dùng
     *
     * @param recipientEmail Địa chỉ email người nhận
     * @param recipientName  Tên người nhận
     * @param otpCode        Mã OTP 6 chữ số
     * @param purpose        Mục đích gửi (Đăng ký tài khoản, Đặt lại mật khẩu, ...)
     */
    void sendOtpEmail(String recipientEmail, String recipientName, String otpCode, String purpose);
}
