package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductDiscount;
import com.haui.UrbanMart.repository.ProductDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductPricingService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ProductDiscountRepository productDiscountRepository;

    @Transactional(readOnly = true)
    public Map<UUID, ProductPrice> getCurrentPrices(Collection<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }

        OffsetDateTime now = OffsetDateTime.now();
        Map<UUID, ProductDiscount> activeDiscounts =
                productDiscountRepository.findActiveByProductIds(
                                products.stream().map(Product::getId).toList(),
                                now
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                discount -> discount.getProduct().getId(),
                                Function.identity()
                        ));

        return products.stream().collect(Collectors.toMap(
                Product::getId,
                product -> toPrice(product, activeDiscounts.get(product.getId()))
        ));
    }

    @Transactional(readOnly = true)
    public ProductPrice getCurrentPrice(Product product) {
        OffsetDateTime now = OffsetDateTime.now();
        ProductDiscount discount = productDiscountRepository
                .findActiveByProductId(product.getId(), now)
                .orElse(null);
        return toPrice(product, discount);
    }

    private ProductPrice toPrice(Product product, ProductDiscount discount) {
        BigDecimal basePrice = product.getBasePrice();
        if (discount == null) {
            return new ProductPrice(basePrice, basePrice, null, null, null);
        }

        BigDecimal price = basePrice
                .multiply(ONE_HUNDRED.subtract(discount.getDiscountPercent()))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        return new ProductPrice(
                basePrice,
                price,
                discount.getDiscountPercent(),
                discount.getStartsAt(),
                discount.getEndsAt()
        );
    }
}
