package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.ProductDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, UUID> {

    @Query("""
            select discount
            from ProductDiscount discount
            where discount.product.id = :productId
              and discount.startsAt <= :now
              and discount.endsAt > :now
            """)
    Optional<ProductDiscount> findActiveByProductId(
            @Param("productId") UUID productId,
            @Param("now") OffsetDateTime now
    );

    List<ProductDiscount> findAllByOrderByCreatedAtDesc();

    @Query("""
            select (count(discount) > 0)
            from ProductDiscount discount
            where discount.product.id = :productId
              and discount.startsAt < :endsAt
              and discount.endsAt > :startsAt
              and (:excludedId is null or discount.id <> :excludedId)
            """)
    boolean existsOverlappingDiscount(
            @Param("productId") UUID productId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("excludedId") UUID excludedId
    );
}
