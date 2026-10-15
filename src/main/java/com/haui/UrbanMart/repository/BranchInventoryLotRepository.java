package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.BranchInventoryLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BranchInventoryLotRepository extends
        JpaRepository<BranchInventoryLot, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot
            FROM BranchInventoryLot lot
            WHERE lot.inventory.id = :inventoryId
              AND lot.quantity > 0
            ORDER BY lot.expiryDate ASC NULLS LAST, lot.createdAt ASC, lot.id ASC
            """)
    List<BranchInventoryLot> findAvailableLotsForUpdate(
            @Param("inventoryId") UUID inventoryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lot
            FROM BranchInventoryLot lot
            WHERE lot.id = :lotId
              AND lot.inventory.branch.id = :branchId
              AND lot.quantity > 0
            """)
    java.util.Optional<BranchInventoryLot> findForUpdate(
            @Param("branchId") UUID branchId,
            @Param("lotId") UUID lotId
    );

    @Query("""
            SELECT lot
            FROM BranchInventoryLot lot
            JOIN FETCH lot.inventory inventory
            JOIN FETCH inventory.product product
            WHERE inventory.branch.id = :branchId
              AND inventory.product.id = :productId
              AND lot.quantity > 0
            ORDER BY lot.expiryDate ASC NULLS LAST, lot.createdAt ASC, lot.id ASC
            """)
    List<BranchInventoryLot> findLots(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId
    );

    @Query("""
            SELECT lot
            FROM BranchInventoryLot lot
            WHERE lot.inventory.branch.id = :branchId
              AND lot.quantity > 0
            """)
    List<BranchInventoryLot> findAllAvailableByBranchId(
            @Param("branchId") UUID branchId
    );
}
