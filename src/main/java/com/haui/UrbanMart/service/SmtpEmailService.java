package com.haui.UrbanMart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private static final String SUBJECT = "Mã xác minh UrbanMart";

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromAddress;

    @Override
    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText("""
                Mã OTP của bạn là: %s

                Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.
                """.formatted(otp));

        mailSender.send(message);
    }
}
