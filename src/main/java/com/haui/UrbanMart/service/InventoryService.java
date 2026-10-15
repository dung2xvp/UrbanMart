package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.InventoryMovement;
import com.haui.UrbanMart.entity.InventoryMovementType;
import com.haui.UrbanMart.entity.Order;
import com.haui.UrbanMart.entity.Product;
import com.haui.UrbanMart.entity.User;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.InventoryMovementRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final BranchInventoryRepository branchInventoryRepository;

    private final InventoryMovementRepository inventoryMovementRepository;

    private final InventoryLotService inventoryLotService;

    private final EntityManager entityManager;

    @Transactional
    public void reserve(UUID branchId, UUID productId, int quantity, UUID orderId, UUID actorId) {
        requirePositive(quantity);
        if (orderId == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }

        int updated = branchInventoryRepository.reserveStock(branchId, productId, quantity);
        if (updated != 1) {
            throw new ConflictException("Không đủ tồn kho để giữ sản phẩm");
        }

        saveMovement(
                branchId, productId, orderId, actorId,
                InventoryMovementType.RESERVED, 0, quantity, "Giữ hàng cho đơn"
        );
    }

    @Transactional
    public void release(UUID branchId, UUID productId, int quantity, UUID orderId, UUID actorId) {
        requirePositive(quantity);
        requireOrderId(orderId);

        int updated = branchInventoryRepository.releaseReservedStock(
                branchId, productId, quantity
        );
        if (updated != 1) {
            throw new ConflictException("Không đủ số lượng đang giữ để giải phóng");
        }

        saveMovement(
                branchId, productId, orderId, actorId,
                InventoryMovementType.RELEASED, 0, -quantity, "Giải phóng hàng đã giữ"
        );
    }

    @Transactional
    public void dispatchDelivery(UUID branchId, UUID productId, int quantity, Order order) {
        dispatchDelivery(branchId, productId, quantity, order, null);
    }

    @Transactional
    public void dispatchDelivery(
            UUID branchId, UUID productId, int quantity, Order order, UUID actorId
    ) {
        requirePositive(quantity);
        requireOrderBelongsToBranch(order, branchId);

        dispatchReservedStock(
                branchId, productId, quantity, order.getId(), actorId,
                "Xuất hàng để giao cho đơn"
        );
    }

    @Transactional
    public void completePickup(UUID branchId, UUID productId, int quantity, Order order) {
        completePickup(branchId, productId, quantity, order, null);
    }

    @Transactional
    public void completePickup(
            UUID branchId, UUID productId, int quantity, Order order, UUID actorId
    ) {
        requirePositive(quantity);
        requireOrderBelongsToBranch(order, branchId);

        dispatchReservedStock(
                branchId, productId, quantity, order.getId(), actorId,
                "Hoàn tất nhận hàng tại chi nhánh"
        );
    }

    @Transactional
    public void receiveReturn(
            UUID branchId, UUID productId, int sellableQuantity, UUID orderId, UUID actorId, String reason
    ) {
        if (sellableQuantity < 0) {
            throw new BadRequestException("Số lượng hoàn không được âm");
        }

        if (sellableQuantity == 0) {
            return;
        }

        int updated = branchInventoryRepository.receiveReturnedStock(
                branchId, productId, sellableQuantity
        );

        if (updated != 1) {
            throw new ConflictException("Không tìm thấy dòng tồn kho để nhập lại hàng hoàn");
        }
        BranchInventory inventory = branchInventoryRepository
                .findForUpdate(branchId, productId)
                .orElseThrow(() -> new ConflictException(
                        "Không tìm thấy dòng tồn kho để tạo lô hàng hoàn"
                ));
        inventoryLotService.receive(inventory, sellableQuantity, null);

        saveMovement(
                branchId, productId, orderId, actorId,
                InventoryMovementType.RETURNED, sellableQuantity, 0, reason
        );
    }

    @Transactional
    public void adjustOnHand(
            UUID branchId, UUID productId, int newQuantity, UUID actorId, String reason
    ) {
        if (newQuantity < 0) {
            throw new BadRequestException("Tồn kho không được âm");
        }

        BranchInventory inventory = branchInventoryRepository
                .findForUpdate(branchId, productId)
                .orElseThrow(()-> new ResourceNotFoundException("Không tìm thấy dòng tồn kho"));

        if (newQuantity < inventory.getReservedQuantity()) {
            throw new ConflictException(
                    "Tồn kho mới không thể thấp hơn số lượng đang giữ"
            );
        }

        int delta = newQuantity - inventory.getStockQuantity();
        if (delta == 0) {
            return;
        }

        if (delta < 0) {
            inventoryLotService.consume(inventory, -delta);
        } else {
            inventoryLotService.receive(inventory, delta, null);
        }
        inventory.setStockQuantity(newQuantity);
        branchInventoryRepository.save(inventory);

        saveMovement(
                branchId, productId, null, actorId,
                InventoryMovementType.ADJUSTMENT, delta, 0, reason
        );
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng phải lớn hơn 0");
        }
    }

    private void requireOrderId(UUID orderId) {
        if (orderId == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
    }

    private void requireOrderBelongsToBranch(Order order, UUID branchId) {
        if (order == null || order.getId() == null) {
            throw new BadRequestException("Đơn hàng không được để trống");
        }
        if (branchId == null
                || order.getBranch() == null
                || order.getBranch().getId() == null
                || !branchId.equals(order.getBranch().getId())) {
            throw new BadRequestException("Đơn hàng không thuộc chi nhánh này");
        }
    }

    private void dispatchReservedStock(
            UUID branchId,
            UUID productId,
            int quantity,
            UUID orderId,
            UUID actorId,
            String reason
    ) {
        int updated = branchInventoryRepository.dispatchReservedStock(
                branchId, productId, quantity
        );
        if (updated != 1) {
            throw new ConflictException("Không đủ số lượng đang giữ để xuất kho");
        }
        BranchInventory inventory = branchInventoryRepository
                .findForUpdate(branchId, productId)
                .orElseThrow(() -> new ConflictException(
                        "Không tìm thấy tồn kho để cập nhật lô xuất hàng"
                ));
        inventoryLotService.consume(inventory, quantity);

        saveMovement(
                branchId, productId, orderId, actorId,
                InventoryMovementType.DISPATCHED, -quantity, -quantity, reason
        );
    }

    private void saveMovement(
            UUID branchId,
            UUID productId,
            UUID orderId,
            UUID actorId,
            InventoryMovementType type,
            int onHandDelta,
            int reservedDelta,
            String reason
    ) {
        InventoryMovement movement = new InventoryMovement();
        movement.setBranch(entityManager.getReference(
                Branch.class,
                branchId
        ));
        movement.setProduct(entityManager.getReference(
                Product.class,
                productId
        ));
        movement.setOrder(orderId == null
                ? null
                : entityManager.getReference(Order.class, orderId));
        movement.setPerformedBy(actorId == null
                ? null
                : entityManager.getReference(User.class, actorId));
        movement.setMovementType(type);
        movement.setOnHandDelta(onHandDelta);
        movement.setReservedDelta(reservedDelta);
        movement.setReason(reason);
        inventoryMovementRepository.save(movement);
    }
}
