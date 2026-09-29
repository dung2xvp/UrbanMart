package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.AddCartItemRequest;
import com.haui.UrbanMart.dto.request.UpdateCartItemRequest;
import com.haui.UrbanMart.dto.response.CartItemResponse;
import com.haui.UrbanMart.dto.response.CartResponse;
import com.haui.UrbanMart.entity.*;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId, UUID branchId) {
        validateSellingBranch(branchId);

        return cartRepository.findByUserIdAndBranchId(userId, branchId)
                .map(this::buildCartResponse)
                .orElseGet(() -> new CartResponse(
                        null,
                        branchId,
                        List.of(),
                        0,
                        BigDecimal.ZERO
                ));
    }
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        Branch branch = validateSellingBranch(request.getBranchId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy saản phẩm"
                ));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException(
                    "Sản phẩm đã ngừng kinh doanh"
            );
        }

        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branch.getId(), product.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sản phẩm không được bán tại chi nhánh này"
                ));

        Cart cart = cartRepository.findByUserIdAndBranchId(userId, branch.getId())
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Không tìm thấy người dùng"
                            ));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setBranch(branch);
                    return cartRepository.save(newCart);
                });
        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });
        int updatedQuantity = item.getQuantity() + request.getQuantity();

        if (updatedQuantity > inventory.getStockQuantity()) {
            throw new BadRequestException("Số lượng vượt quá tồn kho hiện tại");
        }

        item.setQuantity(updatedQuantity);
        item.setUnitPrice(product.getBasePrice());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    public CartResponse updateItem(UUID userId, UUID itemId,
                                   UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay san pham trong gio hang"
                ));

        UUID branchId = item.getCart().getBranch().getId();
        UUID productId = item.getProduct().getId();

        BranchInventory inventory = branchInventoryRepository
                .findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "San pham khong con duoc ban tai chi nhanh nay"
                ));

        if (request.getQuantity() > inventory.getStockQuantity()) {
            throw new BadRequestException(
                    "So luong vuot qua ton kho hien tai"
            );
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(item.getCart());
    }

    public void removeItem(UUID userId, UUID itemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay san pham trong gio hang"
                ));

        cartItemRepository.delete(item);
    }

    public void clearCart(UUID userId, UUID branchId) {
        validateSellingBranch(branchId);

        cartRepository.findByUserIdAndBranchId(userId, branchId)
                .ifPresent(cart ->
                        cartItemRepository.deleteAllByCartId(cart.getId())
                );
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> responseItems = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItemRepository.findAllByCartId(cart.getId())) {
            UUID productId = item.getProduct().getId();
            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            responseItems.add(new CartItemResponse(
                    item.getId(),
                    productId,
                    item.getProduct().getName(),
                    item.getProduct().getImageUrl(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    subtotal
            ));

            totalQuantity += item.getQuantity();
            totalAmount = totalAmount.add(subtotal);
        }

        return new CartResponse(
                cart.getId(),
                cart.getBranch().getId(),
                responseItems,
                totalQuantity,
                totalAmount
        );
    }

    private Branch validateSellingBranch(UUID branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay chi nhanh"
                ));

        if (branch.getStatus() != BranchStatus.ACTIVE || branch.isWarehouse()) {
            throw new BadRequestException(
                    "Chi nhanh khong hoat dong ban hang"
            );
        }

        return branch;
    }
}
