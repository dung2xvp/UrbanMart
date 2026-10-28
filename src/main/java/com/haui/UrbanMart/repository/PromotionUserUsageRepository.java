package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.PromotionUserUsage;
import com.haui.UrbanMart.entity.PromotionUserUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.List;

public interface PromotionUserUsageRepository
        extends JpaRepository<PromotionUserUsage, PromotionUserUsageId> {

    @Query("""
            SELECT usage
            FROM PromotionUserUsage usage
            WHERE usage.id.customerId = :customerId
              AND usage.id.promotionId IN :promotionIds
            """)
    List<PromotionUserUsage> findAllForCustomerAndPromotions(
            @Param("customerId") UUID customerId,
            @Param("promotionIds") List<UUID> promotionIds
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO promotion_user_usage (
                promotion_id, customer_id, used_count, updated_at
            )
            VALUES (:promotionId, :customerId, 1, now())
            ON CONFLICT (promotion_id, customer_id)
            DO UPDATE
               SET used_count = promotion_user_usage.used_count + 1,
                   updated_at = now()
             WHERE promotion_user_usage.used_count < :perUserLimit
            """, nativeQuery = true)
    int incrementUserUsageIfAvailable(
            @Param("promotionId") UUID promotionId,
            @Param("customerId") UUID customerId,
            @Param("perUserLimit") int perUserLimit
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE promotion_user_usage
            SET used_count = used_count - 1,
                updated_at = now()
            WHERE promotion_id = :promotionId
              AND customer_id = :customerId
              AND used_count > 0
            """, nativeQuery = true)
    int decrementUserUsageIfPositive(
            @Param("promotionId") UUID promotionId,
            @Param("customerId") UUID customerId
    );
}
