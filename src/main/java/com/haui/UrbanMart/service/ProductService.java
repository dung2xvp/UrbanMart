package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.ProductResponse;
import com.haui.UrbanMart.entity.*;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(
            UUID branchId, String keyword, Pageable pageable
    ) {
        validateSellingBranch(branchId);

        String normalizedKeyword =
                keyword == null || keyword.isBlank()
                        ? null
                        : keyword.trim();

        return branchInventoryRepository.searchProducts(
                branchId,
                ProductStatus.ACTIVE,
                normalizedKeyword,
                pageable
        ).map(inventory -> ProductResponse.from(
                inventory.getProduct(),
                inventory.getStockQuantity()
        ));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID branchId, UUID productId) {
        validateSellingBranch(branchId);

        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm tại chi nhánh"
                ));

        Product product = inventory.getProduct();

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy sản phẩm đang kinh doanh"
            );
        }
        return ProductResponse.from(product, inventory.getStockQuantity());
    }
    private void validateSellingBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay chi nhanh"
                ));

        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new ResourceNotFoundException(
                    "Khong tim thay chi nhanh dang ban hang"
            );
        }
    }
}