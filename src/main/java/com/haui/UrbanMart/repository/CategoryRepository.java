package com.haui.UrbanMart.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.haui.UrbanMart.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT id, parent_id
                FROM categories
                WHERE id = :categoryId
                UNION ALL
                SELECT child.id, child.parent_id
                FROM categories child
                JOIN category_tree parent ON child.parent_id = parent.id
            )
            SELECT id FROM category_tree
            """, nativeQuery = true)
    List<UUID> findSelfAndDescendantIds(UUID categoryId);
}