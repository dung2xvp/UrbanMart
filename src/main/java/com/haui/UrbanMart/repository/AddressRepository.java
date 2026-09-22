package com.haui.UrbanMart.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haui.UrbanMart.entity.Address;

public interface AddressRepository extends JpaRepository <Address, UUID> {
    
}
