package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.BranchWriteRequest;
import com.haui.UrbanMart.dto.response.BranchAdminResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchAdminService {

    private static final BigDecimal DEFAULT_DELIVERY_RADIUS_KM = BigDecimal.valueOf(5);

    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Page<BranchAdminResponse> getBranches(Pageable pageable) {
        return branchRepository.findAll(pageable)
                .map(BranchAdminResponse::from);
    }

    @Transactional(readOnly = true)
    public BranchAdminResponse getBranch(UUID id) {
        return BranchAdminResponse.from(findBranch(id));
    }

    @Transactional
    public BranchAdminResponse createBranch(BranchWriteRequest request) {
        validateCoordinates(request.lat(), request.lng());

        Branch branch = new Branch();
        applyRequest(branch, request);
        if (branch.getStatus() == null) {
            branch.setStatus(BranchStatus.ACTIVE);
        }
        return BranchAdminResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchAdminResponse updateBranch(UUID id, BranchWriteRequest request) {
        validateCoordinates(request.lat(), request.lng());

        Branch branch = findBranch(id);
        applyRequest(branch, request);
        return BranchAdminResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public void closeBranch(UUID id) {
        Branch branch = findBranch(id);
        branch.setStatus(BranchStatus.CLOSED_TEMP);
    }

    private Branch findBranch(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
    }

    private void applyRequest(Branch branch, BranchWriteRequest request) {
        branch.setName(request.name().trim());
        branch.setAddress(request.address().trim());
        branch.setLat(request.lat());
        branch.setLng(request.lng());
        branch.setPhone(request.phone() == null || request.phone().isBlank()
                ? null
                : request.phone().trim());
        branch.setDeliveryRadiusKm(request.deliveryRadiusKm() == null
                ? DEFAULT_DELIVERY_RADIUS_KM
                : request.deliveryRadiusKm());
        branch.setWarehouse(Boolean.TRUE.equals(request.isWarehouse()));
        branch.setOpeningHours(request.openingHours());
        if (request.status() != null) {
            branch.setStatus(request.status());
        }
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if ((lat == null) != (lng == null)) {
            throw new BadRequestException("Vĩ độ và kinh độ phải được cung cấp cùng nhau");
        }
    }
}
