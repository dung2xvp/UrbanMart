package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.VoucherWriteRequest;
import com.haui.UrbanMart.dto.response.VoucherIneligibilityReason;
import com.haui.UrbanMart.dto.response.VoucherStatus;
import com.haui.UrbanMart.dto.response.VoucherResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionType;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import com.haui.UrbanMart.repository.PromotionRepository;
import com.haui.UrbanMart.repository.PromotionUserUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final PromotionRepository promotionRepository;
    private final PromotionUserUsageRepository promotionUserUsageRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final OrderService orderService;

    private static final ZoneId BUSINESS_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional(readOnly = true)
    public Page<VoucherResponse> getPublicVouchers(
            String keyword,
            boolean availableOnly,
            Pageable pageable,
            UUID customerId,
            UUID branchId
    ) {
        var promotions = promotionRepository.findPublicVouchers(
                        normalizeKeyword(keyword),
                        availableOnly,
                        OffsetDateTime.now(),
                        pageable
                );
        if (customerId == null || branchId == null) {
            return promotions.map(VoucherResponse::from);
        }

        OrderService.CartVoucherContext cartContext =
                orderService.getCartVoucherContext(customerId, branchId);
        List<UUID> promotionIds = promotions.getContent().stream()
                .map(Promotion::getId)
                .toList();
        Map<UUID, Integer> usageCounts = promotionIds.isEmpty()
                ? Map.of()
                : promotionUserUsageRepository.findAllForCustomerAndPromotions(
                                customerId,
                                promotionIds
                        ).stream()
                        .collect(Collectors.toMap(
                                usage -> usage.getId().getPromotionId(),
                                usage -> usage.getUsedCount()
                        ));
        return promotions.map(promotion -> {
            int usageCount = usageCounts.getOrDefault(promotion.getId(), 0);
            return VoucherResponse.from(
                    promotion,
                    assessEligibility(
                            promotion,
                            customerId,
                            branchId,
                            cartContext,
                            usageCount
                    )
            );
        });
    }

    private VoucherResponse.CustomerEligibility assessEligibility(
            Promotion promotion,
            UUID customerId,
            UUID branchId,
            OrderService.CartVoucherContext cartContext,
            int usageCount
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean usedByCurrentUser = usageCount > 0;
        List<VoucherIneligibilityReason> reasons = new ArrayList<>();

        if (usageCount >= promotion.getPerUserLimit()) {
            return eligibility(
                    true,
                    usageCount,
                    VoucherStatus.USED,
                    List.of(VoucherIneligibilityReason.USER_USAGE_LIMIT_REACHED)
            );
        }
        if (!now.isBefore(promotion.getEndsAt())) {
            return eligibility(
                    usedByCurrentUser,
                    usageCount,
                    VoucherStatus.EXPIRED,
                    List.of(VoucherIneligibilityReason.EXPIRED)
            );
        }
        if (now.isBefore(promotion.getStartsAt())) {
            reasons.add(VoucherIneligibilityReason.NOT_STARTED);
        }
        if (promotion.getBranch() != null
                && !promotion.getBranch().getId().equals(branchId)) {
            reasons.add(VoucherIneligibilityReason.BRANCH_MISMATCH);
        }
        if (!cartContext.hasItems()) {
            reasons.add(VoucherIneligibilityReason.EMPTY_CART);
        } else if (cartContext.subtotal().compareTo(promotion.getMinOrderValue()) < 0) {
            reasons.add(VoucherIneligibilityReason.MIN_ORDER_VALUE);
        }
        if (promotion.getUsageLimit() != null
                && promotion.getUsedCount() >= promotion.getUsageLimit()) {
            reasons.add(VoucherIneligibilityReason.GLOBAL_USAGE_LIMIT_REACHED);
        }
        if (promotion.isFirstOrder()
                && orderRepository.existsByCustomer_IdAndStatus(
                        customerId,
                        OrderStatus.COMPLETED
                )) {
            reasons.add(VoucherIneligibilityReason.FIRST_ORDER_ONLY);
        }
        if (!promotion.getSpecialDaysOfMonth().isEmpty()
                && !promotion.getSpecialDaysOfMonth().contains(
                        OffsetDateTime.now(BUSINESS_TIME_ZONE).getDayOfMonth()
                )) {
            reasons.add(VoucherIneligibilityReason.SPECIAL_DAY_ONLY);
        }
        if (cartContext.hasItems() && !promotion.getApplicableCategories().isEmpty()) {
            Set<UUID> eligibleCategoryIds = new HashSet<>();
            promotion.getApplicableCategories().forEach(category ->
                    eligibleCategoryIds.addAll(
                            categoryRepository.findSelfAndDescendantIds(category.getId())
                    )
            );
            boolean hasApplicableProducts = cartContext.categorySubtotals().entrySet().stream()
                    .anyMatch(entry -> eligibleCategoryIds.contains(entry.getKey())
                            && entry.getValue().signum() > 0);
            if (!hasApplicableProducts) {
                reasons.add(VoucherIneligibilityReason.NO_APPLICABLE_PRODUCTS);
            }
        }

        return eligibility(
                usedByCurrentUser,
                usageCount,
                reasons.isEmpty() ? VoucherStatus.AVAILABLE : VoucherStatus.NOT_ELIGIBLE,
                reasons
        );
    }

    private VoucherResponse.CustomerEligibility eligibility(
            boolean usedByCurrentUser,
            int usageCount,
            VoucherStatus status,
            List<VoucherIneligibilityReason> reasons
    ) {
        return new VoucherResponse.CustomerEligibility(
                usedByCurrentUser,
                usageCount,
                status,
                status == VoucherStatus.AVAILABLE,
                List.copyOf(reasons)
        );
    }

    @Transactional(readOnly = true)
    public VoucherResponse getPublicVoucher(UUID id) {
        Promotion promotion = getPromotion(id);
        if (!promotion.isActive()) {
            throw new ResourceNotFoundException("Không tìm thấy voucher");
        }
        return VoucherResponse.from(promotion);
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAdminVouchers(
            String keyword,
            Boolean active,
            Pageable pageable
    ) {
        return promotionRepository.searchAdminVouchers(
                        normalizeKeyword(keyword),
                        active,
                        pageable
                )
                .map(VoucherResponse::from);
    }

    @Transactional(readOnly = true)
    public VoucherResponse getAdminVoucher(UUID id) {
        return VoucherResponse.from(getPromotion(id));
    }

    @Transactional
    public VoucherResponse create(VoucherWriteRequest request) {
        validateRequest(request, null);
        String code = normalizeCode(request.code());
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Mã voucher đã tồn tại");
        }

        Promotion promotion = new Promotion();
        applyRequest(promotion, request, code);
        return VoucherResponse.from(promotionRepository.save(promotion));
    }

    @Transactional
    public VoucherResponse update(UUID id, VoucherWriteRequest request) {
        Promotion promotion = promotionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher"));
        validateRequest(request, promotion);

        String code = normalizeCode(request.code());
        if (!promotion.getCode().equalsIgnoreCase(code)
                && promotion.getUsedCount() > 0) {
            throw new ConflictException("Không thể đổi mã voucher đã được sử dụng");
        }
        if (promotionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Mã voucher đã tồn tại");
        }

        applyRequest(promotion, request, code);
        return VoucherResponse.from(promotionRepository.save(promotion));
    }

    @Transactional
    public void deactivate(UUID id) {
        Promotion promotion = promotionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher"));
        promotion.setActive(false);
    }

    private void applyRequest(
            Promotion promotion,
            VoucherWriteRequest request,
            String code
    ) {
        promotion.setCode(code);
        promotion.setTitle(request.title().trim());
        promotion.setDescription(request.description());
        promotion.setType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setMaxDiscountAmount(request.maxDiscountAmount());
        promotion.setMinOrderValue(request.minOrderAmount());
        promotion.setStartsAt(request.startDate());
        promotion.setEndsAt(request.endDate());
        promotion.setUsageLimit(request.usageLimit());
        promotion.setPerUserLimit(request.userLimit());
        promotion.setActive(request.isActive());
        promotion.setBranch(resolveBranch(request.branchId()));

        VoucherWriteRequest.Conditions conditions = request.conditions();
        Set<UUID> categoryIds = conditions == null
                || conditions.applicableCategoryIds() == null
                ? Set.of()
                : conditions.applicableCategoryIds();
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException(
                    "Một hoặc nhiều danh mục áp dụng không tồn tại"
            );
        }
        promotion.getApplicableCategories().clear();
        promotion.getApplicableCategories().addAll(categories);
        promotion.setFirstOrder(
                conditions != null && Boolean.TRUE.equals(conditions.isFirstOrder())
        );
        promotion.getSpecialDaysOfMonth().clear();
        if (conditions != null && conditions.specialDaysOfMonth() != null) {
            promotion.getSpecialDaysOfMonth().addAll(conditions.specialDaysOfMonth());
        }
    }

    private Branch resolveBranch(UUID branchId) {
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new BadRequestException("Chi nhánh áp dụng voucher không hợp lệ");
        }
        return branch;
    }

    private void validateRequest(VoucherWriteRequest request, Promotion existing) {
        if (request.startDate() != null
                && request.endDate() != null
                && !request.startDate().isBefore(request.endDate())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.discountType() == PromotionType.PERCENT
                && request.maxDiscountAmount() == null) {
            throw new BadRequestException(
                    "Voucher phần trăm cần có giá trị giảm tối đa"
            );
        }
        if (request.discountType() == PromotionType.PERCENT
                && request.discountValue() != null
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException(
                    "Phần trăm giảm giá không được vượt quá 100"
            );
        }
        if (request.usageLimit() != null
                && existing != null
                && request.usageLimit() < existing.getUsedCount()) {
            throw new BadRequestException(
                    "Giới hạn lượt dùng không được thấp hơn số lượt đã sử dụng"
            );
        }
    }

    private Promotion getPromotion(UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher"));
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? "" : keyword.trim();
    }
}
