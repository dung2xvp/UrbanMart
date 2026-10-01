package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.ProductResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.repository.ProductListSort;
import com.haui.UrbanMart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductResponse>> getProducts(
            @RequestParam UUID branchId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction
    ) {
        validateListParameters(page, size, minPrice, maxPrice);
        ProductListSort productSort = resolveSort(sort, sortBy, direction);
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.success("Lấy danh sách sản phẩm thành công",
                productService.getProducts(
                        branchId,
                        keyword,
                        categoryId,
                        includeDescendants,
                        brandIds,
                        minPrice,
                        maxPrice,
                        inStock,
                        productSort,
                        pageable
                ));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<Page<ProductResponse>> getProductsByCategory(
            @RequestParam UUID branchId,
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort
    ) {
        validateListParameters(page, size, minPrice, maxPrice);
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.success("Lấy sản phẩm theo danh mục thành công",
                productService.getProductsByCategory(
                        branchId,
                        categoryId,
                        includeDescendants,
                        keyword,
                        brandIds,
                        minPrice,
                        maxPrice,
                        inStock,
                        parseSort(sort),
                        pageable
                ));
    }

    @GetMapping("/promotions")
    public ApiResponse<Page<ProductResponse>> getPromotions(
            @RequestParam UUID branchId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "discount_desc") String sort
    ) {
        validateListParameters(page, size, minPrice, maxPrice);
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.success("Lấy danh sách sản phẩm khuyến mãi thành công",
                productService.getPromotions(
                        branchId,
                        keyword,
                        categoryId,
                        includeDescendants,
                        brandIds,
                        minPrice,
                        maxPrice,
                        inStock,
                        parseSort(sort),
                        pageable
                ));
    }

    @GetMapping("/daily-fresh")
    public ApiResponse<Page<ProductResponse>> getDailyFresh(
            @RequestParam UUID branchId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            @RequestParam(required = false) List<UUID> brandIds,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name_asc") String sort
    ) {
        validateListParameters(page, size, minPrice, maxPrice);
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.success("Lấy sản phẩm hàng ngày thành công",
                productService.getDailyFresh(
                        branchId,
                        keyword,
                        categoryId,
                        includeDescendants,
                        brandIds,
                        minPrice,
                        maxPrice,
                        inStock,
                        parseSort(sort),
                        pageable
                ));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable UUID productId,
            @RequestParam UUID branchId
    ) {
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công",
                productService.getProduct(branchId, productId));
    }

    private void validateListParameters(
            int page,
            int size,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoac size khong hop le");
        }
        if (minPrice != null && minPrice.signum() < 0
                || maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("Giá lọc không được âm");
        }
        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice không được lớn hơn maxPrice");
        }
    }

    private ProductListSort resolveSort(
            String sort,
            String sortBy,
            String direction
    ) {
        if (sort != null) {
            return parseSort(sort);
        }
        if (sortBy == null && direction == null) {
            return ProductListSort.NAME_ASC;
        }

        String property = sortBy == null ? "name" : sortBy;
        String order = direction == null ? "asc" : direction;
        return switch (property + "_" + order.toLowerCase()) {
            case "name_asc" -> ProductListSort.NAME_ASC;
            case "name_desc" -> ProductListSort.NAME_DESC;
            case "price_asc", "basePrice_asc" -> ProductListSort.PRICE_ASC;
            case "price_desc", "basePrice_desc" -> ProductListSort.PRICE_DESC;
            case "createdAt_desc" -> ProductListSort.NEWEST;
            default -> throw new BadRequestException("Kiểu sắp xếp không hợp lệ");
        };
    }

    private ProductListSort parseSort(String sort) {
        return switch (sort.toLowerCase()) {
            case "name_asc" -> ProductListSort.NAME_ASC;
            case "name_desc" -> ProductListSort.NAME_DESC;
            case "price_asc" -> ProductListSort.PRICE_ASC;
            case "price_desc" -> ProductListSort.PRICE_DESC;
            case "discount_desc" -> ProductListSort.DISCOUNT_DESC;
            case "newest" -> ProductListSort.NEWEST;
            default -> throw new BadRequestException("Kiểu sắp xếp không hợp lệ");
        };
    }
}
