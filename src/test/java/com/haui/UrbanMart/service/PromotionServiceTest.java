package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionType;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.OrderPromotionRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.PromotionRepository;
import com.haui.UrbanMart.repository.PromotionUserUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionServiceTest {

    private PromotionRepository promotionRepository;
    private PromotionUserUsageRepository usageRepository;
    private OrderPromotionRepository orderPromotionRepository;
    private OrderRepository orderRepository;
    private CategoryRepository categoryRepository;
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        usageRepository = mock(PromotionUserUsageRepository.class);
        orderPromotionRepository = mock(OrderPromotionRepository.class);
        orderRepository = mock(OrderRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        promotionService = new PromotionService(
                promotionRepository,
                usageRepository,
                orderPromotionRepository,
                orderRepository,
                categoryRepository
        );
    }

    @Test
    void percentVoucherAppliesAfterSaleToEveryUnit() {
        UUID customerId = UUID.randomUUID();
        Promotion promotion = activePromotion(PromotionType.PERCENT);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setMaxDiscountAmount(new BigDecimal("50000"));
        when(promotionRepository.findByCodeIgnoreCase("STACK10"))
                .thenReturn(Optional.of(promotion));
        when(orderRepository.existsByCustomer_IdAndStatus(any(), any()))
                .thenReturn(false);

        var discount = promotionService.calculateDiscount(
                UUID.randomUUID(),
                customerId,
                "stack10",
                new BigDecimal("240000"),
                Map.of(),
                false
        );

        assertEquals(new BigDecimal("24000.00"), discount.amount());
    }

    @Test
    void categoryVoucherDiscountsOnlyEligibleSaleSubtotal() {
        UUID customerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID childCategoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        Promotion promotion = activePromotion(PromotionType.PERCENT);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setApplicableCategories(Set.of(category));
        when(promotionRepository.findByCodeIgnoreCase("STACK10"))
                .thenReturn(Optional.of(promotion));
        when(categoryRepository.findSelfAndDescendantIds(categoryId))
                .thenReturn(List.of(categoryId, childCategoryId));
        when(orderRepository.existsByCustomer_IdAndStatus(any(), any()))
                .thenReturn(false);

        var discount = promotionService.calculateDiscount(
                UUID.randomUUID(),
                customerId,
                "STACK10",
                new BigDecimal("300000"),
                Map.of(
                        categoryId, new BigDecimal("50000"),
                        childCategoryId, new BigDecimal("25000"),
                        UUID.randomUUID(), new BigDecimal("225000")
                ),
                false
        );

        assertEquals(new BigDecimal("7500.00"), discount.amount());
    }

    private Promotion activePromotion(PromotionType type) {
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setCode("STACK10");
        promotion.setType(type);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setMinOrderValue(new BigDecimal("100"));
        promotion.setStartsAt(OffsetDateTime.now().minusDays(1));
        promotion.setEndsAt(OffsetDateTime.now().plusDays(1));
        promotion.setPerUserLimit(1);
        promotion.setActive(true);
        return promotion;
    }
}
