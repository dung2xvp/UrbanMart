package com.haui.UrbanMart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "recipe_ingredients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recipe_ingredients_recipe_product",
                columnNames = {"recipe_id", "product_id"}
        )
)
@Getter
@Setter
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingredient_group", nullable = false, length = 20)
    private IngredientGroup group;

    @Column(length = 255)
    private String note;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
