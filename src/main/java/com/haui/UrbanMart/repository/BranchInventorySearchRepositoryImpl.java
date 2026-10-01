package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductDiscount;
import com.haui.UrbanMart.entity.ProductStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class BranchInventorySearchRepositoryImpl
        implements BranchInventorySearchRepository {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<BranchInventory> searchProducts(
            UUID branchId,
            ProductStatus status,
            String keyword,
            List<UUID> categoryIds,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            boolean promotionsOnly,
            boolean dailyFreshOnly,
            ProductListSort sort,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        OffsetDateTime now = OffsetDateTime.now();

        CriteriaQuery<BranchInventory> query =
                cb.createQuery(BranchInventory.class);
        Root<BranchInventory> inventory = query.from(BranchInventory.class);
        Join<BranchInventory, Product> product =
                inventory.join("product", JoinType.INNER);
        Join<Product, ProductDiscount> discount =
                product.join("discounts", JoinType.LEFT);
        discount.on(
                cb.lessThanOrEqualTo(discount.get("startsAt"), now),
                cb.greaterThan(discount.get("endsAt"), now)
        );

        List<Predicate> predicates = buildPredicates(
                cb, inventory, product, discount, branchId, status, keyword,
                categoryIds, brandIds, minPrice, maxPrice, inStock,
                promotionsOnly, dailyFreshOnly
        );
        query.select(inventory)
                .where(predicates.toArray(Predicate[]::new));
        addFetches(inventory);
        addOrdering(query, cb, product, discount, sort);

        TypedQuery<BranchInventory> dataQuery = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize());
        List<BranchInventory> results = dataQuery.getResultList();
        long total = countResults(
                cb, branchId, status, keyword, categoryIds, brandIds,
                minPrice, maxPrice, inStock, promotionsOnly, dailyFreshOnly, now
        );
        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<BranchInventory> inventory,
            Join<BranchInventory, Product> product,
            Join<Product, ProductDiscount> discount,
            UUID branchId,
            ProductStatus status,
            String keyword,
            List<UUID> categoryIds,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            boolean promotionsOnly,
            boolean dailyFreshOnly
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(inventory.get("branch").get("id"), branchId));
        predicates.add(cb.equal(product.get("status"), status));

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(product.get("name")), pattern),
                    cb.like(cb.lower(product.get("sku")), pattern)
            ));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            predicates.add(product.get("category").get("id").in(categoryIds));
        }
        if (brandIds != null && !brandIds.isEmpty()) {
            predicates.add(product.get("brand").get("id").in(brandIds));
        }
        if (inStock != null) {
            predicates.add(inStock
                    ? cb.greaterThan(inventory.get("stockQuantity"), 0)
                    : cb.equal(inventory.get("stockQuantity"), 0));
        }
        if (promotionsOnly) {
            predicates.add(cb.isNotNull(discount.get("id")));
        }
        if (dailyFreshOnly) {
            predicates.add(cb.isTrue(product.get("dailyFresh")));
        }

        Expression<BigDecimal> price = currentPrice(cb, product, discount);
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(price, minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(price, maxPrice));
        }
        return predicates;
    }

    private long countResults(
            CriteriaBuilder cb,
            UUID branchId,
            ProductStatus status,
            String keyword,
            List<UUID> categoryIds,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            boolean promotionsOnly,
            boolean dailyFreshOnly,
            OffsetDateTime now
    ) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<BranchInventory> inventory = countQuery.from(BranchInventory.class);
        Join<BranchInventory, Product> product =
                inventory.join("product", JoinType.INNER);
        Join<Product, ProductDiscount> discount =
                product.join("discounts", JoinType.LEFT);
        discount.on(
                cb.lessThanOrEqualTo(discount.get("startsAt"), now),
                cb.greaterThan(discount.get("endsAt"), now)
        );

        countQuery.select(cb.countDistinct(inventory.get("id")))
                .where(buildPredicates(
                        cb, inventory, product, discount, branchId, status,
                        keyword, categoryIds, brandIds, minPrice, maxPrice,
                        inStock, promotionsOnly, dailyFreshOnly
                ).toArray(Predicate[]::new));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Expression<BigDecimal> currentPrice(
            CriteriaBuilder cb,
            Join<BranchInventory, Product> product,
            Join<Product, ProductDiscount> discount
    ) {
        Expression<BigDecimal> multiplier = cb.quot(
                cb.diff(
                        cb.literal(ONE_HUNDRED),
                        discount.<BigDecimal>get("discountPercent")
                ),
                cb.literal(ONE_HUNDRED)
        ).as(BigDecimal.class);
        Expression<BigDecimal> discountedPrice = cb.function(
                "round",
                BigDecimal.class,
                cb.prod(product.<BigDecimal>get("basePrice"), multiplier),
                cb.literal(2)
        );
        return cb.<BigDecimal>selectCase()
                .when(cb.isNotNull(discount.get("id")), discountedPrice)
                .otherwise(product.get("basePrice"));
    }

    private void addFetches(Root<BranchInventory> inventory) {
        var productFetch = inventory.fetch("product", JoinType.INNER);
        productFetch.fetch("category", JoinType.LEFT);
        productFetch.fetch("brand", JoinType.LEFT);
    }

    private void addOrdering(
            CriteriaQuery<BranchInventory> query,
            CriteriaBuilder cb,
            Join<BranchInventory, Product> product,
            Join<Product, ProductDiscount> discount,
            ProductListSort sort
    ) {
        switch (sort) {
            case NAME_ASC -> query.orderBy(
                    cb.asc(product.get("name")),
                    cb.asc(product.get("id"))
            );
            case NAME_DESC -> query.orderBy(
                    cb.desc(product.get("name")),
                    cb.asc(product.get("id"))
            );
            case PRICE_ASC -> query.orderBy(
                    cb.asc(currentPrice(cb, product, discount)),
                    cb.asc(product.get("id"))
            );
            case PRICE_DESC -> query.orderBy(
                    cb.desc(currentPrice(cb, product, discount)),
                    cb.asc(product.get("id"))
            );
            case DISCOUNT_DESC -> query.orderBy(
                    cb.desc(cb.coalesce(
                            discount.get("discountPercent"),
                            BigDecimal.ZERO
                    )),
                    cb.asc(product.get("id"))
            );
            case NEWEST -> query.orderBy(
                    cb.desc(product.get("createdAt")),
                    cb.asc(product.get("id"))
            );
        }
    }
}
