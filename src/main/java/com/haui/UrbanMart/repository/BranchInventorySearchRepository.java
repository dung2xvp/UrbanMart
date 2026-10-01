package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BranchInventorySearchRepository {

    Page<BranchInventory> searchProducts(
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
    );
}
