package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.UserCreationRequest;
import com.haui.UrbanMart.dto.request.VerifyOtpRequest;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class AuthService {
    private static final String PURPOSE_REGISTER = "register";

    private final UserRepository userRepository;
    private final UserService userService;
    private final OtpService otpService;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final SmsService smsService;

    public void register(UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("So dien thoai da duoc dang ky");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email da duoc su dung");
        }

        if (!otpService.canSend(PURPOSE_REGISTER, request.getPhone())) {
            throw new BadRequestException("Vui long doi truoc khi gui lai OTP");
        }

        pendingRegistrationStore.save(request.getPhone(), request);

        String otp = otpService.generateAndStore(PURPOSE_REGISTER, request.getPhone());
        smsService.sendOtp(request.getPhone(), otp);
    }

    public void resendOtp(String phone) {
        if (pendingRegistrationStore.get(phone) == null) {
            throw new BadRequestException("Khong tim thay yeu cau dang ky, vui long dang ky lai");
        }

        if (!otpService.canSend(PURPOSE_REGISTER, phone)) {
            throw new BadRequestException("Vui long doi truoc khi gui lai OTP");
        }

        String otp = otpService.generateAndStore(PURPOSE_REGISTER, phone);
        smsService.sendOtp(phone, otp);
    }

    public UserResponse verifyRegisterOtp(VerifyOtpRequest request) {
        boolean valid = otpService.verify(PURPOSE_REGISTER, request.getPhone(), request.getOtp());

        if (!valid) {
            throw new BadRequestException("OTP khong hop le hoac da het han");
        }

        UserCreationRequest pending = pendingRegistrationStore.get(request.getPhone());

        if (pending == null) {
            throw new BadRequestException("Yeu cau dang ky da het han, vui long dang ky lai");
        }

        User created = userService.createRequest(pending);
        pendingRegistrationStore.delete(request.getPhone());

        return UserResponse.from(created);
    }
}
