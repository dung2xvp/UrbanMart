package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.response.CategoryDto;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        Map<UUID, List<Category>> childrenByParentId = new LinkedHashMap<>();
        List<Category> rootCategories = new ArrayList<>();

        for (Category category : categories) {
            if (category.getParent() == null) {
                rootCategories.add(category);
            } else {
                UUID parentId = category.getParent().getId();

                childrenByParentId
                        .computeIfAbsent(parentId, key -> new ArrayList<>())
                        .add(category);
            }
        }

        List<CategoryDto> result = new ArrayList<>();

        for (Category rootCategory : rootCategories) {
            result.add(
                    buildCategoryTree(rootCategory, childrenByParentId)
            );
        }
        return result;
    }
    private CategoryDto buildCategoryTree(
            Category category,
            Map<UUID, List<Category>> childrenByParentId
    ) {
        List<CategoryDto> children = new ArrayList<>();

        List<Category> childCategories =
                childrenByParentId.getOrDefault(
                        category.getId(),
                        List.of()
                );

        for (Category childCategory : childCategories) {
            children.add(
                    buildCategoryTree(childCategory, childrenByParentId)
            );
        }

        return new CategoryDto(
                category.getId(),
                category.getName(),
                children
        );
    }
}