package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndNameContainingIgnoreCase(
            ProductStatus status, String name, Pageable pageable
    );

    boolean existsByCategory_Id(UUID categoryId);

    @Query(value = "SELECT nextval('product_sku_seq')", nativeQuery = true)
    Long nextSkuValue();
}