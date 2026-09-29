package com.haui.UrbanMart.repository;

import java.util.UUID;

import com.haui.UrbanMart.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.haui.UrbanMart.entity.BranchInventory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, UUID> {
    Optional<BranchInventory> findByBranchIdAndProductId(UUID branchId, UUID productId);

    List<BranchInventory> findAllByBranchId(UUID branchId);

    @Query(
            value = """
        select bi
        from BranchInventory bi
        join fetch bi.product p
        left join fetch p.category
        left join fetch p.brand
        where bi.branch.id = :branchId
          and p.status = :status
          and (
              :keyword is null
              or lower(p.name) like lower(concat('%', :keyword, '%'))
              or lower(p.sku) like lower(concat('%', :keyword, '%'))
          )
        """,
            countQuery = """
        select count(bi)
        from BranchInventory bi
        join bi.product p
        where bi.branch.id = :branchId
          and p.status = :status
          and (
              :keyword is null
              or lower(p.name) like lower(concat('%', :keyword, '%'))
              or lower(p.sku) like lower(concat('%', :keyword, '%'))
          )
        """
    )
    Page<BranchInventory> searchProducts(
            @Param("branchId") UUID branchId,
            @Param("status") ProductStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}