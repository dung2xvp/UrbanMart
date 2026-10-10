package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Branch;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record BranchAdminResponse(
        UUID id,
        UUID accountId,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        String phone,
        BigDecimal deliveryRadiusKm,
        boolean isWarehouse,
        Map<String, String> openingHours,
        String status,
        OffsetDateTime createdAt
) {
    public static BranchAdminResponse from(Branch branch) {
        return new BranchAdminResponse(
                branch.getId(),
                branch.getAccount() == null ? null : branch.getAccount().getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getLat(),
                branch.getLng(),
                branch.getPhone(),
                branch.getDeliveryRadiusKm(),
                branch.isWarehouse(),
                branch.getOpeningHours(),
                branch.getStatus().name(),
                branch.getCreatedAt()
        );
    }
}
