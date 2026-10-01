package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.WishlistProductResponse;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.ProductStatus;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.entity.Wishlist;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.ProductRepository;
import com.haui.UrbanMart.repository.UserRepository;
import com.haui.UrbanMart.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductPricingService productPricingService;

    @Transactional(readOnly = true)
    public List<WishlistProductResponse> getMyWishlist(UUID userId) {
        List<Wishlist> wishlists = wishlistRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, ProductPrice> prices = productPricingService.getCurrentPrices(
                wishlists.stream().map(Wishlist::getProduct).toList()
        );

        return wishlists.stream()
                .map(wishlist -> WishlistProductResponse.from(
                        wishlist,
                        prices.get(wishlist.getProduct().getId())
                ))
                .toList();
    }

    @Transactional
    public void add(UUID userId, UUID productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy user"
                ));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm"
                ));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException(
                    "Sản phẩm đã ngừng kinh doanh"
            );
        }
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ConflictException("Sản phẩm đã có trong danh sách yêu thích");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);

        wishlistRepository.save(wishlist);
    }
    @Transactional
    public void remove(UUID userId, UUID productId) {
        Wishlist wishlist = wishlistRepository
                .findByUserIdAndProductId(userId, productId)
                .orElseThrow(()->new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm trong danh sách yêu thích"
                ));
        wishlistRepository.delete(wishlist);
    }
}
