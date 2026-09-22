package com.haui.UrbanMart.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.haui.UrbanMart.entity.User;

public interface UserRepository extends JpaRepository <User, UUID> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone (String phone);
    boolean existsByEmail (String email);
}
