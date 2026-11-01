package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.OrderItem;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findAllByOrder_IdOrderByProduct_IdAsc(UUID orderId);

    @Query("""
            SELECT item.product.id AS productId,
                   SUM(item.quantity) AS quantitySold
            FROM OrderItem item
            WHERE item.order.branch.id = :branchId
              AND item.order.status = :orderStatus
              AND item.product.status = :productStatus
            GROUP BY item.product.id
            ORDER BY SUM(item.quantity) DESC, item.product.id ASC
            """)
    List<BestSellingProduct> findBestSellingProducts(
            @Param("branchId") UUID branchId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("productStatus") ProductStatus productStatus,
            Pageable pageable
    );

    interface BestSellingProduct {
        UUID getProductId();

        Long getQuantitySold();
    }
}
