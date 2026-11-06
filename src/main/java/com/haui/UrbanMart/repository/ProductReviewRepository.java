package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    boolean existsByOrder_IdAndProduct_Id(UUID orderId, UUID productId);

    @EntityGraph(attributePaths = {"customer", "branch"})
    @Query("""
            SELECT review
            FROM ProductReview review
            WHERE review.product.id = :productId
              AND (:rating IS NULL OR review.rating = :rating)
            ORDER BY review.createdAt DESC, review.id ASC
            """)
    Page<ProductReview> findPublicReviews(
            @Param("productId") UUID productId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @Query("""
            SELECT review.rating AS rating,
                   COUNT(review) AS total
            FROM ProductReview review
            WHERE review.product.id = :productId
            GROUP BY review.rating
            """)
    List<RatingCount> countByRating(@Param("productId") UUID productId);

    @Query("""
            SELECT AVG(review.rating)
            FROM ProductReview review
            WHERE review.product.id = :productId
            """)
    Double averageRatingByProduct(@Param("productId") UUID productId);

    long countByProduct_Id(UUID productId);

    @Query("""
            SELECT AVG(review.rating)
            FROM ProductReview review
            WHERE review.branch.id = :branchId
            """)
    Double averageRatingByBranch(@Param("branchId") UUID branchId);

    long countByBranch_Id(UUID branchId);

    interface RatingCount {
        Integer getRating();

        Long getTotal();
    }
}
