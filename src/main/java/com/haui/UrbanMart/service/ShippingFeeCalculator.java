package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.exception.BadRequestException;

import java.math.BigDecimal;

final class ShippingFeeCalculator {

    static final BigDecimal MAX_DELIVERY_RADIUS_KM = BigDecimal.valueOf(40);

    private static final double EARTH_RADIUS_KM = 6371;

    private ShippingFeeCalculator() {
    }

    static double distanceKm(
            Branch branch,
            BigDecimal destinationLat,
            BigDecimal destinationLng
    ) {
        if (branch.getLat() == null || branch.getLng() == null) {
            throw new BadRequestException("Chi nhánh chưa được cấu hình tọa độ giao hàng");
        }
        validateCoordinates(branch.getLat(), branch.getLng());
        validateCoordinates(destinationLat, destinationLng);

        double lat1 = Math.toRadians(branch.getLat().doubleValue());
        double lat2 = Math.toRadians(destinationLat.doubleValue());
        double latitudeDelta = lat2 - lat1;
        double longitudeDelta = Math.toRadians(
                destinationLng.doubleValue() - branch.getLng().doubleValue()
        );
        double sinLatitude = Math.sin(latitudeDelta / 2);
        double sinLongitude = Math.sin(longitudeDelta / 2);
        double haversine = sinLatitude * sinLatitude
                + Math.cos(lat1) * Math.cos(lat2)
                * sinLongitude * sinLongitude;

        return EARTH_RADIUS_KM * 2 * Math.asin(Math.sqrt(Math.min(1, haversine)));
    }

    static BigDecimal calculateFee(BigDecimal subtotal, double distanceKm) {
        if (distanceKm < 0 || distanceKm > MAX_DELIVERY_RADIUS_KM.doubleValue()) {
            throw new BadRequestException("Địa chỉ giao hàng nằm ngoài phạm vi phục vụ 40 km");
        }

        int distanceBand = distanceKm <= 3 ? 0
                : distanceKm <= 10 ? 1
                : distanceKm <= 20 ? 2
                : distanceKm <= 30 ? 3
                : 4;

        if (subtotal.compareTo(BigDecimal.valueOf(300_000)) < 0) {
            return feeFor(distanceBand, 10_000, 20_000, 30_000, 40_000, 40_000);
        }
        if (subtotal.compareTo(BigDecimal.valueOf(500_000)) <= 0) {
            return feeFor(distanceBand, 0, 10_000, 15_000, 20_000, 20_000);
        }
        return feeFor(distanceBand, 0, 0, 0, 0, 20_000);
    }

    private static BigDecimal feeFor(
            int distanceBand,
            long upTo3Km,
            long upTo10Km,
            long upTo20Km,
            long upTo30Km,
            long upTo40Km
    ) {
        long[] fees = {upTo3Km, upTo10Km, upTo20Km, upTo30Km, upTo40Km};
        return BigDecimal.valueOf(fees[distanceBand]);
    }

    private static void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null
                || lat.compareTo(BigDecimal.valueOf(-90)) < 0
                || lat.compareTo(BigDecimal.valueOf(90)) > 0
                || lng.compareTo(BigDecimal.valueOf(-180)) < 0
                || lng.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Tọa độ giao hàng không hợp lệ");
        }
    }
}
