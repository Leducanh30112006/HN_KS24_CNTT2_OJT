package com.quickshop.identity.repository;

import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByOwnerId(Long ownerId);
    Optional<Shop> findByShopName(String shopName);
    boolean existsByShopName(String shopName);
    boolean existsByOwnerId(Long ownerId);
    List<Shop> findByStatus(ShopStatus status);
}
