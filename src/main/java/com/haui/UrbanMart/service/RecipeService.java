package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.RecipeWriteRequest;
import com.haui.UrbanMart.dto.response.RecipeAdminResponse;
import com.haui.UrbanMart.dto.response.RecipeIngredientsResponse;
import com.haui.UrbanMart.dto.response.RecipeSummaryResponse;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.IngredientGroup;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.entity.Recipe;
import com.haui.UrbanMart.entity.RecipeCategory;
import com.haui.UrbanMart.entity.RecipeIngredient;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import com.haui.UrbanMart.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private static final int MAX_SIMILAR_PRODUCTS = 3;
    private static final int MAX_SLUG_LENGTH = 200;

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final ProductPricingService productPricingService;

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public Page<RecipeAdminResponse> getAdminRecipes(
            String keyword,
            RecipeCategory category,
            Boolean isActive,
            Pageable pageable
    ) {
        return recipeRepository.search(normalizeKeyword(keyword), category, isActive, pageable)
                .map(recipe -> toAdminResponse(recipe, false));
    }

    @Transactional(readOnly = true)
    public RecipeAdminResponse getAdminRecipe(UUID id) {
        return toAdminResponse(findWithIngredients(id), true);
    }

    @Transactional
    public RecipeAdminResponse create(RecipeWriteRequest request) {
        String slug = resolveSlug(request.slug(), request.name(), null);
        Map<UUID, Product> products = loadProducts(request.ingredients());

        Recipe recipe = new Recipe();
        applyFields(recipe, request, slug);
        addIngredients(recipe, request.ingredients(), products);
        return toAdminResponse(recipeRepository.save(recipe), true);
    }

    @Transactional
    public RecipeAdminResponse update(UUID id, RecipeWriteRequest request) {
        Recipe recipe = findWithIngredients(id);
        String slug = resolveSlug(request.slug(), request.name(), id);
        Map<UUID, Product> products = loadProducts(request.ingredients());

        applyFields(recipe, request, slug);
        // Xoa nguyen lieu cu va flush truoc khi them moi: Hibernate chay INSERT truoc DELETE
        // nen neu khong flush se vi pham UNIQUE (recipe_id, product_id).
        recipe.getIngredients().clear();
        recipeRepository.flush();
        addIngredients(recipe, request.ingredients(), products);
        return toAdminResponse(recipeRepository.save(recipe), true);
    }

    /** Xoa mem: an mon an khoi danh sach khach hang, giu du lieu de khoi phuc. */
    @Transactional
    public void deactivate(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
        recipe.setActive(false);
    }

    // --------------------------------------------------------------- customer

    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getPublicRecipes(
            String keyword,
            RecipeCategory category,
            Pageable pageable
    ) {
        Page<Recipe> recipes = recipeRepository.search(
                normalizeKeyword(keyword), category, true, pageable
        );
        Map<UUID, Long> counts = recipes.isEmpty()
                ? Map.of()
                : recipeRepository.countIngredients(
                                recipes.map(Recipe::getId).getContent()
                        ).stream()
                        .collect(Collectors.toMap(
                                RecipeRepository.IngredientCount::getRecipeId,
                                RecipeRepository.IngredientCount::getTotal
                        ));
        return recipes.map(recipe -> new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getSlug(),
                recipe.getImageUrl(),
                recipe.getCategory(),
                recipe.getCategory().getLabel(),
                counts.getOrDefault(recipe.getId(), 0L)
        ));
    }

    @Transactional(readOnly = true)
    public RecipeIngredientsResponse getIngredientsForBranch(UUID recipeId, UUID branchId) {
        Recipe recipe = findWithIngredients(recipeId);
        if (!recipe.isActive()) {
            throw new ResourceNotFoundException("Không tìm thấy món ăn");
        }
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh");
        }

        // San pham da ngung kinh doanh khong hien thi tren popup mua hang.
        List<RecipeIngredient> ingredients = recipe.getIngredients().stream()
                .filter(ingredient -> ingredient.getProduct().getStatus() == ProductStatus.ACTIVE)
                .toList();
        List<UUID> productIds = ingredients.stream()
                .map(ingredient -> ingredient.getProduct().getId())
                .toList();

        Map<UUID, BranchInventory> inventories = productIds.isEmpty()
                ? Map.of()
                : branchInventoryRepository
                        .findAllByBranchIdAndProduct_IdIn(branchId, productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                inventory -> inventory.getProduct().getId(),
                                Function.identity()
                        ));

        Map<UUID, List<Product>> availableByCategory = loadAvailableByCategory(
                branchId, ingredients
        );

        Set<Product> pricedProducts = new HashSet<>();
        ingredients.forEach(ingredient -> pricedProducts.add(ingredient.getProduct()));
        availableByCategory.values().forEach(pricedProducts::addAll);
        Map<UUID, ProductPrice> prices = productPricingService.getCurrentPrices(pricedProducts);

        Map<IngredientGroup, List<RecipeIngredientsResponse.Item>> grouped =
                new EnumMap<>(IngredientGroup.class);
        for (RecipeIngredient ingredient : ingredients) {
            Product product = ingredient.getProduct();
            int available = availableQuantity(inventories.get(product.getId()));
            List<Product> similar = similarProducts(product, availableByCategory);
            boolean inStock = available > 0;
            ProductPrice price = prices.get(product.getId());

            grouped.computeIfAbsent(ingredient.getGroup(), key -> new ArrayList<>())
                    .add(new RecipeIngredientsResponse.Item(
                            product.getId(),
                            product.getName(),
                            product.getDescription(),
                            product.getImageUrl(),
                            price.price(),
                            price.basePrice(),
                            price.discountPercent(),
                            product.getUnit(),
                            ingredient.getQuantity(),
                            ingredient.isRequired(),
                            ingredient.getNote(),
                            inStock,
                            available,
                            !similar.isEmpty(),
                            inStock
                                    ? List.of()
                                    : similar.stream()
                                    .map(item -> toSimilarProduct(item, prices.get(item.getId())))
                                    .toList()
                    ));
        }

        List<RecipeIngredientsResponse.Group> groups = grouped.entrySet().stream()
                .map(entry -> new RecipeIngredientsResponse.Group(
                        entry.getKey(),
                        entry.getKey().getLabel(),
                        entry.getValue()
                ))
                .toList();
        return new RecipeIngredientsResponse(recipe.getId(), recipe.getName(), branchId, groups);
    }

    // ---------------------------------------------------------------- helpers

    private Recipe findWithIngredients(UUID id) {
        return recipeRepository.findWithIngredientsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
    }

    private void applyFields(Recipe recipe, RecipeWriteRequest request, String slug) {
        recipe.setName(request.name().trim());
        recipe.setSlug(slug);
        recipe.setDescription(blankToNull(request.description()));
        recipe.setImageUrl(blankToNull(request.imageUrl()));
        recipe.setCategory(request.categoryMonAn());
        recipe.setActive(request.isActive() == null || request.isActive());
    }

    private Map<UUID, Product> loadProducts(List<RecipeWriteRequest.Ingredient> ingredients) {
        Set<UUID> ids = new HashSet<>();
        for (RecipeWriteRequest.Ingredient ingredient : ingredients) {
            if (!ids.add(ingredient.productId())) {
                throw new BadRequestException("Sản phẩm bị lặp trong danh sách nguyên liệu");
            }
        }
        Map<UUID, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (UUID id : ids) {
            Product product = products.get(id);
            if (product == null) {
                throw new ResourceNotFoundException("Không tìm thấy sản phẩm " + id);
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException(
                        "Sản phẩm " + product.getName() + " đã ngừng kinh doanh"
                );
            }
        }
        return products;
    }

    private void addIngredients(
            Recipe recipe,
            List<RecipeWriteRequest.Ingredient> requests,
            Map<UUID, Product> products
    ) {
        int order = 0;
        for (RecipeWriteRequest.Ingredient request : requests) {
            boolean required = request.isRequired() == null || request.isRequired();
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setRecipe(recipe);
            ingredient.setProduct(products.get(request.productId()));
            ingredient.setQuantity(request.quantity());
            ingredient.setRequired(required);
            ingredient.setGroup(request.group() != null
                    ? request.group()
                    : required ? IngredientGroup.MAIN : IngredientGroup.SPICE);
            ingredient.setNote(blankToNull(request.note()));
            ingredient.setSortOrder(order++);
            recipe.getIngredients().add(ingredient);
        }
    }

    private String resolveSlug(String requestedSlug, String name, UUID currentId) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String slug = requestedSlug.trim();
            boolean taken = currentId == null
                    ? recipeRepository.existsBySlug(slug)
                    : recipeRepository.existsBySlugAndIdNot(slug, currentId);
            if (taken) {
                throw new ConflictException("Slug món ăn đã tồn tại");
            }
            return slug;
        }

        String base = toSlug(name);
        if (base.isEmpty()) {
            throw new BadRequestException("Không thể sinh slug từ tên món ăn, vui lòng nhập slug");
        }
        String slug = base;
        int suffix = 2;
        while (currentId == null
                ? recipeRepository.existsBySlug(slug)
                : recipeRepository.existsBySlugAndIdNot(slug, currentId)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    /** "Củ su hào xào tàu hũ" -> "cu-su-hao-xao-tau-hu". */
    static String toSlug(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(
                        value.trim().toLowerCase().replace('đ', 'd'),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (normalized.length() > MAX_SLUG_LENGTH - 10) {
            normalized = normalized.substring(0, MAX_SLUG_LENGTH - 10)
                    .replaceAll("-+$", "");
        }
        return normalized;
    }

    private Map<UUID, List<Product>> loadAvailableByCategory(
            UUID branchId,
            Collection<RecipeIngredient> ingredients
    ) {
        Set<UUID> categoryIds = ingredients.stream()
                .map(ingredient -> ingredient.getProduct().getCategory())
                .filter(Objects::nonNull)
                .map(category -> category.getId())
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Product>> result = new LinkedHashMap<>();
        for (BranchInventory inventory : branchInventoryRepository
                .findAvailableByBranchAndCategories(
                        branchId,
                        ProductStatus.ACTIVE,
                        categoryIds
                )) {
            Product product = inventory.getProduct();
            result.computeIfAbsent(product.getCategory().getId(), key -> new ArrayList<>())
                    .add(product);
        }
        return result;
    }

    private List<Product> similarProducts(
            Product product,
            Map<UUID, List<Product>> availableByCategory
    ) {
        if (product.getCategory() == null) {
            return List.of();
        }
        return availableByCategory.getOrDefault(product.getCategory().getId(), List.of())
                .stream()
                .filter(candidate -> !candidate.getId().equals(product.getId()))
                .limit(MAX_SIMILAR_PRODUCTS)
                .toList();
    }

    private RecipeIngredientsResponse.SimilarProduct toSimilarProduct(
            Product product,
            ProductPrice price
    ) {
        return new RecipeIngredientsResponse.SimilarProduct(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                price.price(),
                price.basePrice(),
                product.getUnit()
        );
    }

    private int availableQuantity(BranchInventory inventory) {
        if (inventory == null) {
            return 0;
        }
        return Math.max(0, inventory.getStockQuantity() - inventory.getReservedQuantity());
    }

    private RecipeAdminResponse toAdminResponse(Recipe recipe, boolean includeIngredients) {
        List<RecipeAdminResponse.Ingredient> ingredients = includeIngredients
                ? recipe.getIngredients().stream()
                .sorted(Comparator.comparingInt(RecipeIngredient::getSortOrder))
                .map(ingredient -> {
                    Product product = ingredient.getProduct();
                    return new RecipeAdminResponse.Ingredient(
                            ingredient.getId(),
                            product.getId(),
                            product.getName(),
                            product.getSku(),
                            product.getImageUrl(),
                            product.getUnit(),
                            product.getBasePrice(),
                            product.getStatus(),
                            ingredient.getQuantity(),
                            ingredient.isRequired(),
                            ingredient.getGroup(),
                            ingredient.getNote()
                    );
                })
                .toList()
                : List.of();
        return new RecipeAdminResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getSlug(),
                recipe.getDescription(),
                recipe.getImageUrl(),
                recipe.getCategory(),
                recipe.getCategory().getLabel(),
                recipe.isActive(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                ingredients
        );
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? "" : keyword.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
