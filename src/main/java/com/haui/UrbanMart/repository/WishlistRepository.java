package com.haui.UrbanMart.repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.haui.UrbanMart.entity.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    @EntityGraph(attributePaths = "product")
    List<Wishlist> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
}