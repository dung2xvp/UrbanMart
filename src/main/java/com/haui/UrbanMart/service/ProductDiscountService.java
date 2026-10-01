package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.ProductDiscountRequest;
import com.haui.UrbanMart.dto.response.ProductDiscountResponse;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductDiscount;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.ProductDiscountRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductDiscountService {

    private final ProductDiscountRepository productDiscountRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDiscountResponse> getAll() {
        return productDiscountRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ProductDiscountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDiscountResponse getById(UUID id) {
        return ProductDiscountResponse.from(findDiscount(id));
    }

    @Transactional
    public ProductDiscountResponse create(ProductDiscountRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm"
                ));

        validatePeriod(request);
        ensureNoOverlap(
                product.getId(),
                request,
                null
        );

        ProductDiscount discount = new ProductDiscount();
        discount.setProduct(product);
        applyRequest(discount, request);

        return ProductDiscountResponse.from(
                productDiscountRepository.saveAndFlush(discount)
        );
    }

    @Transactional
    public ProductDiscountResponse update(UUID id, ProductDiscountRequest request) {
        ProductDiscount discount = findDiscount(id);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm"
                ));

        validatePeriod(request);
        ensureNoOverlap(
                product.getId(),
                request,
                id
        );

        discount.setProduct(product);
        applyRequest(discount, request);

        return ProductDiscountResponse.from(
                productDiscountRepository.saveAndFlush(discount)
        );
    }

    @Transactional
    public void delete(UUID id) {
        ProductDiscount discount = findDiscount(id);
        productDiscountRepository.delete(discount);
    }

    private ProductDiscount findDiscount(UUID id) {
        return productDiscountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đợt giảm giá"
                ));
    }

    private void validatePeriod(ProductDiscountRequest request) {
        if (!request.getStartsAt().isBefore(request.getEndsAt())) {
            throw new BadRequestException(
                    "Thời gian bắt đầu phải trước thời gian kết thúc"
            );
        }
    }

    private void ensureNoOverlap(
            UUID productId,
            ProductDiscountRequest request,
            UUID excludedId
    ) {
        if (productDiscountRepository.existsOverlappingDiscount(
                productId,
                request.getStartsAt(),
                request.getEndsAt(),
                excludedId
        )) {
            throw new ConflictException(
                    "Sản phẩm đã có đợt giảm giá trong khoảng thời gian này"
            );
        }
    }

    private void applyRequest(
            ProductDiscount discount,
            ProductDiscountRequest request
    ) {
        discount.setDiscountPercent(request.getDiscountPercent());
        discount.setStartsAt(request.getStartsAt());
        discount.setEndsAt(request.getEndsAt());
    }
}
