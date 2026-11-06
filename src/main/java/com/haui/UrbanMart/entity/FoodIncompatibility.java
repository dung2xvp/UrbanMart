package com.haui.UrbanMart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "food_incompatibilities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_food_incompatibilities_pair",
                columnNames = {"tag_a", "tag_b"}
        )
)
@Getter
@Setter
public class FoodIncompatibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Luon nho hon tagB (so sanh String#compareTo) - xem V18. */
    @Column(name = "tag_a", nullable = false, length = 50)
    private String tagA;

    @Column(name = "tag_b", nullable = false, length = 50)
    private String tagB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncompatibilitySeverity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String effect;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
