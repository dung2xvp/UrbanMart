package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.OrderReviewRequest;
import com.haui.UrbanMart.dto.request.ProductReviewRequest;
import com.haui.UrbanMart.dto.request.ReviewType;
import com.haui.UrbanMart.dto.response.AdminReviewResponse;
import com.haui.UrbanMart.dto.response.BranchReviewPageResponse;
import com.haui.UrbanMart.dto.response.ProductReviewPageResponse;
import com.haui.UrbanMart.dto.response.ReviewCreatedResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.DeliveryType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderReview;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductReview;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.OrderItemRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.OrderReviewRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import com.haui.UrbanMart.repository.ProductReviewRepository;
import com.haui.UrbanMart.repository.ReviewSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final OrderRepository orderRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final ReviewSearchRepository reviewSearchRepository;

    @Transactional
    public ReviewCreatedResponse createOrderReview(
            UUID customerId,
            UUID orderId,
            OrderReviewRequest request
    ) {
        Order order = findCompletedCustomerOrder(customerId, orderId);
        if (orderReviewRepository.existsByOrder_Id(orderId)) {
            throw new ConflictException("Đơn hàng đã được đánh giá");
        }
        if (order.getDeliveryType() == DeliveryType.DELIVERY
                && request.deliveryRating() == null) {
            throw new BadRequestException("Đánh giá giao hàng là bắt buộc với đơn giao tận nơi");
        }
        if (order.getDeliveryType() == DeliveryType.PICKUP
                && request.deliveryRating() != null) {
            throw new BadRequestException("Đơn tự đến lấy không có đánh giá giao hàng");
        }

        OrderReview review = new OrderReview();
        review.setOrder(order);
        review.setCustomer(order.getCustomer());
        review.setBranch(order.getBranch());
        review.setDeliveryRating(request.deliveryRating());
        review.setServiceRating(request.serviceRating());
        review.setComment(blankToNull(request.comment()));
        review.setImages(request.images() == null ? List.of() : request.images());
        OrderReview saved = orderReviewRepository.save(review);
        return new ReviewCreatedResponse(
                saved.getId(), order.getId(), order.getOrderCode(), null, null,
                saved.getCreatedAt()
        );
    }

    @Transactional
    public ReviewCreatedResponse createProductReview(
            UUID customerId,
            UUID productId,
            ProductReviewRequest request
    ) {
        Order order = findCompletedCustomerOrder(customerId, request.orderId());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (!orderItemRepository.existsByOrder_IdAndProduct_Id(order.getId(), productId)) {
            throw new BadRequestException("Sản phẩm không thuộc đơn hàng này");
        }
        if (productReviewRepository.existsByOrder_IdAndProduct_Id(order.getId(), productId)) {
            throw new ConflictException("Sản phẩm trong đơn hàng đã được đánh giá");
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setOrder(order);
        review.setCustomer(order.getCustomer());
        review.setBranch(order.getBranch());
        review.setRating(request.rating());
        review.setComment(blankToNull(request.comment()));
        review.setImages(request.images() == null ? List.of() : request.images());
        ProductReview saved = productReviewRepository.save(review);
        return new ReviewCreatedResponse(
                saved.getId(), order.getId(), order.getOrderCode(), productId, true,
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public ProductReviewPageResponse getProductReviews(
            UUID productId,
            Integer rating,
            Pageable pageable
    ) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }
        Page<ProductReview> reviews = productReviewRepository.findPublicReviews(
                productId, rating, pageable
        );
        List<ProductReviewPageResponse.Item> content = reviews.getContent().stream()
                .map(review -> new ProductReviewPageResponse.Item(
                        review.getId(),
                        review.getCustomer().getFullName(),
                        null,
                        review.getRating(),
                        review.getComment(),
                        review.getImages(),
                        true,
                        review.getCreatedAt(),
                        review.getBranch().getName()
                ))
                .toList();
        Map<String, Long> spreads = new LinkedHashMap<>();
        for (int score = 5; score >= 1; score--) {
            spreads.put(Integer.toString(score), 0L);
        }
        productReviewRepository.countByRating(productId).forEach(
                item -> spreads.put(Integer.toString(item.getRating()), item.getTotal())
        );
        Double average = productReviewRepository.averageRatingByProduct(productId);
        return new ProductReviewPageResponse(
                content,
                average == null ? 0 : average,
                productReviewRepository.countByProduct_Id(productId),
                spreads,
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public BranchReviewPageResponse getBranchReviews(
            UUID accountId,
            ReviewType type,
            Integer rating,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        Branch branch = branchRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy chi nhánh của tài khoản"
                ));
        validateDateRange(from, to);
        Page<ReviewSearchRepository.Row> page = reviewSearchRepository.search(
                new ReviewSearchRepository.Filter(branch.getId(), type, rating, from, to, null),
                pageable
        );
        List<BranchReviewPageResponse.Item> content = page.getContent().stream()
                .map(row -> new BranchReviewPageResponse.Item(
                        row.id(),
                        row.reviewType(),
                        row.orderId(),
                        row.orderCode(),
                        row.customerName(),
                        maskPhone(row.customerPhone()),
                        row.productId(),
                        row.productName(),
                        row.rating(),
                        row.deliveryRating(),
                        row.serviceRating(),
                        row.comment(),
                        row.images(),
                        row.createdAt()
                ))
                .toList();
        return new BranchReviewPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                new BranchReviewPageResponse.Summary(
                        type == ReviewType.PRODUCT ? null
                                : orderReviewRepository.averageServiceRatingByBranch(branch.getId()),
                        type == ReviewType.ORDER ? null
                                : productReviewRepository.averageRatingByBranch(branch.getId()),
                        type == ReviewType.PRODUCT ? 0
                                : orderReviewRepository.countByBranch_Id(branch.getId()),
                        type == ReviewType.ORDER ? 0
                                : productReviewRepository.countByBranch_Id(branch.getId())
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAdminReviews(
            UUID branchId,
            ReviewType type,
            Integer rating,
            OffsetDateTime from,
            OffsetDateTime to,
            String keyword,
            Pageable pageable
    ) {
        validateDateRange(from, to);
        if (branchId != null && !branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh");
        }
        return reviewSearchRepository.search(
                        new ReviewSearchRepository.Filter(
                                branchId, type, rating, from, to, keyword
                        ),
                        pageable
                )
                .map(row -> new AdminReviewResponse(
                        row.id(),
                        row.branchId(),
                        row.branchName(),
                        row.reviewType(),
                        row.orderId(),
                        row.orderCode(),
                        "PRODUCT".equals(row.reviewType())
                                ? row.productName() : row.orderCode(),
                        row.productId(),
                        row.customerId(),
                        row.customerName(),
                        row.rating(),
                        row.deliveryRating(),
                        row.serviceRating(),
                        row.comment(),
                        row.images(),
                        row.createdAt()
                ));
    }

    private Order findCompletedCustomerOrder(UUID customerId, UUID orderId) {
        Order order = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn hàng đã hoàn tất");
        }
        return order;
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BadRequestException("startDate phải sớm hơn endDate");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 5) {
            return "****";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
