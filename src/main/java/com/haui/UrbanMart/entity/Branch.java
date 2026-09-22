package com.haui.UrbanMart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "branches")
@Getter @Setter
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // 1-1: moi chi nhanh co toi da 1 tai khoan dung chung (UNIQUE ben DB).
    // account_id co the NULL neu chi nhanh moi tao nhung chua gan tai khoan.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true)
    private User account;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "delivery_radius_km", precision = 5, scale = 2)
    private BigDecimal deliveryRadiusKm = BigDecimal.valueOf(5);

    @Column(name = "is_warehouse", nullable = false)
    private boolean isWarehouse = false;

    @Column(length = 20)
    private String phone;

    // JSONB - map truc tiep sang Map<String,String>, Hibernate 6 tu serialize/deserialize.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "opening_hours")
    private Map<String, String> openingHours;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private BranchStatus status = BranchStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;
}