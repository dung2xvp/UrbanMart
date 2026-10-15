package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.BranchInventoryLotRequest;
import com.haui.UrbanMart.dto.request.BranchInventoryWriteRequest;
import com.haui.UrbanMart.dto.request.UpdateBranchInventoryLotExpiryRequest;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.CategoryInventory;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.ProductInventory;
import com.haui.UrbanMart.dto.response.BranchInventoryByCategoryResponse.SubGroup;
import com.haui.UrbanMart.dto.response.BranchInventoryCategoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryLotResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryProductResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryReportResponse;
import com.haui.UrbanMart.dto.response.BranchInventoryResponse;
import com.haui.UrbanMart.dto.response.BranchInventorySummaryResponse;
import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.BranchInventoryLot;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.entity.InventoryMovement;
import com.haui.UrbanMart.entity.InventoryMovementType;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryLotRepository;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.InventoryMovementRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BranchInventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final BranchInventoryLotRepository branchInventoryLotRepository;
    private final InventoryLotService inventoryLotService;

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

        BranchInventory inventory = branchInventoryRepository
                .findForUpdate(branch.getId(), product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm trong tồn kho chi nhánh"
                ));

        int previousStock = inventory.getStockQuantity();
        if (request.stockQuantity() >= previousStock) {
            throw new BadRequestException(
                    "Điều chỉnh tồn kho chỉ được giảm số lượng"
            );
        }
        if (request.stockQuantity() < inventory.getReservedQuantity()) {
            throw new BadRequestException(
                    "Số lượng tồn kho không thể thấp hơn số lượng đang được giữ"
            );
        }

        int quantityRemoved = previousStock - request.stockQuantity();
        inventoryLotService.consume(inventory, quantityRemoved);
        inventory.setStockQuantity(request.stockQuantity());
        inventory = branchInventoryRepository.save(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setBranch(branch);
        movement.setProduct(product);
        movement.setPerformedBy(branch.getAccount());
        movement.setMovementType(InventoryMovementType.ADJUSTMENT);
        movement.setOnHandDelta(request.stockQuantity() - previousStock);
        movement.setReservedDelta(0);
        movement.setReason(request.reason());
        inventoryMovementRepository.save(movement);

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
    public BranchInventoryReportResponse getOwnInventoryReport(
            UUID branchId,
            UUID accountId
    ) {
        return buildInventoryReport(findBranchForAccount(branchId, accountId));
    }

    @Transactional(readOnly = true)
    public BranchInventoryReportResponse getInventoryReport(UUID branchId) {
        return buildInventoryReport(findBranch(branchId));
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryCategoryReportResponse> getOwnInventoryCategoryReport(
            UUID branchId,
            UUID accountId
    ) {
        return buildInventoryCategoryReport(findBranchForAccount(branchId, accountId));
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryCategoryReportResponse> getInventoryCategoryReport(UUID branchId) {
        return buildInventoryCategoryReport(findBranch(branchId));
    }

    @Transactional(readOnly = true)
    public Page<BranchInventoryProductResponse> searchAdminInventory(
            UUID branchId,
            UUID categoryId,
            String keyword,
            Pageable pageable
    ) {
        findBranch(branchId);
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Không tìm thấy danh mục");
        }
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
        return branchInventoryRepository.searchAdminInventory(
                        branchId,
                        categoryId,
                        normalizedKeyword,
                        pageable
                )
                .map(inventory -> new BranchInventoryProductResponse(
                        inventory.getProduct().getId(),
                        inventory.getProduct().getSku(),
                        inventory.getProduct().getName(),
                        inventory.getProduct().getImageUrl(),
                        inventory.getProduct().getUnit(),
                        inventory.getProduct().getCategory() == null
                                ? null
                                : inventory.getProduct().getCategory().getId(),
                        inventory.getProduct().getCategory() == null
                                ? null
                                : inventory.getProduct().getCategory().getName(),
                        inventory.getStockQuantity(),
                        inventory.getReservedQuantity(),
                        inventory.getStockQuantity() - inventory.getReservedQuantity(),
                        inventory.getUpdatedAt()
                ));
    }

    @Transactional
    public BranchInventoryLotResponse receiveOwnLot(
            UUID branchId,
            UUID accountId,
            BranchInventoryLotRequest request
    ) {
        Branch branch = findBranchForAccount(branchId, accountId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        if (branch.getStatus() != BranchStatus.ACTIVE
                || product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(
                    "Chỉ được nhập lô cho chi nhánh và sản phẩm đang hoạt động"
            );
        }

        branchInventoryRepository.insertStockIfMissing(
                branch.getId(), product.getId(), 0
        );
        BranchInventory inventory = branchInventoryRepository
                .findForUpdate(branch.getId(), product.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy tồn kho sau khi tạo liên kết"
                ));
        int newStock;
        try {
            newStock = Math.addExact(inventory.getStockQuantity(), request.quantity());
        } catch (ArithmeticException exception) {
            throw new ConflictException("Số lượng tồn kho vượt giới hạn cho phép");
        }

        inventory.setStockQuantity(newStock);
        inventory = branchInventoryRepository.save(inventory);
        BranchInventoryLot lot =
                inventoryLotService.receive(inventory, request.quantity(), request.expiryDate());

        InventoryMovement movement = new InventoryMovement();
        movement.setBranch(branch);
        movement.setProduct(product);
        movement.setPerformedBy(branch.getAccount());
        movement.setMovementType(InventoryMovementType.RECEIVED);
        movement.setOnHandDelta(request.quantity());
        movement.setReservedDelta(0);
        movement.setReason(request.expiryDate() == null
                ? "Nhập lô hàng không có ngày hết hạn"
                : "Nhập lô hàng, hết hạn " + request.expiryDate());
        inventoryMovementRepository.save(movement);

        return new BranchInventoryLotResponse(
                lot.getId(),
                product.getId(),
                product.getName(),
                request.quantity(),
                request.expiryDate(),
                lot.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryLotResponse> getOwnLots(
            UUID branchId,
            UUID accountId,
            UUID productId
    ) {
        findBranchForAccount(branchId, accountId);
        return inventoryLotService.getLots(branchId, productId);
    }

    @Transactional
    public BranchInventoryLotResponse updateOwnLotExpiry(
            UUID branchId,
            UUID accountId,
            UUID lotId,
            UpdateBranchInventoryLotExpiryRequest request
    ) {
        findBranchForAccount(branchId, accountId);
        return inventoryLotService.updateExpiry(branchId, lotId, request.expiryDate());
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

    private BranchInventoryReportResponse buildInventoryReport(Branch branch) {
        List<BranchInventory> inventories =
                branchInventoryRepository.findAllByBranchId(branch.getId());
        List<BranchInventoryLot> lots =
                branchInventoryLotRepository.findAllAvailableByBranchId(branch.getId());
        LocalDate today = LocalDate.now();
        LocalDate expiryCutoff = today.plusDays(7);
        long reserved = 0;
        long available = 0;
        long lowStock = 0;
        long outOfStock = 0;
        for (BranchInventory inventory : inventories) {
            int availableQuantity =
                    inventory.getStockQuantity() - inventory.getReservedQuantity();
            reserved += inventory.getReservedQuantity();
            available += availableQuantity;
            if (availableQuantity == 0) {
                outOfStock++;
            } else if (availableQuantity <= inventory.getMinStockLevel()) {
                lowStock++;
            }
        }
        long expiringSoon = lots.stream()
                .filter(lot -> lot.getExpiryDate() != null
                        && !lot.getExpiryDate().isBefore(today)
                        && !lot.getExpiryDate().isAfter(expiryCutoff))
                .map(lot -> lot.getInventory().getId())
                .distinct()
                .count();
        return new BranchInventoryReportResponse(
                branch.getId(),
                branch.getName(),
                inventories.size(),
                inventories.stream().mapToLong(BranchInventory::getStockQuantity).sum(),
                available,
                reserved,
                lowStock,
                outOfStock,
                expiringSoon
        );
    }

    private List<BranchInventoryCategoryReportResponse> buildInventoryCategoryReport(
            Branch branch
    ) {
        List<Category> categories = categoryRepository.findAll();
        Map<UUID, InventoryCategoryTotals> totalsByCategory = new HashMap<>();
        categories.forEach(category ->
                totalsByCategory.put(category.getId(), new InventoryCategoryTotals())
        );

        for (BranchInventory inventory : branchInventoryRepository.findAllByBranchId(branch.getId())) {
            Category category = inventory.getProduct().getCategory();
            while (category != null) {
                InventoryCategoryTotals totals = totalsByCategory.get(category.getId());
                if (totals != null) {
                    totals.add(inventory);
                }
                category = category.getParent();
            }
        }

        Map<UUID, List<Category>> childrenByParent = new HashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParent() == null) {
                roots.add(category);
            } else {
                childrenByParent.computeIfAbsent(category.getParent().getId(), ignored ->
                        new ArrayList<>()).add(category);
            }
        }
        Comparator<Category> byName = Comparator.comparing(
                Category::getName, String.CASE_INSENSITIVE_ORDER
        );
        roots.sort(byName);
        childrenByParent.values().forEach(children -> children.sort(byName));

        return roots.stream()
                .map(root -> {
                    InventoryCategoryTotals rootTotals = totalsByCategory.get(root.getId());
                    List<BranchInventoryCategoryReportResponse.SubCategory> subCategories =
                            childrenByParent.getOrDefault(root.getId(), List.of()).stream()
                                    .map(child -> {
                                        InventoryCategoryTotals childTotals =
                                                totalsByCategory.get(child.getId());
                                        return new BranchInventoryCategoryReportResponse.SubCategory(
                                                child.getId(),
                                                child.getName(),
                                                childTotals.totalSkus,
                                                childTotals.totalOnHand,
                                                childTotals.totalAvailable
                                        );
                                    })
                                    .toList();
                    return new BranchInventoryCategoryReportResponse(
                            root.getId(),
                            root.getName(),
                            rootTotals.totalSkus,
                            rootTotals.totalOnHand,
                            rootTotals.totalAvailable,
                            rootTotals.lowStockSkus,
                            subCategories
                    );
                })
                .toList();
    }

    private static class InventoryCategoryTotals {
        private long totalSkus;
        private long totalOnHand;
        private long totalAvailable;
        private long lowStockSkus;

        private void add(BranchInventory inventory) {
            int available = inventory.getStockQuantity() - inventory.getReservedQuantity();
            totalSkus++;
            totalOnHand += inventory.getStockQuantity();
            totalAvailable += available;
            if (available > 0 && available <= inventory.getMinStockLevel()) {
                lowStockSkus++;
            }
        }
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
