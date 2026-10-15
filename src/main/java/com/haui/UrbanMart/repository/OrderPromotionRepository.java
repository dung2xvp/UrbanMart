package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.OrderPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import com.haui.UrbanMart.entity.OrderPromotionStatus;

public interface OrderPromotionRepository extends JpaRepository<OrderPromotion, UUID> {
    @EntityGraph(attributePaths = {"promotion", "customer"})
    List<OrderPromotion> findAllByOrder_IdAndStatus(
            UUID orderId,
            OrderPromotionStatus status
    );

    @EntityGraph(attributePaths = {"promotion"})
    java.util.Optional<OrderPromotion> findFirstByOrder_IdOrderByCreatedAtAsc(UUID orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE order_promotions
            SET status = 'RELEASED',
                released_at = :releasedAt
            WHERE order_id = :orderId
              AND status = 'APPLIED'
            """, nativeQuery = true)
    int releaseAppliedPromotions(
            @Param("orderId") UUID orderId,
            @Param("releasedAt") OffsetDateTime releasedAt
    );
}
