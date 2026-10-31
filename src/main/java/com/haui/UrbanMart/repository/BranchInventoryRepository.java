package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;

import com.haui.UrbanMart.entity.BranchInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;

public interface BranchInventoryRepository extends
        JpaRepository<BranchInventory, UUID>,
        BranchInventorySearchRepository {
    Optional<BranchInventory> findByBranchIdAndProductId(UUID branchId, UUID productId);

    @Query("""
            SELECT inventory.product.id AS productId,
                   SUM(inventory.stockQuantity) AS stockQuantity
            FROM BranchInventory inventory
            WHERE inventory.product.id IN :productIds
            GROUP BY inventory.product.id
            """)
    List<ProductStockQuantity> sumStockQuantitiesByProductIds(
            @Param("productIds") Collection<UUID> productIds
    );

    interface ProductStockQuantity {
        UUID getProductId();

        Long getStockQuantity();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT inventory
            FROM BranchInventory inventory
            WHERE inventory.branch.id = :branchId
              AND inventory.product.id = :productId
            """)
    Optional<BranchInventory> findForUpdate(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId
    );

    @EntityGraph(attributePaths = {"branch", "product", "product.category"})
    List<BranchInventory> findAllByBranchId(UUID branchId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    @Query("""
            SELECT inventory
            FROM BranchInventory inventory
            WHERE inventory.branch.id = :branchId
              AND (:categoryId IS NULL OR inventory.product.category.id = :categoryId)
              AND (
                    :keyword IS NULL
                    OR LOWER(inventory.product.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(inventory.product.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY inventory.product.name ASC, inventory.product.id ASC
            """)
    Page<BranchInventory> searchAdminInventory(
            @Param("branchId") UUID branchId,
            @Param("categoryId") UUID categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO branch_inventory (
                branch_id, product_id, stock_quantity, updated_at
            )
            VALUES (:branchId, :productId, :stockQuantity, now())
            ON CONFLICT (branch_id, product_id) DO NOTHING
            """, nativeQuery = true)
    int insertStockIfMissing(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("stockQuantity") int stockQuantity
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO branch_inventory (
                branch_id, product_id, stock_quantity, updated_at
            )
            SELECT b.id, :productId, 0, now()
            FROM branches b
            ON CONFLICT (branch_id, product_id) DO NOTHING
            """, nativeQuery = true)
    int insertZeroStockForAllBranches(@Param("productId") UUID productId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO branch_inventory (
                branch_id, product_id, stock_quantity, updated_at
            )
            SELECT :branchId, p.id, 0, now()
            FROM products p
            ON CONFLICT (branch_id, product_id) DO NOTHING
            """, nativeQuery = true)
    int insertZeroStockForAllProducts(@Param("branchId") UUID branchId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE branch_inventory
        SET reserved_quantity = reserved_quantity + :quantity,
            updated_at = now()
        WHERE branch_id = :branchId
          AND product_id = :productId
          AND stock_quantity - reserved_quantity >= :quantity
          AND :quantity > 0
        """, nativeQuery = true)
    int reserveStock(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE branch_inventory
        SET stock_quantity = stock_quantity - :quantity,
            reserved_quantity = reserved_quantity - :quantity,
            updated_at = now()
        WHERE branch_id = :branchId
          AND product_id = :productId
          AND reserved_quantity >= :quantity
          AND stock_quantity >= :quantity
          AND :quantity > 0
        """, nativeQuery = true)
    int dispatchReservedStock(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE branch_inventory
        SET reserved_quantity = reserved_quantity - :quantity,
            updated_at = now()
        WHERE branch_id = :branchId
          AND product_id = :productId
          AND reserved_quantity >= :quantity
          AND :quantity > 0
        """, nativeQuery = true)
    int releaseReservedStock(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE branch_inventory
    SET stock_quantity = stock_quantity + :quantity,
        updated_at = now()
    WHERE branch_id = :branchId
      AND product_id = :productId
      AND :quantity > 0
    """, nativeQuery = true)
    int receiveReturnedStock(
            @Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            @Param("quantity") int quantity
    );
}