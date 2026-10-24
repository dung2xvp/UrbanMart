package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    Optional<Promotion> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT promotion
            FROM Promotion promotion
            WHERE promotion.id = :id
            """)
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            SELECT promotion
            FROM Promotion promotion
            WHERE promotion.active = true
              AND (:availableOnly = false OR (
                  promotion.startsAt <= :now
                  AND promotion.endsAt > :now
                  AND (
                      promotion.usageLimit IS NULL
                      OR promotion.usedCount < promotion.usageLimit
                  )
              ))
              AND (
                  :keyword IS NULL
                  OR lower(promotion.code) LIKE lower(concat('%', :keyword, '%'))
                  OR lower(promotion.title) LIKE lower(concat('%', :keyword, '%'))
              )
            ORDER BY promotion.createdAt DESC
            """)
    Page<Promotion> findPublicVouchers(
            @Param("keyword") String keyword,
            @Param("availableOnly") boolean availableOnly,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT promotion
            FROM Promotion promotion
            WHERE (:active IS NULL OR promotion.active = :active)
              AND (
                  :keyword IS NULL
                  OR lower(promotion.code) LIKE lower(concat('%', :keyword, '%'))
                  OR lower(promotion.title) LIKE lower(concat('%', :keyword, '%'))
              )
            ORDER BY promotion.createdAt DESC
            """)
    Page<Promotion> searchAdminVouchers(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT promotion
            FROM Promotion promotion
            WHERE lower(promotion.code) = lower(:code)
            """)
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE promotions
            SET used_count = used_count + 1,
                updated_at = now()
            WHERE id = :promotionId
              AND active = true
              AND (usage_limit IS NULL OR used_count < usage_limit)
            """, nativeQuery = true)
    int incrementUsageIfAvailable(@Param("promotionId") UUID promotionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE promotions
            SET used_count = used_count - 1,
                updated_at = now()
            WHERE id = :promotionId
              AND used_count > 0
            """, nativeQuery = true)
    int decrementUsageIfPositive(@Param("promotionId") UUID promotionId);
}
