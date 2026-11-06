package com.haui.UrbanMart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "food_tags")
@Getter
@Setter
public class FoodTag {

    @Id
    @Column(length = 50, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FoodTag tag)) {
            return false;
        }
        return code != null && code.equals(tag.getCode());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
