package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.BranchInventoryLotResponse;
import com.haui.UrbanMart.entity.BranchInventory;
import com.haui.UrbanMart.entity.BranchInventoryLot;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.BranchInventoryLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryLotService {

    private final BranchInventoryLotRepository lotRepository;

    @Transactional
    public BranchInventoryLot receive(
            BranchInventory inventory,
            int quantity,
            LocalDate expiryDate
    ) {
        BranchInventoryLot lot = new BranchInventoryLot();
        lot.setInventory(inventory);
        lot.setQuantity(quantity);
        lot.setExpiryDate(expiryDate);
        return lotRepository.save(lot);
    }

    @Transactional
    public void consume(BranchInventory inventory, int quantity) {
        int remaining = quantity;
        List<BranchInventoryLot> lots =
                lotRepository.findAvailableLotsForUpdate(inventory.getId());
        for (BranchInventoryLot lot : lots) {
            int removed = Math.min(lot.getQuantity(), remaining);
            lot.setQuantity(lot.getQuantity() - removed);
            remaining -= removed;
            if (remaining == 0) {
                break;
            }
        }
        if (remaining > 0) {
            throw new ConflictException(
                    "Số lượng tồn theo lô không khớp với tồn kho tổng hợp"
            );
        }
        lotRepository.saveAll(lots);
    }

    @Transactional(readOnly = true)
    public List<BranchInventoryLotResponse> getLots(UUID branchId, UUID productId) {
        return lotRepository.findLots(branchId, productId).stream()
                .map(lot -> new BranchInventoryLotResponse(
                        lot.getId(),
                        lot.getInventory().getProduct().getId(),
                        lot.getInventory().getProduct().getName(),
                        lot.getQuantity(),
                        lot.getExpiryDate(),
                        lot.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public BranchInventoryLotResponse updateExpiry(
            UUID branchId,
            UUID lotId,
            LocalDate expiryDate
    ) {
        BranchInventoryLot lot = lotRepository.findForUpdate(branchId, lotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lô tồn kho tại chi nhánh"
                ));
        lot.setExpiryDate(expiryDate);
        lot = lotRepository.save(lot);
        return new BranchInventoryLotResponse(
                lot.getId(),
                lot.getInventory().getProduct().getId(),
                lot.getInventory().getProduct().getName(),
                lot.getQuantity(),
                lot.getExpiryDate(),
                lot.getCreatedAt()
        );
    }
}
