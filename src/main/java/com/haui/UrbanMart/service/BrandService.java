package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.BrandResponse;
import com.haui.UrbanMart.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        brand -> brand.getName().toLowerCase()
                ))
                .map(BrandResponse::from)
                .toList();
    }
}
