package com.haui.UrbanMart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class BranchNearbyResponse {

    private UUID id;
    private String name;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String phone;
    private BigDecimal deliveryRadiusKm;
    private BigDecimal distanceKm;
    private boolean deliverable;
}