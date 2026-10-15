package com.haui.UrbanMart.dto.request;

import com.haui.UrbanMart.entity.DeliveryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull
    private UUID clientRequestId;

    @NotNull
    private UUID branchId;

    @NotNull
    private DeliveryType deliveryType;

    @Size(max = 50)
    private String promotionCode;

    private UUID addressId;

    private OffsetDateTime receiveFrom;

    private OffsetDateTime receiveTo;

    @NotBlank
    @Size(max = 150)
    private String recipientName;

    @NotBlank
    @Size(min = 7, max = 20)
    private String recipientPhone;

    @Size(max = 600)
    private String shippingAddress;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal shippingLat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal shippingLng;

    @Size(max = 1000)
    private String note;
}
