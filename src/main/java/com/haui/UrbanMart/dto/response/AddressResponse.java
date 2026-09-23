package com.haui.UrbanMart.dto.response;

import com.haui.UrbanMart.entity.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private String recipientName;
    private String phone;
    private String line;
    private String ward;
    private String district;
    private String city;
    private BigDecimal lat;
    private BigDecimal lng;
    private boolean isDefault;
    private OffsetDateTime createdAt;

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getLat(),
                address.getLng(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }
}
