package com.haui.UrbanMart.service;

import com.haui.UrbanMart.entity.Branch;
import com.haui.UrbanMart.entity.BranchStatus;
import com.haui.UrbanMart.entity.OrderStatus;
import com.haui.UrbanMart.exception.ConflictException;
import com.haui.UrbanMart.repository.BranchInventoryRepository;
import com.haui.UrbanMart.repository.BranchRepository;
import com.haui.UrbanMart.repository.OrderRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BranchAdminServiceTest {

    @Test
    void cannotCloseBranchWithOrdersRequiringAction() {
        UUID branchId = UUID.randomUUID();
        Branch branch = new Branch();
        branch.setStatus(BranchStatus.ACTIVE);

        BranchRepository branchRepository = mock(BranchRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(orderRepository.existsByBranch_IdAndStatusIn(
                eq(branchId),
                eq(ordersRequiringAction())
        )).thenReturn(true);

        BranchAdminService service = new BranchAdminService(
                branchRepository,
                mock(BranchInventoryRepository.class),
                orderRepository
        );

        assertThrows(ConflictException.class, () -> service.closeBranch(branchId));
        assertEquals(BranchStatus.ACTIVE, branch.getStatus());
    }

    @Test
    void closesBranchWhenNoOrdersRequireAction() {
        UUID branchId = UUID.randomUUID();
        Branch branch = new Branch();
        branch.setStatus(BranchStatus.ACTIVE);

        BranchRepository branchRepository = mock(BranchRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(orderRepository.existsByBranch_IdAndStatusIn(
                eq(branchId),
                eq(ordersRequiringAction())
        )).thenReturn(false);

        BranchAdminService service = new BranchAdminService(
                branchRepository,
                mock(BranchInventoryRepository.class),
                orderRepository
        );

        service.closeBranch(branchId);

        assertEquals(BranchStatus.CLOSED_TEMP, branch.getStatus());
        verify(orderRepository).existsByBranch_IdAndStatusIn(
                eq(branchId),
                eq(ordersRequiringAction())
        );
    }

    private Set<OrderStatus> ordersRequiringAction() {
        return Set.of(
                OrderStatus.PENDING,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPING,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.DELIVERY_FAILED,
                OrderStatus.DELIVERED_PENDING_CONFIRMATION
        );
    }
}
