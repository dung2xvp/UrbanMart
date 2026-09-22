package com.haui.UrbanMart.exception;

// Dung cho loi 401 do NGHIEP VU (vd: sai mat khau luc doi mat khau,
// OTP sai...) - khac voi 401 do JWT sai/thieu (da xu ly rieng o
// CustomAuthenticationEntryPoint, khong di qua GlobalExceptionHandler).
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}