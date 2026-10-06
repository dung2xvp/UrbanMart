package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.haui.UrbanMart.entity.BranchInventory;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BranchInventoryRepository extends
        JpaRepository<BranchInventory, UUID>,
        BranchInventorySearchRepository {
    Optional<BranchInventory> findByBranchIdAndProductId(UUID branchId, UUID productId);

    List<BranchInventory> findAllByBranchId(UUID branchId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO branch_inventory (branch_id, product_id, stock_quantity, updated_at)
            VALUES (:branchId, :productId, :stockQuantity, now())
            ON CONFLICT (branch_id, product_id)
            DO UPDATE SET
                stock_quantity = EXCLUDED.stock_quantity,
                updated_at = now()
            """, nativeQuery = true)
    void upsertStock(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("stockQuantity") int stockQuantity
    );
}