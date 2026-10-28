package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.VoucherIneligibilityReason;
import com.haui.UrbanMart.dto.response.VoucherStatus;
import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionUserUsage;
import com.haui.UrbanMart.entity.PromotionUserUsageId;
import com.haui.UrbanMart.entity.PromotionType;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.PromotionRepository;
import com.haui.UrbanMart.repository.PromotionUserUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoucherServiceTest {

    private PromotionRepository promotionRepository;
    private PromotionUserUsageRepository usageRepository;
    private OrderRepository orderRepository;
    private CategoryRepository categoryRepository;
    private OrderService orderService;
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        usageRepository = mock(PromotionUserUsageRepository.class);
        orderRepository = mock(OrderRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        orderService = mock(OrderService.class);
        voucherService = new VoucherService(
                promotionRepository,
                usageRepository,
                orderRepository,
                categoryRepository,
                branchRepository,
                orderService
        );
    }

    @Test
    void reportsMinimumOrderValueWhenVoucherIsUnusedButCartIsTooSmall() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Promotion promotion = activePromotion();
        promotion.setMinOrderValue(new BigDecimal("500"));
        stubVoucherList(promotion);
        when(orderService.getCartVoucherContext(customerId, branchId))
                .thenReturn(new OrderService.CartVoucherContext(
                        true,
                        new BigDecimal("200"),
                        java.util.Map.of()
                ));

        var response = voucherService.getPublicVouchers(
                null,
                false,
                PageRequest.of(0, 10),
                customerId,
                branchId
        ).getContent().getFirst();

        assertEquals(false, response.usedByCurrentUser());
        assertEquals(VoucherStatus.NOT_ELIGIBLE, response.status());
        assertEquals(false, response.canApply());
        assertEquals(
                List.of(VoucherIneligibilityReason.MIN_ORDER_VALUE),
                response.ineligibilityReasons()
        );
    }

    @Test
    void reportsUsedWhenCustomerReachedPerUserLimit() {
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Promotion promotion = activePromotion();
        stubVoucherList(promotion);

        PromotionUserUsage usage = new PromotionUserUsage();
        PromotionUserUsageId id = new PromotionUserUsageId();
        id.setPromotionId(promotion.getId());
        id.setCustomerId(customerId);
        usage.setId(id);
        usage.setUsedCount(1);
        when(usageRepository.findAllForCustomerAndPromotions(
                eq(customerId),
                anyList()
        )).thenReturn(List.of(usage));
        when(orderService.getCartVoucherContext(customerId, branchId))
                .thenReturn(new OrderService.CartVoucherContext(
                        true,
                        new BigDecimal("1000"),
                        java.util.Map.of()
                ));

        var response = voucherService.getPublicVouchers(
                null,
                false,
                PageRequest.of(0, 10),
                customerId,
                branchId
        ).getContent().getFirst();

        assertEquals(true, response.usedByCurrentUser());
        assertEquals(1, response.userUsageCount());
        assertEquals(VoucherStatus.USED, response.status());
        assertEquals(false, response.canApply());
    }

    private void stubVoucherList(Promotion promotion) {
        when(promotionRepository.findPublicVouchers(
                anyString(),
                eq(false),
                any(OffsetDateTime.class),
                any()
        )).thenReturn(new PageImpl<>(List.of(promotion)));
        when(usageRepository.findAllForCustomerAndPromotions(any(), anyList()))
                .thenReturn(List.of());
        when(orderRepository.existsByCustomer_IdAndStatus(any(), any()))
                .thenReturn(false);
    }

    private Promotion activePromotion() {
        Promotion promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setCode("SAVE10");
        promotion.setTitle("Save 10");
        promotion.setType(PromotionType.FIXED);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setMinOrderValue(BigDecimal.ZERO);
        promotion.setStartsAt(OffsetDateTime.now().minusDays(1));
        promotion.setEndsAt(OffsetDateTime.now().plusDays(1));
        promotion.setPerUserLimit(1);
        promotion.setActive(true);
        promotion.setApplicableCategories(Set.of());
        promotion.setSpecialDaysOfMonth(Set.of());
        return promotion;
    }
}
