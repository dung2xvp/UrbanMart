package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.OrderReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderReviewRepository extends JpaRepository<OrderReview, UUID> {

    boolean existsByOrder_Id(UUID orderId);

    @Query("""
            SELECT AVG(review.serviceRating)
            FROM OrderReview review
            WHERE review.branch.id = :branchId
            """)
    Double averageServiceRatingByBranch(@Param("branchId") UUID branchId);

    long countByBranch_Id(UUID branchId);
}
