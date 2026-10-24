package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.OrderPromotion;
import com.haui.UrbanMart.entity.OrderPromotionStatus;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionType;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.PromotionUserUsageId;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.repository.OrderPromotionRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.PromotionRepository;
import com.haui.UrbanMart.repository.PromotionUserUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionUserUsageRepository promotionUserUsageRepository;
    private final OrderPromotionRepository orderPromotionRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;

    private static final ZoneId BUSINESS_TIME_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional
    public PromotionDiscount calculateDiscount(
            UUID branchId,
            UUID customerId,
            String code,
            BigDecimal subtotal,
            Map<UUID, BigDecimal> categorySubtotals,
            boolean reserveUsage
    ) {
        if (code == null || code.isBlank()) {
            return new PromotionDiscount(null, BigDecimal.ZERO);
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        Promotion promotion = (reserveUsage
                ? promotionRepository.findByCodeForUpdate(normalizedCode)
                : promotionRepository.findByCodeIgnoreCase(normalizedCode))
                .orElseThrow(() -> new BadRequestException("Mã voucher không hợp lệ"));

        validatePromotion(promotion, branchId, customerId, subtotal);
        BigDecimal qualifyingSubtotal = getQualifyingSubtotal(
                promotion,
                subtotal,
                categorySubtotals
        );
        BigDecimal discount = calculateAmount(promotion, qualifyingSubtotal);

        if (reserveUsage) {
            if (promotion.getUsageLimit() != null
                    && promotion.getUsedCount() >= promotion.getUsageLimit()) {
                throw new ConflictException("Voucher đã hết lượt sử dụng");
            }
            int customerUsage = getCustomerUsage(promotion.getId(), customerId);
            if (customerUsage >= promotion.getPerUserLimit()) {
                throw new BadRequestException("Bạn đã sử dụng hết lượt của voucher này");
            }

            promotion.setUsedCount(promotion.getUsedCount() + 1);
            int userUsageUpdated = promotionUserUsageRepository
                    .incrementUserUsageIfAvailable(
                            promotion.getId(),
                            customerId,
                            promotion.getPerUserLimit()
                    );
            if (userUsageUpdated != 1) {
                throw new ConflictException("Voucher đã đạt giới hạn sử dụng của bạn");
            }
        }

        return new PromotionDiscount(promotion, discount);
    }

    @Transactional
    public void attachToOrder(
            Order order,
            User customer,
            PromotionDiscount discount
    ) {
        if (discount.promotion() == null) {
            return;
        }
        OrderPromotion orderPromotion = new OrderPromotion();
        orderPromotion.setOrder(order);
        orderPromotion.setPromotion(discount.promotion());
        orderPromotion.setCustomer(customer);
        orderPromotion.setDiscountAmount(discount.amount());
        orderPromotion.setStatus(OrderPromotionStatus.APPLIED);
        orderPromotionRepository.save(orderPromotion);
    }

    @Transactional
    public void releaseForCancelledOrder(UUID orderId) {
        List<Redemption> redemptions = orderPromotionRepository
                .findAllByOrder_IdAndStatus(orderId, OrderPromotionStatus.APPLIED)
                .stream()
                .map(redemption -> new Redemption(
                        redemption.getPromotion().getId(),
                        redemption.getCustomer().getId()
                ))
                .toList();

        if (redemptions.isEmpty()) {
            return;
        }
        int released = orderPromotionRepository.releaseAppliedPromotions(
                orderId,
                OffsetDateTime.now()
        );
        if (released != redemptions.size()) {
            throw new ConflictException("Không thể giải phóng lượt sử dụng voucher");
        }
        for (Redemption redemption : redemptions) {
            if (promotionRepository.decrementUsageIfPositive(redemption.promotionId()) != 1
                    || promotionUserUsageRepository.decrementUserUsageIfPositive(
                            redemption.promotionId(),
                            redemption.customerId()
                    ) != 1) {
                throw new ConflictException("Không thể hoàn lại lượt sử dụng voucher");
            }
        }
    }

    private void validatePromotion(
            Promotion promotion,
            UUID branchId,
            UUID customerId,
            BigDecimal subtotal
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        if (!promotion.isActive()
                || now.isBefore(promotion.getStartsAt())
                || !now.isBefore(promotion.getEndsAt())) {
            throw new BadRequestException("Voucher chưa có hiệu lực hoặc đã hết hạn");
        }
        if (promotion.getBranch() != null
                && !promotion.getBranch().getId().equals(branchId)) {
            throw new BadRequestException("Voucher không áp dụng cho chi nhánh này");
        }
        if (subtotal.compareTo(promotion.getMinOrderValue()) < 0) {
            throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu của voucher");
        }
        if (promotion.getUsageLimit() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            throw new BadRequestException("Voucher đã hết lượt sử dụng");
        }
        if (getCustomerUsage(promotion.getId(), customerId) >= promotion.getPerUserLimit()) {
            throw new BadRequestException("Bạn đã sử dụng hết lượt của voucher này");
        }
        if (promotion.isFirstOrder()
                && orderRepository.existsByCustomer_IdAndStatus(
                        customerId,
                        OrderStatus.COMPLETED
                )) {
            throw new BadRequestException(
                    "Voucher chỉ áp dụng cho đơn hàng đầu tiên đã hoàn tất"
            );
        }
        if (!promotion.getSpecialDaysOfMonth().isEmpty()
                && !promotion.getSpecialDaysOfMonth().contains(
                        OffsetDateTime.now(BUSINESS_TIME_ZONE).getDayOfMonth()
                )) {
            throw new BadRequestException(
                    "Voucher chỉ áp dụng vào các ngày đặc biệt đã quy định"
            );
        }
    }

    private BigDecimal getQualifyingSubtotal(
            Promotion promotion,
            BigDecimal subtotal,
            Map<UUID, BigDecimal> categorySubtotals
    ) {
        if (promotion.getApplicableCategories().isEmpty()) {
            return subtotal;
        }

        Set<UUID> eligibleCategoryIds = new HashSet<>();
        promotion.getApplicableCategories().forEach(category ->
                eligibleCategoryIds.addAll(
                        categoryRepository.findSelfAndDescendantIds(category.getId())
                )
        );
        BigDecimal qualifyingSubtotal = categorySubtotals.entrySet().stream()
                .filter(entry -> eligibleCategoryIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (qualifyingSubtotal.signum() <= 0) {
            throw new BadRequestException(
                    "Giỏ hàng không có sản phẩm thuộc danh mục áp dụng voucher"
            );
        }
        return qualifyingSubtotal.min(subtotal);
    }

    private int getCustomerUsage(UUID promotionId, UUID customerId) {
        PromotionUserUsageId id = new PromotionUserUsageId();
        id.setPromotionId(promotionId);
        id.setCustomerId(customerId);
        return promotionUserUsageRepository.findById(id)
                .map(usage -> usage.getUsedCount())
                .orElse(0);
    }

    private BigDecimal calculateAmount(Promotion promotion, BigDecimal subtotal) {
        BigDecimal amount;
        if (promotion.getType() == PromotionType.PERCENT) {
            amount = subtotal.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promotion.getMaxDiscountAmount() != null) {
                amount = amount.min(promotion.getMaxDiscountAmount());
            }
        } else {
            amount = promotion.getDiscountValue().min(subtotal);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public record PromotionDiscount(Promotion promotion, BigDecimal amount) {
        public String code() {
            return promotion == null ? null : promotion.getCode();
        }
    }

    private record Redemption(UUID promotionId, UUID customerId) {
    }
}
