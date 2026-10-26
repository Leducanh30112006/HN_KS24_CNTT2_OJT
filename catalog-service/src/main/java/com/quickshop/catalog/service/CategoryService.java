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
import java.util.List;
import java.util.Locale;
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
        List<Category> categories = categoryRepository.findAll();
        List<CategoryDto> dtos = categories.stream().map(c -> {
            long count = productRepository.countByCategoryId(c.getId());
            return CategoryDto.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .slug(c.getSlug())
                    .icon(c.getIcon())
                    .productCount(count)
                    .createdAt(c.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        // 3. Write back to Redis
        cacheService.cacheCategories(dtos);
        return dtos;
    }

    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        long count = productRepository.countByCategoryId(category.getId());
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .productCount(count)
                .createdAt(category.getCreatedAt())
                .build();
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto dto) {
        String slug = dto.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(dto.getName());
        }

        Category category = Category.builder()
                .name(dto.getName())
                .slug(slug)
                .icon(dto.getIcon() != null ? dto.getIcon() : "Sparkles")
                .build();

        Category saved = categoryRepository.save(category);
        cacheService.evictCategoryCaches();

        return CategoryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .icon(saved.getIcon())
                .productCount(0L)
                .createdAt(saved.getCreatedAt())
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

        Category saved = categoryRepository.save(category);
        cacheService.evictCategoryCaches();

        long count = productRepository.countByCategoryId(saved.getId());
        return CategoryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .icon(saved.getIcon())
                .productCount(count)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
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
