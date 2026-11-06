package com.quickshop.identity.service;

import com.quickshop.identity.dto.CreateShopRequest;
import com.quickshop.identity.dto.ShopResponse;
import com.quickshop.identity.dto.UpdateShopRequest;
import com.quickshop.identity.entity.ShopStatus;

import java.util.List;

public interface ShopService {

    ShopResponse registerShop(Long ownerId, CreateShopRequest request);

    ShopResponse updateShop(Long ownerId, Long shopId, UpdateShopRequest request);

    ShopResponse getMyShop(Long ownerId);

    ShopResponse getShopById(Long id);

    ShopResponse getShopByOwnerId(Long ownerId);

    List<ShopResponse> getAllShops();

    List<ShopResponse> getShopsByStatus(ShopStatus status);

    ShopResponse approveShop(Long id);

    ShopResponse rejectShop(Long id);
}
