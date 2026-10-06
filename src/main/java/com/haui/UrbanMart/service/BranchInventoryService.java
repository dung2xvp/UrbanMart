package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchInventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    @Transactional
    public BranchInventoryResponse setStock(BranchInventoryWriteRequest request) {
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new BadRequestException("Không thể cập nhật tồn kho cho chi nhánh ngừng hoạt động");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Không thể cập nhật tồn kho cho sản phẩm đã ngừng kinh doanh");
        }

        branchInventoryRepository.upsertStock(
                branch.getId(),
                product.getId(),
                request.stockQuantity()
        );

        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy tồn kho sau khi cập nhật"
                ));
        return BranchInventoryResponse.from(inventory);
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryResponse> getBranchInventory(UUID branchId) {
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Không tìm thấy chi nhánh");
        }

        return branchInventoryRepository.findAllByBranchId(branchId).stream()
                .map(BranchInventoryResponse::from)
                .toList();
    }
}
