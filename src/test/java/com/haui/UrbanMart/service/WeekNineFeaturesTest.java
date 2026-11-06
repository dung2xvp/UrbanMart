package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.IncompatibilityCheckRequest;
import com.haui.UrbanMart.dto.request.ProductReviewRequest;
import com.haui.UrbanMart.entity.FoodIncompatibility;
import com.haui.UrbanMart.entity.FoodTag;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.FoodIncompatibilityRepository;
import com.haui.UrbanMart.repository.FoodTagRepository;
import com.haui.UrbanMart.repository.OrderItemRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.OrderReviewRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import com.haui.UrbanMart.repository.ProductReviewRepository;
import com.haui.UrbanMart.repository.ReviewSearchRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeekNineFeaturesTest {

    @Test
    void checksConflictingTagsAcrossProducts() {
        UUID seafoodId = UUID.randomUUID();
        UUID citrusId = UUID.randomUUID();
        Product seafood = product(seafoodId, "HAI_SAN");
        Product citrus = product(citrusId, "VITAMIN_C");
        FoodIncompatibility rule = new FoodIncompatibility();
        rule.setId(UUID.randomUUID());
        rule.setTagA("HAI_SAN");
        rule.setTagB("VITAMIN_C");

        ProductRepository products = mock(ProductRepository.class);
        FoodIncompatibilityRepository rules = mock(FoodIncompatibilityRepository.class);
        when(products.findWithFoodTagsByIdIn(List.of(seafoodId, citrusId)))
                .thenReturn(List.of(seafood, citrus));
        when(rules.findActiveWithinTags(Set.of("HAI_SAN", "VITAMIN_C")))
                .thenReturn(List.of(rule));

        FoodIncompatibilityService service = new FoodIncompatibilityService(
                rules, mock(FoodTagRepository.class), products
        );
        var response = service.checkProducts(
                new IncompatibilityCheckRequest(List.of(seafoodId, citrusId))
        );

        assertTrue(response.hasConflict());
        assertEquals(1, response.warnings().size());
        assertEquals(2, response.warnings().getFirst().conflictingProducts().size());
    }

    @Test
    void rejectsProductReviewWhenOrderIsNotCompleted() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);

        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findByIdAndCustomer_Id(orderId, customerId))
                .thenReturn(Optional.of(order));
        ProductReviewRepository reviews = mock(ProductReviewRepository.class);
        ReviewService service = new ReviewService(
                orders,
                mock(OrderReviewRepository.class),
                reviews,
                mock(ProductRepository.class),
                mock(OrderItemRepository.class),
                mock(BranchRepository.class),
                mock(ReviewSearchRepository.class)
        );

        assertThrows(BadRequestException.class, () -> service.createProductReview(
                customerId,
                UUID.randomUUID(),
                new ProductReviewRequest(orderId, 5, "Tốt", List.of())
        ));
        verify(reviews, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Product product(UUID id, String tagCode) {
        FoodTag tag = new FoodTag();
        tag.setCode(tagCode);
        Product product = new Product();
        product.setId(id);
        product.setName(tagCode);
        product.setStatus(ProductStatus.ACTIVE);
        product.setFoodTags(Set.of(tag));
        return product;
    }
}
