package com.haui.UrbanMart.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.repository.projection.BranchInventorySummaryProjection;
import com.haui.UrbanMart.repository.projection.BranchNearbyProjection;

public interface BranchRepository extends JpaRepository <Branch, UUID> {

    Optional<Branch> findByAccount_Id(UUID accountId);
    List<Branch> findAllByAccount_IdIn(List<UUID> accountIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT branch
            FROM Branch branch
            WHERE branch.id = :branchId
            """)
    Optional<Branch> findForUpdateById(@Param("branchId") UUID branchId);

    @Query(value = """
            SELECT b.id AS "branchId",
                   b.name AS "branchName",
                   b.address AS "branchAddress",
                   b.phone AS "branchPhone",
                   COALESCE(SUM(bi.stock_quantity), 0) AS "totalStock"
            FROM branches b
            LEFT JOIN branch_inventory bi ON bi.branch_id = b.id
            GROUP BY b.id, b.name, b.address, b.phone
            ORDER BY b.name
            """, nativeQuery = true)
    List<BranchInventorySummaryProjection> findInventorySummaries();

    @Query(value = """
            SELECT
                b.id AS id,
                b.name AS name,
                b.address AS address,
                b.lat AS lat,
                b.lng AS lng,
                b.phone AS phone,
                b.delivery_radius_km AS deliveryRadiusKm,
                6371 * acos(
                    LEAST(
                        1.0,
                        GREATEST(
                            -1.0,
                            cos(radians(:lat))
                            * cos(radians(b.lat))
                            * cos(radians(b.lng) - radians(:lng))
                            + sin(radians(:lat))
                            * sin(radians(b.lat))
                        )
                    )
                ) AS distanceKm
            FROM branches b
            WHERE b.status = 'ACTIVE'
              AND b.is_warehouse = false
              AND b.lat IS NOT NULL
              AND b.lng IS NOT NULL
              AND 6371 * acos(
                    LEAST(
                        1.0,
                        GREATEST(
                            -1.0,
                            cos(radians(:lat))
                            * cos(radians(b.lat))
                            * cos(radians(b.lng) - radians(:lng))
                            + sin(radians(:lat))
                            * sin(radians(b.lat))
                        )
                    )
                  ) <= :radiusKm
            ORDER BY distanceKm ASC
            """, nativeQuery = true)
    List<BranchNearbyProjection> findNearby(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radiusKm") BigDecimal radiusKm
    );
}