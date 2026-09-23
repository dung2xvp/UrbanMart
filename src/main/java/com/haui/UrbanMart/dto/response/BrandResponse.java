package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Brand;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class BrandResponse {

    private UUID id;
    private String name;
    private String logoUrl;

    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getLogoUrl()
        );
    }
}
