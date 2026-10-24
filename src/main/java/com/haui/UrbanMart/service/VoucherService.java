package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.VoucherWriteRequest;
import com.haui.UrbanMart.dto.response.VoucherResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.entity.Promotion;
import com.haui.UrbanMart.entity.PromotionType;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final PromotionRepository promotionRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Page<VoucherResponse> getPublicVouchers(
            String keyword,
            boolean availableOnly,
            Pageable pageable
    ) {
        return promotionRepository.findPublicVouchers(
                        normalizeKeyword(keyword),
                        availableOnly,
                        OffsetDateTime.now(),
                        pageable
                )
                .map(VoucherResponse::from);
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
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
