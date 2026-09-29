package com.haui.UrbanMart.controller;

import com.haui.UrbanMart.dto.response.ApiResponse;
import com.haui.UrbanMart.dto.response.ProductResponse;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page hoac size khong hop le");
        }

        Sort.Direction sortDirection = switch (direction.toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw new BadRequestException(
                    "Direction chỉ nhận DESC hoặc ASC"
            );
        };
        String sortProperty = switch (sortBy) {
            case "name" -> "product.name";
            case "price" -> "product.basePrice";
            case "createdAt" -> "product.createdAt";
            default -> throw new BadRequestException("Truong sort khong hop le");
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortProperty)
        );
        return ApiResponse.success("Lay danh sach san pham thanh cong",
                productService.getProducts(branchId, keyword, pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable UUID productId,
            @RequestParam UUID branchId
    ) {
        return ApiResponse.success("Lay chi tiet san pham thanh cong",
                productService.getProduct(branchId, productId));
    }
}
