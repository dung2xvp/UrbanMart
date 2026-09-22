package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haui.UrbanMart.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
}