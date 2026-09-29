package com.haui.UrbanMart.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haui.UrbanMart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndBranchId(UUID userId, UUID branchId);

}