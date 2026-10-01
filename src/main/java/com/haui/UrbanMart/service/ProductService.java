package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.ProductResponse;
import com.haui.UrbanMart.entity.*;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.ProductListSort;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPricingService productPricingService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(
            UUID branchId,
            String keyword,
            UUID categoryId,
            boolean includeDescendants,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            ProductListSort sort,
            Pageable pageable
    ) {
        return searchProducts(
                branchId,
                keyword,
                categoryId,
                includeDescendants,
                brandIds,
                minPrice,
                maxPrice,
                inStock,
                false,
                false,
                sort,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPromotions(
            UUID branchId,
            String keyword,
            UUID categoryId,
            boolean includeDescendants,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            ProductListSort sort,
            Pageable pageable
    ) {
        return searchProducts(
                branchId,
                keyword,
                categoryId,
                includeDescendants,
                brandIds,
                minPrice,
                maxPrice,
                inStock,
                true,
                false,
                sort,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getDailyFresh(
            UUID branchId,
            String keyword,
            UUID categoryId,
            boolean includeDescendants,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            ProductListSort sort,
            Pageable pageable
    ) {
        return searchProducts(
                branchId,
                keyword,
                categoryId,
                includeDescendants,
                brandIds,
                minPrice,
                maxPrice,
                inStock,
                false,
                true,
                sort,
                pageable
        );
    }

    private Page<ProductResponse> searchProducts(
            UUID branchId,
            String keyword,
            UUID categoryId,
            boolean includeDescendants,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            boolean promotionsOnly,
            boolean dailyFreshOnly,
            ProductListSort sort,
            Pageable pageable
    ) {
        validateSellingBranch(branchId);

        String normalizedKeyword =
                keyword == null || keyword.isBlank()
                        ? null
                        : keyword.trim();

        List<UUID> categoryIds = resolveCategoryIds(
                categoryId,
                includeDescendants
        );
        Page<BranchInventory> inventories = branchInventoryRepository.searchProducts(
                branchId,
                ProductStatus.ACTIVE,
                normalizedKeyword,
                categoryIds,
                brandIds,
                minPrice,
                maxPrice,
                inStock,
                promotionsOnly,
                dailyFreshOnly,
                sort,
                pageable
        );
        List<Product> products = inventories.getContent().stream()
                .map(BranchInventory::getProduct)
                .toList();
        Map<UUID, ProductPrice> prices =
                productPricingService.getCurrentPrices(products);

        return inventories.map(inventory -> ProductResponse.from(
                inventory.getProduct(),
                inventory.getStockQuantity(),
                prices.get(inventory.getProduct().getId())
        ));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(
            UUID branchId,
            UUID categoryId,
            boolean includeDescendants,
            String keyword,
            List<UUID> brandIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            ProductListSort sort,
            Pageable pageable
    ) {
        return getProducts(
                branchId,
                keyword,
                categoryId,
                includeDescendants,
                brandIds,
                minPrice,
                maxPrice,
                inStock,
                sort,
                pageable
        );
    }

    private List<UUID> resolveCategoryIds(
            UUID categoryId,
            boolean includeDescendants
    ) {
        if (categoryId == null) {
            return null;
        }
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục");
        }
        return includeDescendants
                ? categoryRepository.findSelfAndDescendantIds(categoryId)
                : List.of(categoryId);
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
        return ProductResponse.from(
                product,
                inventory.getStockQuantity(),
                productPricingService.getCurrentPrice(product)
        );
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