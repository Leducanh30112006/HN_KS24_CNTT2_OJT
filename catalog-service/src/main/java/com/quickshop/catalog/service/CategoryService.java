package com.quickshop.catalog.service;

import com.quickshop.catalog.dto.CategoryDto;
import com.quickshop.catalog.entity.Category;
import com.quickshop.catalog.repository.CategoryRepository;
import com.quickshop.catalog.repository.ProductRepository;
import com.quickshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogCacheService cacheService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public List<CategoryDto> getAllCategories() {
        // 1. Try Redis cache (TTL 10m)
        List<CategoryDto> cached = cacheService.getCachedCategories();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. Cache miss -> DB lookup
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, String> nameMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<CategoryDto> dtos = allCategories.stream().map(c -> {
            long count = productRepository.countByCategoryId(c.getId());
            return CategoryDto.builder()
                    .id(c.getId())
                    .parentId(c.getParentId())
                    .parentName(c.getParentId() != null ? nameMap.get(c.getParentId()) : null)
                    .name(c.getName())
                    .slug(c.getSlug())
                    .icon(c.getIcon())
                    .productCount(count)
                    .createdAt(c.getCreatedAt())
                    .children(new ArrayList<>())
                    .build();
        }).collect(Collectors.toList());

        // 3. Write back to Redis
        cacheService.cacheCategories(dtos);
        return dtos;
    }

    public List<CategoryDto> getCategoryTree() {
        // 1. Try Redis cache
        List<CategoryDto> cached = cacheService.getCachedCategoryTree();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 2. Cache miss -> Build tree from all categories
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, CategoryDto> dtoMap = new LinkedHashMap<>();

        for (Category c : allCategories) {
            long count = productRepository.countByCategoryId(c.getId());
            CategoryDto dto = CategoryDto.builder()
                    .id(c.getId())
                    .parentId(c.getParentId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .icon(c.getIcon())
                    .productCount(count)
                    .createdAt(c.getCreatedAt())
                    .children(new ArrayList<>())
                    .build();
            dtoMap.put(c.getId(), dto);
        }

        List<CategoryDto> rootCategories = new ArrayList<>();

        for (CategoryDto dto : dtoMap.values()) {
            if (dto.getParentId() != null && dtoMap.containsKey(dto.getParentId())) {
                CategoryDto parent = dtoMap.get(dto.getParentId());
                dto.setParentName(parent.getName());
                parent.getChildren().add(dto);
            } else {
                rootCategories.add(dto);
            }
        }

        // Aggregate product counts for parents
        for (CategoryDto root : rootCategories) {
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                long totalCount = root.getProductCount() != null ? root.getProductCount() : 0L;
                for (CategoryDto child : root.getChildren()) {
                    totalCount += (child.getProductCount() != null ? child.getProductCount() : 0L);
                }
                root.setProductCount(totalCount);
            }
        }

        // 3. Write back to Redis
        cacheService.cacheCategoryTree(rootCategories);
        return rootCategories;
    }

    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String parentName = null;
        if (category.getParentId() != null) {
            parentName = categoryRepository.findById(category.getParentId())
                    .map(Category::getName)
                    .orElse(null);
        }

        long count = productRepository.countByCategoryId(category.getId());
        List<Category> children = categoryRepository.findByParentId(category.getId());
        List<CategoryDto> childDtos = children.stream().map(ch -> CategoryDto.builder()
                .id(ch.getId())
                .parentId(ch.getParentId())
                .name(ch.getName())
                .slug(ch.getSlug())
                .icon(ch.getIcon())
                .productCount(productRepository.countByCategoryId(ch.getId()))
                .createdAt(ch.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return CategoryDto.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .parentName(parentName)
                .name(category.getName())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .productCount(count)
                .createdAt(category.getCreatedAt())
                .children(childDtos)
                .build();
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(dto.getName());
        }

        Long parentId = dto.getParentId();
        String parentName = null;
        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại: ID " + parentId));
            parentName = parent.getName();
        }

        Category category = Category.builder()
                .parentId(parentId)
                .name(dto.getName())
                .slug(slug)
                .icon(dto.getIcon() != null ? dto.getIcon() : "Sparkles")
                .build();

        Category saved = categoryRepository.save(category);
        cacheService.evictCategoryCaches();

        return CategoryDto.builder()
                .id(saved.getId())
                .parentId(saved.getParentId())
                .parentName(parentName)
                .name(saved.getName())
                .slug(saved.getSlug())
                .icon(saved.getIcon())
                .productCount(0L)
                .createdAt(saved.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            category.setName(dto.getName());
        }
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            category.setSlug(dto.getSlug());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }

        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new IllegalArgumentException("Danh mục không thể là danh mục cha của chính nó");
            }
            if (!categoryRepository.existsById(dto.getParentId())) {
                throw new IllegalArgumentException("Danh mục cha không tồn tại: ID " + dto.getParentId());
            }
            category.setParentId(dto.getParentId());
        }

        Category saved = categoryRepository.save(category);
        cacheService.evictCategoryCaches();

        String parentName = null;
        if (saved.getParentId() != null) {
            parentName = categoryRepository.findById(saved.getParentId())
                    .map(Category::getName)
                    .orElse(null);
        }

        long count = productRepository.countByCategoryId(saved.getId());
        return CategoryDto.builder()
                .id(saved.getId())
                .parentId(saved.getParentId())
                .parentName(parentName)
                .name(saved.getName())
                .slug(saved.getSlug())
                .icon(saved.getIcon())
                .productCount(count)
                .createdAt(saved.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }

        long childCount = categoryRepository.countByParentId(id);
        if (childCount > 0) {
            throw new IllegalStateException("Không thể xóa danh mục đang có " + childCount + " danh mục con. Hãy xóa hoặc di chuyển các danh mục con trước.");
        }

        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Không thể xóa danh mục đang chứa " + productCount + " sản phẩm.");
        }

        categoryRepository.deleteById(id);
        cacheService.evictCategoryCaches();
    }

    public static String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
