package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"branch"})
    Page<Order> findAllByCustomer_IdOrderByCreatedAtDesc(
            UUID customerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch"})
    Page<Order> findAllByCustomer_IdAndStatusInOrderByCreatedAtDesc(
            UUID customerId,
            Set<OrderStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch"})
    Optional<Order> findByIdAndCustomer_Id(UUID orderId, UUID customerId);

    Optional<Order> findByCustomer_IdAndClientRequestId(
            UUID customerId,
            UUID clientRequestId
    );

    @EntityGraph(attributePaths = {"branch"})
    Optional<Order> findByIdAndBranch_Id(UUID orderId, UUID branchId);

    @EntityGraph(attributePaths = {"branch"})
    Page<Order> findAllByBranch_IdOrderByCreatedAtDesc(
            UUID branchId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch"})
    Page<Order> findAllByBranch_IdAndStatusOrderByCreatedAtDesc(
            UUID branchId,
            OrderStatus status,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'PROCESSING',
            processing_at = now(),
            updated_at = now()
        WHERE id = :orderId
        AND branch_id = :branchId
        AND status = 'PENDING'
        AND (
            payment_method = 'COD'
            OR payment_status = 'PAID'
        )
        """, nativeQuery = true)
    int confirmPendingOrder(
            @Param("orderId") UUID orderId,
            @Param("branchId") UUID branchId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE orders
        SET status = 'SHIPPING',
            shipped_at = now(),
            updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND status = 'PROCESSING'
          AND delivery_type = 'DELIVERY'
        """, nativeQuery = true)
    int dispatchOrder(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'READY_FOR_PICKUP',
            updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND status = 'PROCESSING'
          AND delivery_type = 'PICKUP'
        """, nativeQuery = true)
    int markReadyForPickup(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'DELIVERED_PENDING_CONFIRMATION',
        delivered_at = now(),
        payment_status = CASE
            WHEN payment_method = 'COD' THEN 'PAID'::payment_status
            ELSE payment_status
        END,
        paid_at = CASE
            WHEN payment_method = 'COD' THEN now()
            ELSE paid_at
        END,
        paid_confirmed_by = CASE
            WHEN payment_method = 'COD' THEN :accountId
            ELSE paid_confirmed_by
        END,
        delivery_failed_reason = NULL,
        updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND (
          (delivery_type = 'DELIVERY' AND status = 'SHIPPING')
          OR (delivery_type = 'PICKUP' AND status = 'READY_FOR_PICKUP')
          )
        """, nativeQuery = true)
    int markOrderHandedOver(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId,
        @Param("accountId") UUID accountId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'COMPLETED',
        completed_at = now(),
        updated_at = now()
        WHERE id = :orderId
          AND customer_id = :customerId
          AND status = 'DELIVERED_PENDING_CONFIRMATION'
        """, nativeQuery = true)
    int confirmDeliveredOrder(
        @Param("orderId") UUID orderId,
        @Param("customerId") UUID customerId
    );

    @Modifying
    @Query(value = """
        UPDATE orders
        SET status = 'COMPLETED',
        completed_at = now(),
        updated_at = now()
        WHERE status = 'DELIVERED_PENDING_CONFIRMATION'
          AND delivered_at <= now() - INTERVAL '72 hours'
        """, nativeQuery = true)
    int completeOrdersPastConfirmationDeadline();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'CANCELLED',
            cancelled_by = CAST(:actor AS order_actor),
            cancel_reason = :reason,
            cancelled_at = now(),
            updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND status IN ('PENDING', 'PROCESSING')
        """, nativeQuery = true)
    int cancelBeforeDispatch(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId,
        @Param("actor") String actor,
        @Param("reason") String reason
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'DELIVERY_FAILED',
            delivery_failed_reason = :reason,
            updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND status = 'SHIPPING'
        """, nativeQuery = true)
    int markDeliveryFailed(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId,
        @Param("reason") String reason
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE orders
        SET status = 'SHIPPING',
            delivery_failed_reason = NULL,
            updated_at = now()
        WHERE id = :orderId
          AND branch_id = :branchId
          AND status = 'DELIVERY_FAILED'
        """, nativeQuery = true)
    int retryDelivery(
        @Param("orderId") UUID orderId,
        @Param("branchId") UUID branchId
    );

}
