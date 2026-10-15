package com.haui.UrbanMart.repository;

import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.haui.UrbanMart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndBranchId(UUID userId, UUID branchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cart
            FROM Cart cart
            WHERE cart.user.id = :userId
              AND cart.branch.id = :branchId
            """)
    Optional<Cart> findForUpdateByUserIdAndBranchId(
            @Param("userId") UUID userId,
            @Param("branchId") UUID branchId
    );
}