package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findAllByOrder_IdOrderByCreatedAtAsc(UUID orderId);
}
