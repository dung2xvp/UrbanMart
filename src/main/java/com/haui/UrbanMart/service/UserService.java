package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.UpdateProfileRequest;
import com.haui.UrbanMart.dto.response.UserResponse;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.haui.UrbanMart.dto.request.UserCreationRequest;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import com.haui.UrbanMart.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service 
@RequiredArgsConstructor 
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createRequest (UserCreationRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Số điện thoại đã được đăng ký");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setBirthDate(request.getBirthDate());
        user.setGender(request.getGender());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));
        return UserResponse.from(user);
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email da duoc su dung");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
