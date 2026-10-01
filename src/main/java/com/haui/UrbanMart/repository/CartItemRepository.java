package com.haui.UrbanMart.repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.haui.UrbanMart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    @EntityGraph(attributePaths = "product")
    List<CartItem> findAllByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);

    Optional<CartItem> findByIdAndCartUserId(UUID itemId, UUID userId);
    void deleteAllByCartId(UUID cartId);
}