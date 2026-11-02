package com.quickshop.catalog.repository;

import com.quickshop.catalog.entity.Product;
import com.quickshop.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByShopId(Long shopId);
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByStatusIn(Collection<ProductStatus> statuses);

    Page<Product> findByShopId(Long shopId, Pageable pageable);
    Page<Product> findByShopIdAndStatus(Long shopId, ProductStatus status, Pageable pageable);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findByStatusIn(Collection<ProductStatus> statuses, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    long countByShopId(Long shopId);
    long countByShopIdAndStatus(Long shopId, ProductStatus status);
    long countByStatus(ProductStatus status);
    long countByCategoryId(Long categoryId);
}
