package com.haui.UrbanMart.service;

import com.haui.UrbanMart.dto.request.CategoryCreateRequest;
import com.haui.UrbanMart.dto.request.CategoryUpdateRequest;
import com.haui.UrbanMart.dto.response.CategoryDto;
import com.haui.UrbanMart.entity.Category;
import com.haui.UrbanMart.exception.BadRequestException;
import com.haui.UrbanMart.exception.ResourceNotFoundException;
import com.haui.UrbanMart.repository.CategoryRepository;
import com.haui.UrbanMart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
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

    @Transactional
    public CategoryDto createCategoryTree(CategoryCreateRequest request) {
        Category root = createNode(request, null);
        return toDto(root);
    }

    private Category createNode(CategoryCreateRequest request, Category parent) {
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setParent(parent);
        category = categoryRepository.save(category);

        if (request.getChildren() != null) {
            for (CategoryCreateRequest childRequest : request.getChildren()) {
                createNode(childRequest, category);
            }
        }

        return category;
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        category.setName(request.getName().trim());

        if (request.getParentId() == null) {
            category.setParent(null);
        } else {
            var descendants = new HashSet<>(categoryRepository.findSelfAndDescendantIds(id));

            if (descendants.contains(request.getParentId())) {
                throw new BadRequestException("Không thể chuyển danh mục vào chính nó hoặc danh mục con");
            }

            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục cha"));
            category.setParent(parent);
        }

        categoryRepository.save(category);
        return toDto(category);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        if (categoryRepository.existsByParent_Id(id)) {
            throw new BadRequestException("Không thể xóa danh mục đang có danh mục con");
        }

        if (productRepository.existsByCategory_Id(id)) {
            throw new BadRequestException("Không thể xóa danh mục đang được sản phẩm sử dụng");
        }

        categoryRepository.delete(category);
    }
    private CategoryDto toDto(Category category) {
        List<CategoryDto> children = categoryRepository.findAll().stream()
                .filter(item -> item.getParent() != null
                        && item.getParent().getId().equals(category.getId()))
                .map(this::toDto)
                .toList();

        return new CategoryDto(category.getId(), category.getName(), children);
    }
}