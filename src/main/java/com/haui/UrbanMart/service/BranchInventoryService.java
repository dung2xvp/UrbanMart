package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.CategoryInventory;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.ProductInventory;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.SubGroup;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventorySummaryResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchInventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public BranchInventoryResponse setBranchStock(
            UUID branchId,
            UUID accountId,
            BranchInventoryWriteRequest request
    ) {
        Branch branch = findBranchForAccount(branchId, accountId);
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
    public BranchInventoryByCategoryResponse getInventoryByCategories(UUID branchId) {
        return buildInventoryByCategories(findBranch(branchId));
    }

    @Transactional(readOnly = true)
    public BranchInventoryByCategoryResponse getOwnInventoryByCategories(
            UUID branchId,
            UUID accountId
    ) {
        return buildInventoryByCategories(findBranchForAccount(branchId, accountId));
    }

    @Transactional(readOnly = true)
    public List<BranchInventorySummaryResponse> getBranchInventorySummaries() {
        return branchRepository.findInventorySummaries().stream()
                .map(summary -> new BranchInventorySummaryResponse(
                        summary.getBranchId(),
                        summary.getBranchName(),
                        summary.getBranchAddress(),
                        summary.getBranchPhone(),
                        summary.getTotalStock()
                ))
                .toList();
    }

    private BranchInventoryByCategoryResponse buildInventoryByCategories(Branch branch) {
        List<BranchInventory> inventories =
                branchInventoryRepository.findAllByBranchId(branch.getId());
        List<Category> categories = categoryRepository.findAll();
        Map<UUID, List<ProductInventory>> productsByCategory = new HashMap<>();
        long totalBranchStock = 0;

        for (BranchInventory inventory : inventories) {
            totalBranchStock += inventory.getStockQuantity();
            Category category = inventory.getProduct().getCategory();
            if (category != null) {
                productsByCategory.computeIfAbsent(category.getId(), ignored -> new ArrayList<>())
                        .add(new ProductInventory(
                                inventory.getId(),
                                inventory.getProduct().getId(),
                                inventory.getProduct().getName(),
                                inventory.getProduct().getSku(),
                                inventory.getProduct().getUnit(),
                                inventory.getStockQuantity(),
                                inventory.getUpdatedAt()
                        ));
            }
        }
        productsByCategory.values().forEach(products ->
                products.sort(Comparator.comparing(ProductInventory::productName)));

        Map<UUID, List<Category>> childrenByParent = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParent() == null) {
                rootCategories.add(category);
            } else {
                childrenByParent.computeIfAbsent(category.getParent().getId(), ignored -> new ArrayList<>())
                        .add(category);
            }
        }
        Comparator<Category> categoryOrder = Comparator.comparing(
                Category::getName,
                String.CASE_INSENSITIVE_ORDER
        );
        rootCategories.sort(categoryOrder);
        childrenByParent.values().forEach(children -> children.sort(categoryOrder));

        List<CategoryInventory> categoryResponses = rootCategories.stream()
                .map(root -> toCategoryInventory(root, productsByCategory, childrenByParent, true))
                .toList();
        return new BranchInventoryByCategoryResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                totalBranchStock,
                categoryResponses
        );
    }

    private CategoryInventory toCategoryInventory(
            Category category,
            Map<UUID, List<ProductInventory>> productsByCategory,
            Map<UUID, List<Category>> childrenByParent,
            boolean root
    ) {
        List<ProductInventory> directProducts =
                productsByCategory.getOrDefault(category.getId(), List.of());
        List<Category> children = childrenByParent.getOrDefault(category.getId(), List.of());
        List<SubGroup> subGroups = new ArrayList<>();
        long totalStock = directProducts.stream()
                .mapToLong(ProductInventory::stockQuantity)
                .sum();

        for (Category child : children) {
            CategoryInventory childResult =
                    toCategoryInventory(child, productsByCategory, childrenByParent, false);
            totalStock += childResult.totalStock();
            subGroups.addAll(childResult.subGroups());
        }

        if (!root && (children.isEmpty() || !directProducts.isEmpty())) {
            String parentName = category.getParent() == null
                    ? null
                    : category.getParent().getName();
            subGroups.add(new SubGroup(
                    category.getId(),
                    parentName,
                    category.getName(),
                    directProducts.size(),
                    directProducts.stream().mapToLong(ProductInventory::stockQuantity).sum(),
                    directProducts
            ));
        } else if (root && !directProducts.isEmpty()) {
            subGroups.add(new SubGroup(
                    category.getId(),
                    null,
                    category.getName(),
                    directProducts.size(),
                    directProducts.stream().mapToLong(ProductInventory::stockQuantity).sum(),
                    directProducts
            ));
        }
        return new CategoryInventory(category.getId(), category.getName(), totalStock, subGroups);
    }

    private Branch findBranch(UUID branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi nhánh"));
    }

    private Branch findBranchForAccount(UUID branchId, UUID accountId) {
        Branch branch = branchRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Tài khoản không được gắn với chi nhánh"
                ));
        if (!branch.getId().equals(branchId)) {
            throw new AccessDeniedException("Chỉ được truy cập tồn kho chi nhánh của tài khoản");
        }
        return branch;
    }
}
