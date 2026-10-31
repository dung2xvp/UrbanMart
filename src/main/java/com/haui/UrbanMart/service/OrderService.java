package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.CreateOrderRequest;
import com.haui.UrbanMart.dto.request.CustomerOrderGroup;
import com.haui.UrbanMart.dto.request.OrderQuoteRequest;
import com.haui.UrbanMart.dto.response.OrderDetailResponse;
import com.haui.UrbanMart.dto.response.OrderPricingResponse;
import com.haui.UrbanMart.dto.response.OrderSummaryResponse;
import com.haui.UrbanMart.entity.*;
import com.haui.UrbanMart.event.BranchOrderCreatedEvent;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final BranchRepository branchRepository;

    private final ProductPricingService productPricingService;
    private final InventoryService inventoryService;
    private final PromotionService promotionService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getCustomerOrders(
            UUID customerId,
            CustomerOrderGroup group,
            Pageable pageable
    ) {
        Page<Order> orders = switch (group) {
            case ALL -> orderRepository.findAllByCustomer_IdOrderByCreatedAtDesc(
                    customerId,
                    pageable
            );
            case PENDING, PROCESSING, CANCELLED, RETURNED ->
                    orderRepository.findAllByCustomer_IdAndStatusInOrderByCreatedAtDesc(
                            customerId,
                            EnumSet.of(OrderStatus.valueOf(group.name())),
                            pageable
                    );
            case COMPLETED -> orderRepository.findAllByCustomer_IdAndStatusInOrderByCreatedAtDesc(
                    customerId,
                    EnumSet.of(
                            OrderStatus.DELIVERED_PENDING_CONFIRMATION,
                            OrderStatus.COMPLETED
                    ),
                    pageable
            );
            case WAITING -> orderRepository.findAllByCustomer_IdAndStatusInOrderByCreatedAtDesc(
                    customerId,
                    EnumSet.of(
                            OrderStatus.SHIPPING,
                            OrderStatus.READY_FOR_PICKUP,
                            OrderStatus.DELIVERY_FAILED
                    ),
                    pageable
            );
        };
        return orders
                .map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getCustomerOrder(UUID customerId, UUID orderId) {
        Order order = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        List<OrderItem> items =
                orderItemRepository.findAllByOrder_IdOrderByProduct_IdAsc(order.getId());
        return toCustomerOrderDetail(order, items);
    }

    @Transactional(readOnly = true)
    public OrderPricingResponse quoteOrder(UUID customerId, OrderQuoteRequest request) {
        if (customerId == null || request == null) {
            throw new BadRequestException("Thông tin báo giá không hợp lệ");
        }
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new BadRequestException("Chi nhánh không nhận đơn hàng");
        }
        Cart cart = cartRepository.findByUserIdAndBranchId(customerId, branch.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giỏ hàng"));
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        CartPricing cartPricing = calculateCartPricing(cartItems);
        BigDecimal subtotal = cartPricing.subtotal();
        PromotionService.PromotionDiscount promotionDiscount =
                promotionService.calculateDiscount(
                        branch.getId(),
                        customerId,
                        request.promotionCode(),
                        subtotal,
                        cartPricing.categorySubtotals(),
                        false
                );
        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2);
        BigDecimal total = subtotal.add(shippingFee).subtract(promotionDiscount.amount());
        return new OrderPricingResponse(
                branch.getId(),
                subtotal,
                shippingFee,
                promotionDiscount.amount(),
                total,
                promotionDiscount.code()
        );
    }

    @Transactional(readOnly = true)
    public CartVoucherContext getCartVoucherContext(UUID customerId, UUID branchId) {
        if (customerId == null || branchId == null) {
            throw new BadRequestException("Thông tin giỏ hàng không hợp lệ");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new BadRequestException("Chi nhánh không nhận đơn hàng");
        }

        Cart cart = cartRepository.findByUserIdAndBranchId(customerId, branch.getId())
                .orElse(null);
        if (cart == null) {
            return CartVoucherContext.empty();
        }
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            return CartVoucherContext.empty();
        }
        CartPricing pricing = calculateCartPricing(cartItems);
        return new CartVoucherContext(
                true,
                pricing.subtotal(),
                pricing.categorySubtotals()
        );
    }

    @Transactional
    public OrderDetailResponse confirmOrderReceived(UUID customerId, UUID orderId) {
        Order order = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng"
                ));
        if (order.getDeliveryType() == DeliveryType.PICKUP
                && order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            if (orderRepository.confirmPickupOrder(orderId, customerId) == 0) {
                throw new ConflictException(
                        "Đơn hàng chưa sẵn sàng để xác nhận đã nhận"
                );
            }
            Order pickedUpOrder = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy đơn hàng"
                    ));
            for (OrderItem item : orderItemRepository
                    .findAllByOrder_IdOrderByProduct_IdAsc(orderId)) {
                inventoryService.completePickup(
                        pickedUpOrder.getBranch().getId(),
                        item.getProduct().getId(),
                        item.getQuantity(),
                        pickedUpOrder,
                        customerId
                );
            }
            return getCustomerOrder(customerId, orderId);
        }

        int updated = orderRepository.confirmDeliveredOrder(orderId, customerId);
        if (updated == 0) {
            if (order.getStatus() != OrderStatus.DELIVERED_PENDING_CONFIRMATION) {
                throw new ConflictException(
                        "Đơn hàng chưa được ghi nhận đã bàn giao hoặc đã hoàn tất"
                );
            }
            throw new ConflictException("Không thể xác nhận đơn hàng ở trạng thái hiện tại");
        }
        return getCustomerOrder(customerId, orderId);
    }

    @Transactional
    public OrderDetailResponse createOrder(UUID customerId, CreateOrderRequest request) {
        if (customerId == null || request == null) {
            throw new BadRequestException("Thông tin tạo đơn không hợp lệ");
        }
        if (request.getClientRequestId() == null
                || request.getBranchId() == null
                || request.getDeliveryType() == null) {
            throw new BadRequestException("Thiếu thông tin checkout bắt buộc");
        }
        validateReceiveWindow(request);

        String fingerprint = createFingerprint(customerId, request);

        var existingOrder = orderRepository.findByCustomer_IdAndClientRequestId(
                customerId,
                request.getClientRequestId()
        );

        if (existingOrder.isPresent()) {
            return getExistingOrderResponse(existingOrder.get(), fingerprint);
        }

        // Serializes checkout attempts for this customer, including retries
        // that accidentally use the same request ID with a different branch.
        User customer = userRepository.findForUpdateById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người đặt"));

        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Chỉ tài khoản khách hàng mới được đặt hàng");
        }

        // Another request may have completed while this request was waiting for
        // the customer lock; recheck before reading the cart or reserving stock.
        existingOrder = orderRepository.findByCustomer_IdAndClientRequestId(
                customerId,
                request.getClientRequestId()
        );
        if (existingOrder.isPresent()) {
            return getExistingOrderResponse(existingOrder.get(), fingerprint);
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));

        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new BadRequestException("Chi nhánh không nhận đơn hàng");
        }

        Cart cart = cartRepository
                .findForUpdateByUserIdAndBranchId(customerId, branch.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giỏ hàng"));

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống");
        }

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (cartItem.getQuantity() <= 0) {
                throw new BadRequestException("Số lượng sản phẩm trong giỏ hàng không hợp lệ");
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException(
                        "Sản phẩm ngừng kinh doanh: " + product.getName()
                );
            }
        }

        Address address = resolveAddress(customerId,request);

        List<Product> products = cartItems.stream()
                .map(CartItem::getProduct)
                .toList();

        Map<UUID, ProductPrice> prices =
                productPricingService.getCurrentPrices(products);

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<UUID, BigDecimal> categorySubtotals = new HashMap<>();
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order();
        order.setCustomer(customer);
        order.setBranch(branch);
        order.setAddress(address);
        order.setOrderCode(generateOrderCode());
        order.setClientRequestId(request.getClientRequestId());
        order.setPayloadFingerprint(fingerprint);
        order.setDeliveryType(request.getDeliveryType());
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        order.setCustomerFullName(customer.getFullName());
        order.setCustomerPhone(customer.getPhone());
        order.setCustomerEmail(customer.getEmail());

        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setNote(request.getNote());

        if (request.getDeliveryType() == DeliveryType.DELIVERY) {
            order.setShippingAddress(resolveShippingAddress(address, request));
            order.setShippingLat(
                    address == null ? request.getShippingLat() : address.getLat()
            );
            order.setShippingLng(
                    address == null ? request.getShippingLng() : address.getLng()
            );
        }
        if (request.getReceiveFrom() != null) {
            order.setReceiveFrom(request.getReceiveFrom());
            order.setReceiveTo(request.getReceiveTo());
        }

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductPrice price = prices.get(product.getId());

            if (price == null) {
                throw new IllegalStateException(
                        "Không tính được giá cho sản phẩm: " + product.getId()
                );
            }

            BigDecimal lineTotal = price.price()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setSku(product.getSku());
            orderItem.setImageUrl(product.getImageUrl());
            orderItem.setUnit(product.getUnit());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(price.price());
            orderItem.setTotalPrice(lineTotal);

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
            if (product.getCategory() != null) {
                categorySubtotals.merge(
                        product.getCategory().getId(),
                        lineTotal,
                        BigDecimal::add
                );
            }
        }

        order.setSubtotal(subtotal);
        order.setShippingFee(BigDecimal.ZERO);
        PromotionService.PromotionDiscount promotionDiscount =
                promotionService.calculateDiscount(
                        branch.getId(),
                        customerId,
                        request.getPromotionCode(),
                        subtotal,
                        categorySubtotals,
                        true
                );
        order.setDiscountAmount(promotionDiscount.amount());
        order.setTotalAmount(subtotal.add(order.getShippingFee())
                .subtract(promotionDiscount.amount()));

        order = orderRepository.saveAndFlush(order);
        promotionService.attachToOrder(order, customer, promotionDiscount);

        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        orderItems = orderItemRepository.saveAllAndFlush(orderItems);

        OrderDetailResponse response = OrderDetailResponse.from(
                order, orderItems, promotionDiscount.code()
        );

        for (OrderItem item : orderItems) {
            inventoryService.reserve(
                    branch.getId(),
                    item.getProduct().getId(),
                    item.getQuantity(),
                    order.getId(),
                    null
            );
        }

        cartItemRepository.deleteAllByCartId(cart.getId());

        notificationService.createOrderNotification(
                order,
                "Đã nhận được đơn hàng " + order.getOrderCode(),
                "Đơn hàng của bạn đã được gửi đến chi nhánh và đang chờ xác nhận."
        );
        if (branch.getAccount() != null) {
            eventPublisher.publishEvent(new BranchOrderCreatedEvent(
                    branch.getAccount().getId(),
                    order.getId(),
                    order.getOrderCode(),
                    branch.getId()
            ));
        }

        return response;
    }

    private OrderDetailResponse getExistingOrderResponse(
            Order order,
            String fingerprint
    ) {
        if (!order.getPayloadFingerprint().equals(fingerprint)) {
            throw new ConflictException(
                    "Mã yêu cầu đã được dùng với thông tin checkout khác"
            );
        }

        List<OrderItem> existingItems =
                orderItemRepository.findAllByOrder_IdOrderByProduct_IdAsc(order.getId());
        return toCustomerOrderDetail(order, existingItems);
    }

    private Address resolveAddress(UUID customerId, CreateOrderRequest request) {
        if (request.getDeliveryType() == DeliveryType.PICKUP) {
            if (request.getAddressId() != null
                    || request.getShippingAddress() != null
                    || request.getShippingLat() != null
                    || request.getShippingLng() != null) {
                throw new BadRequestException(
                        "Đơn nhận tại chi nhánh không được có địa chỉ giao hàng"
                );
            }
            return null;
        }

        if (request.getDeliveryType() != DeliveryType.DELIVERY) {
            throw new BadRequestException("Hình thức nhận hàng không hợp lệ");
        }
        if (request.getReceiveFrom() != null || request.getReceiveTo() != null) {
            throw new BadRequestException(
                    "Thời gian nhận tại chi nhánh chỉ áp dụng cho đơn tự lấy"
            );
        }

        if (request.getAddressId() != null) {
            if (request.getShippingAddress() != null
                    || request.getShippingLat() != null
                    || request.getShippingLng() != null) {
                throw new BadRequestException(
                        "Chỉ chọn địa chỉ đã lưu hoặc nhập địa chỉ mới"
                );
            }

            Address address = addressRepository
                    .findByIdAndUserId(request.getAddressId(), customerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy địa chỉ của tài khoản"
                    ));

            if (address.getLat() == null || address.getLng() == null) {
                throw new BadRequestException(
                        "Địa chỉ đã lưu chưa có tọa độ giao hàng"
                );
            }

            return address;
        }

        if (request.getShippingAddress() == null
                || request.getShippingAddress().isBlank()
                || request.getShippingLat() == null
                || request.getShippingLng() == null) {
            throw new BadRequestException(
                    "Giao hàng cần địa chỉ và tọa độ nhận hàng"
            );
        }

        return null;
    }

    private String resolveShippingAddress(
            Address address,
            CreateOrderRequest request
    ) {
        if (address == null) {
            return request.getShippingAddress().trim();
        }

        return Stream.of(
                        address.getLine(),
                        address.getWard(),
                        address.getDistrict(),
                        address.getCity()
                )
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String generateOrderCode() {
        // "UM" + 30 ký tự hex, tổng chiều dài 32 ký tự.
        return "UM" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 30)
                .toUpperCase();
    }

    private String createFingerprint(UUID customerId, CreateOrderRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            updateDigest(digest, customerId.toString());
            updateDigest(digest, request.getBranchId().toString());
            updateDigest(digest, request.getDeliveryType().name());
            updateDigest(digest, request.getAddressId() == null
                    ? null : request.getAddressId().toString());
            updateDigest(digest, request.getRecipientName());
            updateDigest(digest, request.getRecipientPhone());
            updateDigest(digest, request.getShippingAddress());
            updateDigest(digest, decimalToString(request.getShippingLat()));
            updateDigest(digest, decimalToString(request.getShippingLng()));
            updateDigest(digest, request.getNote());
            if (request.getPromotionCode() != null
                    && !request.getPromotionCode().isBlank()) {
                updateDigest(
                        digest,
                        request.getPromotionCode().trim().toUpperCase(Locale.ROOT)
                );
            }
            if (request.getReceiveFrom() != null || request.getReceiveTo() != null) {
                updateDigest(digest, request.getReceiveFrom() == null
                        ? null : request.getReceiveFrom().toString());
                updateDigest(digest, request.getReceiveTo() == null
                        ? null : request.getReceiveTo().toString());
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không hỗ trợ SHA-256", exception);
        }
    }

    private void updateDigest(MessageDigest digest, String value) {
        if (value == null) {
            digest.update((byte) 0);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) 1);
        digest.update(ByteBuffer.allocate(Integer.BYTES)
                .putInt(bytes.length)
                .array());
        digest.update(bytes);
    }

    private String decimalToString(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private void validateReceiveWindow(CreateOrderRequest request) {
        OffsetDateTime receiveFrom = request.getReceiveFrom();
        OffsetDateTime receiveTo = request.getReceiveTo();
        if (receiveFrom == null && receiveTo == null) {
            return;
        }
        if (receiveFrom == null || receiveTo == null) {
            throw new BadRequestException(
                    "Khung giờ nhận hàng cần có cả thời gian bắt đầu và kết thúc"
            );
        }
        if (request.getDeliveryType() != DeliveryType.PICKUP) {
            throw new BadRequestException(
                    "Chỉ đơn tự lấy mới được chọn khung giờ nhận hàng"
            );
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!receiveFrom.isAfter(now) || !receiveTo.isAfter(receiveFrom)) {
            throw new BadRequestException(
                    "Khung giờ nhận phải nằm trong tương lai và thời gian kết thúc phải sau bắt đầu"
            );
        }
    }

    private CartPricing calculateCartPricing(List<CartItem> cartItems) {
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng đang trống");
        }
        List<Product> products = cartItems.stream()
                .map(CartItem::getProduct)
                .toList();
        Map<UUID, ProductPrice> prices = productPricingService.getCurrentPrices(products);
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<UUID, BigDecimal> categorySubtotals = new HashMap<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() <= 0) {
                throw new BadRequestException(
                        "Số lượng sản phẩm trong giỏ hàng không hợp lệ"
                );
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException(
                        "Sản phẩm ngừng kinh doanh: " + product.getName()
                );
            }
            ProductPrice price = prices.get(product.getId());
            if (price == null) {
                throw new IllegalStateException(
                        "Không tính được giá cho sản phẩm: " + product.getId()
                );
            }
            BigDecimal lineTotal = price.price()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            if (product.getCategory() != null) {
                categorySubtotals.merge(
                        product.getCategory().getId(),
                        lineTotal,
                        BigDecimal::add
                );
            }
        }
        return new CartPricing(subtotal, categorySubtotals);
    }

    private record CartPricing(
            BigDecimal subtotal,
            Map<UUID, BigDecimal> categorySubtotals
    ) {
    }

    public record CartVoucherContext(
            boolean hasItems,
            BigDecimal subtotal,
            Map<UUID, BigDecimal> categorySubtotals
    ) {
        private static CartVoucherContext empty() {
            return new CartVoucherContext(false, BigDecimal.ZERO, Map.of());
        }
    }

    private OrderDetailResponse toCustomerOrderDetail(
            Order order,
            List<OrderItem> items
    ) {
        String promotionCode = orderPromotionRepository
                .findFirstByOrder_IdOrderByCreatedAtAsc(order.getId())
                .map(redemption -> redemption.getPromotion().getCode())
                .orElse(null);
        return OrderDetailResponse.from(order, items, promotionCode);
    }
}