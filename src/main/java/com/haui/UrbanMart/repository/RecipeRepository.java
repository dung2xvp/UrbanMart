package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.Recipe;
import com.haui.UrbanMart.entity.RecipeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("""
            SELECT DISTINCT recipe
            FROM Recipe recipe
            LEFT JOIN FETCH recipe.ingredients ingredient
            LEFT JOIN FETCH ingredient.product product
            LEFT JOIN FETCH product.category
            WHERE recipe.id = :id
            """)
    Optional<Recipe> findWithIngredientsById(@Param("id") UUID id);

    @Query("""
            SELECT recipe
            FROM Recipe recipe
            WHERE (:active IS NULL OR recipe.active = :active)
              AND (:category IS NULL OR recipe.category = :category)
              AND (
                    :keyword = ''
                    OR LOWER(recipe.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(recipe.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY recipe.createdAt DESC, recipe.id ASC
            """)
    Page<Recipe> search(
            @Param("keyword") String keyword,
            @Param("category") RecipeCategory category,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
            SELECT ingredient.recipe.id AS recipeId,
                   COUNT(ingredient) AS total
            FROM RecipeIngredient ingredient
            WHERE ingredient.recipe.id IN :recipeIds
            GROUP BY ingredient.recipe.id
            """)
    List<IngredientCount> countIngredients(@Param("recipeIds") Collection<UUID> recipeIds);

    interface IngredientCount {
        UUID getRecipeId();

        Long getTotal();
    }
}
