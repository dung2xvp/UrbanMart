package com.haui.UrbanMart.repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.haui.UrbanMart.entity.Address;

public interface AddressRepository extends JpaRepository <Address, UUID> {

    List<Address> findAllByUserId(UUID userId);

    Optional<Address> findByIdAndUserId(UUID addressId, UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Address a
            SET a.isDefault = false
            WHERE a.user.id = :userId
            """)
    void clearDefaultAddress(@Param("userId") UUID userId);
}
