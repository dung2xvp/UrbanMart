package com.haui.UrbanMart.repository;

import com.haui.UrbanMart.entity.FoodIncompatibility;
import com.haui.UrbanMart.entity.IncompatibilitySeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodIncompatibilityRepository extends JpaRepository<FoodIncompatibility, UUID> {

    Optional<FoodIncompatibility> findByTagAAndTagB(String tagA, String tagB);

    @Query("""
            SELECT rule
            FROM FoodIncompatibility rule
            WHERE (:severity IS NULL OR rule.severity = :severity)
              AND (:active IS NULL OR rule.active = :active)
              AND (
                    :search IS NULL
                    OR LOWER(rule.tagA) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(rule.tagB) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(rule.title) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            ORDER BY rule.createdAt DESC, rule.id ASC
            """)
    Page<FoodIncompatibility> search(
            @Param("search") String search,
            @Param("severity") IncompatibilitySeverity severity,
            @Param("active") Boolean active,
            Pageable pageable
    );

    /** Quy tac dang bat ma ca hai tag deu nam trong tap tag cho truoc. */
    @Query("""
            SELECT rule
            FROM FoodIncompatibility rule
            WHERE rule.active = true
              AND rule.tagA IN :tagCodes
              AND rule.tagB IN :tagCodes
            """)
    List<FoodIncompatibility> findActiveWithinTags(@Param("tagCodes") Collection<String> tagCodes);

    /** Quy tac dang bat co it nhat mot tag nam trong tap tag cho truoc. */
    @Query("""
            SELECT rule
            FROM FoodIncompatibility rule
            WHERE rule.active = true
              AND (rule.tagA IN :tagCodes OR rule.tagB IN :tagCodes)
            """)
    List<FoodIncompatibility> findActiveTouchingTags(@Param("tagCodes") Collection<String> tagCodes);
}
