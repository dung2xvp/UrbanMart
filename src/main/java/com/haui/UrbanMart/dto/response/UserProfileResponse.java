package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private UserRole role;
    private UserStatus status;
    private OffsetDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getBirthDate(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
