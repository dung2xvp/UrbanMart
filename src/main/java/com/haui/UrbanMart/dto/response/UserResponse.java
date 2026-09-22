package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private UserRole role;
    private UserStatus status;
    private OffsetDateTime createdAt;

    public static UserResponse from(com.haui.UrbanMart.entity.User user) {
        return new UserResponse(
            user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt()
        );
    }
}