package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        LocalDate birthDate,
        String gender,
        UserRole role,
        UserStatus status,
        UUID branchId,
        String branchName,
        OffsetDateTime createdAt
) {
}
