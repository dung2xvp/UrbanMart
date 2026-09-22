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
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "sale_start_at")
    private OffsetDateTime saleStartAt;

    @Column(name = "sale_end_at")
    private OffsetDateTime saleEndAt;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Cot "embedding VECTOR(768)" CHUA map o day theo dung phase0 - chi them
    // khi lam toi phan AI tim kiem ngu nghia (can dependency rieng cho kieu
    // VECTOR cua pgvector, Hibernate khong ho tro san).

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;

    /**
     * Gia hien thi thuc te: uu tien sale_price neu dang trong khoang khuyen
     * mai, nguoc lai dung base_price. Dung ham nay o moi noi can hien thi gia
     * (danh sach san pham, chi tiet, gio hang...) thay vi lap lai logic.
     */
    @Transient
    public BigDecimal getDisplayPrice() {
        OffsetDateTime now = OffsetDateTime.now();
        boolean onSale = salePrice != null
                && saleStartAt != null && saleEndAt != null
                && !now.isBefore(saleStartAt) && !now.isAfter(saleEndAt);
        return onSale ? salePrice : basePrice;
    }
}