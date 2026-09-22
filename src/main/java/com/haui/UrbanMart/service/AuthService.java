package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.*;
import com.haui.UrbanMart.dto.response.LoginResponse;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.exception.UnauthorizedException;
import com.haui.UrbanMart.repository.UserRepository;
import com.haui.UrbanMart.security.JwtUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service 
@RequiredArgsConstructor
public class AuthService {
    private static final String PURPOSE_REGISTER = "register";
    private static final String PURPOSE_RESET = "reset-password";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final UserService userService;
    private final OtpService otpService;
    private final PendingRegistrationStore pendingRegistrationStore;
    private final SmsService smsService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public void register(UserCreationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu và mật khẩu xác nhận không khớp");
        }

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
            throw new BadRequestException("Vui lòng đợi trước khi gửi lại OTP");
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

    public LoginResponse login (LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new BadRequestException("Thông tin đăng nhập không hợp lệ"));

        OffsetDateTime now = OffsetDateTime.now();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BadRequestException("Tài khoản đang bị khóa");
        }

        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(now)) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts((short) 0);
            userRepository.save(user);
        }

        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            request.getPhone(),
                            request.getPassword()
                    );

            authenticationManager.authenticate(authenticationToken);

            user.setFailedLoginAttempts((short) 0);
            user.setLockedUntil(null);
            userRepository.save(user);

            String accessToken = jwtUtil.generateToken(
                    user.getId(),
                    user.getRole().name()
            );

            return new LoginResponse(
                    accessToken,
                    UserResponse.from(user)
            );
        } catch (BadCredentialsException exception) {
            handleFailedLogin(user);
            throw new BadRequestException("Thông tin đăng nhập không hợp lệ");
        }
    }
    private void handleFailedLogin(User user) {
        short failedAttempts = (short) (user.getFailedLoginAttempts() + 1);
        user.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(
                    OffsetDateTime.now().plus(LOCK_DURATION)
            );
        }

        userRepository.save(user);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> user = userRepository.findByPhone(request.getPhone());

        if (user.isPresent()
                && otpService.canSend(PURPOSE_RESET, request.getPhone())) {
            String otp = otpService.generateAndStore(
                    PURPOSE_RESET,
                    request.getPhone()
            );
            smsService.sendOtp(request.getPhone(), otp);
        }
    }
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new BadRequestException("OTP khong hop le hoac da het han"));

        boolean valid = otpService.verify(
                PURPOSE_RESET,
                request.getPhone(),
                request.getOtp()
        );

        if (!valid) {
            throw new BadRequestException("OTP khong hop le hoac da het han");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts((short) 0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Mật khẩu mới và mật khẩu xác nhận không khớp");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Mật khẩu cũ không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
