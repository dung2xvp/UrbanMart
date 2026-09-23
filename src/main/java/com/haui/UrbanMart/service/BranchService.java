package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.BranchNearbyResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.projection.BranchNearbyProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    private final BranchRepository branchRepository;

    public List<BranchNearbyResponse> findNearby(
            BigDecimal lat,
            BigDecimal lng,
            BigDecimal radiusKm
    ) {
        validateCoordinates(lat, lng);

        if (radiusKm == null || radiusKm.signum() <= 0) {
            throw new BadRequestException("Ban kinh tim kiem phai lon hon 0");
        }

        return branchRepository.findNearby(lat, lng, radiusKm)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BranchNearbyResponse toResponse(BranchNearbyProjection branch) {
        BigDecimal deliveryRadiusKm = branch.getDeliveryRadiusKm();
        boolean deliverable = deliveryRadiusKm != null
                && branch.getDistanceKm().compareTo(deliveryRadiusKm) <= 0;

        return new BranchNearbyResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getLat(),
                branch.getLng(),
                branch.getPhone(),
                deliveryRadiusKm,
                branch.getDistanceKm(),
                deliverable
        );
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lat.compareTo(MIN_LATITUDE) < 0
                || lat.compareTo(MAX_LATITUDE) > 0) {
            throw new BadRequestException("Vi do phai nam trong khoang -90 den 90");
        }

        if (lng == null || lng.compareTo(MIN_LONGITUDE) < 0
                || lng.compareTo(MAX_LONGITUDE) > 0) {
            throw new BadRequestException("Kinh do phai nam trong khoang -180 den 180");
        }
    }
}