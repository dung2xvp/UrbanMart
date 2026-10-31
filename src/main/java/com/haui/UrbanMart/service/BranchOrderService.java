package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.DeliveryFailedRequest;
import com.haui.UrbanMart.dto.request.BranchCancelOrderRequest;
import com.haui.UrbanMart.dto.response.OrderDetailResponse;
import com.haui.UrbanMart.dto.response.OrderSummaryResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.DeliveryType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderItem;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.OrderItemRepository;
import com.haui.UrbanMart.repository.OrderPromotionRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

@Service
@RequiredArgsConstructor
public class BranchOrderService {
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final InventoryService inventoryService;
    private final PromotionService promotionService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(
            UUID branchId,
            UUID accountId,
            OrderStatus status,
            Pageable pageable
    ) {
        requireBranchForAccount(branchId, accountId);
        Page<Order> orders = status == null
                ? orderRepository.findAllByBranch_IdOrderByCreatedAtDesc(branchId, pageable)
                : orderRepository.findAllByBranch_IdAndStatusOrderByCreatedAtDesc(
                        branchId, status, pageable
                );
        return orders.map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(UUID branchId, UUID accountId, UUID orderId) {
        Order order = requireOrder(branchId, accountId, orderId);
        return toDetail(order);
    }

    @Transactional
    public OrderDetailResponse confirmOrder(UUID branchId, UUID accountId, UUID orderId) {
        requireOrder(branchId, accountId, orderId);
        OrderDetailResponse response = updateOrder(
                branchId,
                orderId,
                () -> orderRepository.confirmPendingOrder(orderId, branchId),
                "Chỉ đơn đang chờ xác nhận mới có thể xác nhận"
        );
        notifyCustomer(
                branchId,
                orderId,
                "Đơn hàng đã được xác nhận",
                "Chi nhánh đã xác nhận đơn hàng và bắt đầu chuẩn bị."
        );
        return response;
    }

    @Transactional
    public OrderDetailResponse cancelOrder(
            UUID branchId,
            UUID accountId,
            UUID orderId,
            BranchCancelOrderRequest request
    ) {
        Order order = requireOrder(branchId, accountId, orderId);
        if (orderRepository.cancelBeforeDispatch(
                orderId,
                branchId,
                "BRANCH",
                request.reason().trim()
        ) == 0) {
            throw new ConflictException(
                    "Chỉ đơn đang chờ xác nhận hoặc đang chuẩn bị mới có thể hủy"
            );
        }
        for (OrderItem item : orderItemRepository
                .findAllByOrder_IdOrderByProduct_IdAsc(orderId)) {
            inventoryService.release(
                    branchId,
                    item.getProduct().getId(),
                    item.getQuantity(),
                    orderId,
                    accountId
            );
        }
        promotionService.releaseForCancelledOrder(orderId);
        Order cancelledOrder = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        notificationService.createOrderNotification(
                cancelledOrder,
                "Đơn hàng đã bị hủy",
                "Chi nhánh đã hủy đơn hàng " + cancelledOrder.getOrderCode() + "."
        );
        return toDetail(cancelledOrder);
    }

    @Transactional
    public OrderDetailResponse completePreparation(
            UUID branchId,
            UUID accountId,
            UUID orderId
    ) {
        Order order = requireOrder(branchId, accountId, orderId);
        IntSupplier transition;
        String error;
        if (order.getDeliveryType() == DeliveryType.DELIVERY) {
            transition = () -> orderRepository.dispatchOrder(orderId, branchId);
            error = "Chỉ đơn giao hàng đang chuẩn bị mới có thể chuyển sang chờ giao";
        } else {
            transition = () -> orderRepository.markReadyForPickup(orderId, branchId);
            error = "Chỉ đơn tự lấy đang chuẩn bị mới có thể chuyển sang chờ nhận";
        }
        if (transition.getAsInt() == 0) {
            throw new ConflictException(error);
        }
        Order preparedOrder = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (preparedOrder.getDeliveryType() == DeliveryType.DELIVERY) {
            for (OrderItem item : orderItemRepository
                    .findAllByOrder_IdOrderByProduct_IdAsc(orderId)) {
                inventoryService.dispatchDelivery(
                        branchId,
                        item.getProduct().getId(),
                        item.getQuantity(),
                        preparedOrder,
                        accountId
                );
            }
        }
        notificationService.createOrderNotification(
                preparedOrder,
                preparedOrder.getDeliveryType() == DeliveryType.DELIVERY
                        ? "Đơn hàng đang được giao"
                        : "Đơn hàng đã sẵn sàng nhận",
                preparedOrder.getDeliveryType() == DeliveryType.DELIVERY
                        ? "Đơn hàng " + preparedOrder.getOrderCode()
                                + " đã được chuẩn bị và đang trên đường giao đến bạn."
                        : "Đơn hàng " + preparedOrder.getOrderCode()
                                + " đã sẵn sàng tại chi nhánh."
        );
        return toDetail(preparedOrder);
    }

    @Transactional
    public OrderDetailResponse handOverOrder(
            UUID branchId,
            UUID accountId,
            UUID orderId
    ) {
        requireOrder(branchId, accountId, orderId);
        if (orderRepository.markOrderHandedOver(orderId, branchId, accountId) == 0) {
            throw new ConflictException("Đơn hàng chưa ở trạng thái có thể bàn giao");
        }
        Order order = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (order.getDeliveryType() == DeliveryType.PICKUP) {
            for (OrderItem item : orderItemRepository
                    .findAllByOrder_IdOrderByProduct_IdAsc(orderId)) {
                inventoryService.completePickup(
                        branchId,
                        item.getProduct().getId(),
                        item.getQuantity(),
                        order,
                        accountId
                );
            }
        }
        notificationService.createOrderNotification(
                order,
                "Đơn hàng đã được bàn giao",
                "Đơn hàng " + order.getOrderCode()
                        + " đã được bàn giao. Vui lòng xác nhận khi nhận hàng."
        );
        return toDetail(order);
    }

    @Transactional
    public OrderDetailResponse markDeliveryFailed(
            UUID branchId,
            UUID accountId,
            UUID orderId,
            DeliveryFailedRequest request
    ) {
        requireOrder(branchId, accountId, orderId);
        OrderDetailResponse response = updateOrder(
                branchId,
                orderId,
                () -> orderRepository.markDeliveryFailed(
                        orderId, branchId, request.reason().trim()
                ),
                "Chỉ đơn đang giao mới có thể ghi nhận giao thất bại"
        );
        notifyCustomer(
                branchId,
                orderId,
                "Giao hàng chưa thành công",
                "Đơn hàng chưa thể giao thành công. Chi nhánh sẽ liên hệ để hỗ trợ."
        );
        return response;
    }

    @Transactional
    public OrderDetailResponse retryDelivery(UUID branchId, UUID accountId, UUID orderId) {
        requireOrder(branchId, accountId, orderId);
        OrderDetailResponse response = updateOrder(
                branchId,
                orderId,
                () -> orderRepository.retryDelivery(orderId, branchId),
                "Chỉ đơn giao thất bại mới có thể giao lại"
        );
        notifyCustomer(
                branchId,
                orderId,
                "Đơn hàng đang được giao lại",
                "Đơn hàng " + response.getOrderCode() + " đang được giao lại."
        );
        return response;
    }

    private void notifyCustomer(
            UUID branchId,
            UUID orderId,
            String title,
            String content
    ) {
        Order order = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        notificationService.createOrderNotification(order, title, content);
    }

    private OrderDetailResponse updateOrder(
            UUID branchId,
            UUID orderId,
            IntSupplier transition,
            String conflictMessage
    ) {
        if (transition.getAsInt() == 0) {
            throw new ConflictException(conflictMessage);
        }
        Order order = orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return toDetail(order);
    }

    private Order requireOrder(UUID branchId, UUID accountId, UUID orderId) {
        requireBranchForAccount(branchId, accountId);
        return orderRepository.findByIdAndBranch_Id(orderId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    private Branch requireBranchForAccount(UUID branchId, UUID accountId) {
        Branch branch = branchRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Tài khoản không được gắn với chi nhánh"
                ));
        if (!branch.getId().equals(branchId)) {
            throw new AccessDeniedException("Không có quyền truy cập chi nhánh này");
        }
        return branch;
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderItem> items =
                orderItemRepository.findAllByOrder_IdOrderByProduct_IdAsc(order.getId());
        String promotionCode = orderPromotionRepository
                .findFirstByOrder_IdOrderByCreatedAtAsc(order.getId())
                .map(redemption -> redemption.getPromotion().getCode())
                .orElse(null);
        return OrderDetailResponse.from(order, items, promotionCode);
    }
}
