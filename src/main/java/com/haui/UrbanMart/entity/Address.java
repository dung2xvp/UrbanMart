package com.haui.UrbanMart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter @Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ManyToOne vi 1 user co nhieu dia chi (1-N). Chi khai bao 1 chieu (khong
    // can User.addresses nguoc lai) de tranh vong lap serialize JSON.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String line;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String city;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;
}