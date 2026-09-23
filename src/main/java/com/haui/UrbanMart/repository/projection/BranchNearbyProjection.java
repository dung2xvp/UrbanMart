package com.haui.UrbanMart.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface BranchNearbyProjection {

    UUID getId();

    String getName();

    String getAddress();

    BigDecimal getLat();

    BigDecimal getLng();

    String getPhone();

    BigDecimal getDeliveryRadiusKm();

    BigDecimal getDistanceKm();
}
