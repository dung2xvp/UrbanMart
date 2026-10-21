package com.haui.UrbanMart.repository;

import java.util.Optional;
import java.util.UUID;

import com.haui.UrbanMart.entity.UserRole;
import com.haui.UrbanMart.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.haui.UrbanMart.entity.User;

public interface UserRepository extends JpaRepository <User, UUID> {
    @Query("""
            SELECT user
            FROM User user
            WHERE (:keyword = ''
                   OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR user.role = :role)
              AND (:status IS NULL OR user.status = :status)
            """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM User user
            WHERE user.id = :userId
            """)
    Optional<User> findForUpdateById(@Param("userId") UUID userId);

    Optional<User> findByPhone(String phone);
    boolean existsByPhone (String phone);
    boolean existsByEmail (String email);
    boolean existsByPhoneAndIdNot(String phone, UUID userId);
    boolean existsByEmailAndIdNot(String email, UUID userId);
    long countByRoleAndStatus(UserRole role, UserStatus status);
}
