package com.haui.UrbanMart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class PromotionUserUsageId implements Serializable {

    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
}
