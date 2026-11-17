package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.Recipe;
import com.haui.UrbanMart.entity.RecipeCategory;
import com.haui.UrbanMart.entity.RecipeIngredient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeKnowledgeServiceTest {

    @Test
    void contentContainsRecipeAndLinkedCatalogIngredients() {
        Recipe recipe = new Recipe();
        recipe.setName("Canh rau củ");
        recipe.setCategory(RecipeCategory.VEGETABLE);
        recipe.setDescription("Món canh thanh mát");

        Product carrot = new Product();
        carrot.setName("Cà rốt");
        carrot.setUnit("củ");

        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setProduct(carrot);
        ingredient.setQuantity(2);
        ingredient.setRequired(false);
        ingredient.setNote("Thái lát");
        recipe.getIngredients().add(ingredient);

        String content = RecipeKnowledgeService.toContent(recipe);

        assertTrue(content.contains("Tên món: Canh rau củ"));
        assertTrue(content.contains("Danh mục: Món rau, củ"));
        assertTrue(content.contains("Mô tả: Món canh thanh mát"));
        assertTrue(content.contains("Cà rốt, số lượng: 2 củ"));
        assertTrue(content.contains("ghi chú: Thái lát"));
        assertTrue(content.contains("(không bắt buộc)"));
    }

    @Test
    void pgVectorSerializationRequiresFinite768DimensionalValues() {
        float[] vector = new float[RecipeKnowledgeService.EMBEDDING_DIMENSIONS];
        vector[0] = 0.25f;
        vector[1] = -0.5f;

        String serialized = RecipeKnowledgeService.toPgVector(vector);

        assertTrue(serialized.startsWith("[0.25,-0.5,"));
        assertTrue(serialized.endsWith("]"));
        assertThrows(
                IllegalStateException.class,
                () -> RecipeKnowledgeService.toPgVector(new float[]{1f})
        );
        vector[0] = Float.NaN;
        assertThrows(
                IllegalStateException.class,
                () -> RecipeKnowledgeService.toPgVector(vector)
        );
    }
}
