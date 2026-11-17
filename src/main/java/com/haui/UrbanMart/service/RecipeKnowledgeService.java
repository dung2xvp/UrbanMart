package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Recipe;
import com.haui.UrbanMart.entity.RecipeIngredient;
import com.haui.UrbanMart.repository.RecipeRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeKnowledgeService {

    static final int EMBEDDING_DIMENSIONS = 768;
    private static final int EMBEDDING_BATCH_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 6000;

    private static final String UPSERT_SQL = """
            INSERT INTO recipe_embeddings (recipe_id, content, embedding, updated_at)
            VALUES (?, ?, CAST(? AS vector), now())
            ON CONFLICT (recipe_id) DO UPDATE
            SET content = EXCLUDED.content,
                embedding = EXCLUDED.embedding,
                updated_at = now()
            """;

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;
    private final RecipeRepository recipeRepository;

    public RecipeKnowledgeService(
            EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate,
            RecipeRepository recipeRepository
    ) {
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
        this.recipeRepository = recipeRepository;
    }

    @Transactional
    public void indexRecipe(Recipe recipe) {
        if (!recipe.isActive()) {
            deleteEmbedding(recipe.getId());
            return;
        }
        String content = toContent(recipe);
        float[] vector = requireDimensions(embeddingModel.embed(content));
        jdbcTemplate.update(UPSERT_SQL, recipe.getId(), content, toPgVector(vector));
    }

    @Transactional
    public int reindexAllActiveRecipes() {
        List<Recipe> recipes = recipeRepository.findAllActiveWithIngredients();
        List<String> contents = recipes.stream().map(RecipeKnowledgeService::toContent).toList();

        for (int start = 0; start < recipes.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, recipes.size());
            List<String> batchContent = contents.subList(start, end);
            List<float[]> vectors = embeddingModel.embed(batchContent);
            if (vectors.size() != batchContent.size()) {
                throw new IllegalStateException("Gemini trả về số embedding không khớp dữ liệu");
            }

            List<Object[]> rows = new ArrayList<>(vectors.size());
            for (int offset = 0; offset < vectors.size(); offset++) {
                rows.add(new Object[]{
                        recipes.get(start + offset).getId(),
                        batchContent.get(offset),
                        toPgVector(requireDimensions(vectors.get(offset)))
                });
            }
            jdbcTemplate.batchUpdate(UPSERT_SQL, rows);
        }

        jdbcTemplate.update("""
                DELETE FROM recipe_embeddings embedding
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM recipes recipe
                    WHERE recipe.id = embedding.recipe_id
                      AND recipe.is_active = true
                )
                """);
        return recipes.size();
    }

    @Transactional
    public void deleteEmbedding(UUID recipeId) {
        jdbcTemplate.update(
                "DELETE FROM recipe_embeddings WHERE recipe_id = ?",
                recipeId
        );
    }

    public List<RecipeEmbeddingCandidate> search(String query, int limit) {
        float[] vector = requireDimensions(embeddingModel.embed(query));
        String pgVector = toPgVector(vector);
        return jdbcTemplate.query("""
                SELECT embedding.recipe_id,
                       embedding.content,
                       embedding.embedding <=> CAST(? AS vector) AS distance
                FROM recipe_embeddings embedding
                JOIN recipes recipe ON recipe.id = embedding.recipe_id
                WHERE recipe.is_active = true
                ORDER BY embedding.embedding <=> CAST(? AS vector)
                LIMIT ?
                """,
                (resultSet, rowNumber) -> new RecipeEmbeddingCandidate(
                        resultSet.getObject("recipe_id", UUID.class),
                        resultSet.getString("content"),
                        resultSet.getDouble("distance")
                ),
                pgVector,
                pgVector,
                limit
        );
    }

    public long countIndexedRecipes() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recipe_embeddings",
                Long.class
        );
        return count == null ? 0 : count;
    }

    static String toContent(Recipe recipe) {
        StringBuilder content = new StringBuilder()
                .append("Tên món: ").append(recipe.getName())
                .append("\nDanh mục: ").append(recipe.getCategory().getLabel());
        if (StringUtils.hasText(recipe.getDescription())) {
            content.append("\nMô tả: ").append(recipe.getDescription().trim());
        }
        content.append("\nNguyên liệu:");
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            content.append("\n- ")
                    .append(ingredient.getProduct().getName())
                    .append(", số lượng: ").append(ingredient.getQuantity())
                    .append(' ').append(ingredient.getProduct().getUnit());
            if (StringUtils.hasText(ingredient.getNote())) {
                content.append(", ghi chú: ").append(ingredient.getNote().trim());
            }
            if (!ingredient.isRequired()) {
                content.append(" (không bắt buộc)");
            }
        }
        return content.length() <= MAX_CONTENT_LENGTH
                ? content.toString()
                : content.substring(0, MAX_CONTENT_LENGTH);
    }

    static String toPgVector(float[] vector) {
        requireDimensions(vector);
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            float component = vector[index];
            if (!Float.isFinite(component)) {
                throw new IllegalStateException("Embedding chứa giá trị không hữu hạn");
            }
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(component));
        }
        return value.append(']').toString();
    }

    private static float[] requireDimensions(float[] vector) {
        if (vector == null || vector.length != EMBEDDING_DIMENSIONS) {
            throw new IllegalStateException(
                    "Embedding phải có đúng " + EMBEDDING_DIMENSIONS + " chiều"
            );
        }
        return vector;
    }

    public record RecipeEmbeddingCandidate(UUID recipeId, String content, double distance) {
    }
}
