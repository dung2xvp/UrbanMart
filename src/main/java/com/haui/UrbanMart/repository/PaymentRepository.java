package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findAllByProviderAndProviderReference(
            String provider,
            String providerReference
    );

    Optional<Payment> findByTransactionId(String transactionId);
}
