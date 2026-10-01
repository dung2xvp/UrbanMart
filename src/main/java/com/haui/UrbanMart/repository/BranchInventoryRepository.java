package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.haui.UrbanMart.entity.BranchInventory;

public interface BranchInventoryRepository extends
        JpaRepository<BranchInventory, UUID>,
        BranchInventorySearchRepository {
    Optional<BranchInventory> findByBranchIdAndProductId(UUID branchId, UUID productId);

    List<BranchInventory> findAllByBranchId(UUID branchId);
}