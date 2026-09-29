package com.haui.UrbanMart.service;

import com.haui.UrbanMart.repository.ProductDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductDiscountService {

    private final ProductDiscountRepository productDiscountRepository;
}
