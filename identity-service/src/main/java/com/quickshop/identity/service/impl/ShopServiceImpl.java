package com.quickshop.identity.service.impl;

import com.quickshop.common.exception.BadRequestException;
import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.dto.CreateShopRequest;
import com.quickshop.identity.dto.ShopResponse;
import com.quickshop.identity.dto.UpdateShopRequest;
import com.quickshop.identity.entity.Role;
import com.quickshop.identity.entity.Shop;
import com.quickshop.identity.entity.ShopStatus;
import com.quickshop.identity.entity.User;
import com.quickshop.identity.entity.UserStatus;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShopResponse registerShop(Long ownerId, CreateShopRequest request) {
        if (ownerId == null) {
            throw new BadRequestException("Yêu cầu chưa được xác thực danh tính chủ sở hữu");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        if (!owner.isActive() || owner.getStatus() == UserStatus.BLOCKED) {
            throw new BadRequestException("Tài khoản của bạn đã bị khóa hoặc chưa được kích hoạt");
        }

        if (shopRepository.existsByOwnerId(ownerId)) {
            throw new BadRequestException("Tài khoản của bạn đã có hồ sơ gian hàng trên hệ thống, không thể đăng ký thêm");
        }

        if (shopRepository.existsByShopName(request.getShopName())) {
            throw new BadRequestException("Tên gian hàng '" + request.getShopName() + "' đã được sử dụng, vui lòng chọn tên khác");
        }

        Shop shop = Shop.builder()
                .ownerId(ownerId)
                .shopName(request.getShopName().trim())
                .warehouseAddress(request.getWarehouseAddress().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .taxCode(request.getTaxCode() != null ? request.getTaxCode().trim() : null)
                .citizenId(request.getCitizenId() != null ? request.getCitizenId().trim() : null)
                .businessType(StringUtils.hasText(request.getBusinessType()) ? request.getBusinessType().trim() : "INDIVIDUAL")
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bankCode(request.getBankCode().trim())
                .bankAccountNumber(request.getBankAccountNumber().trim())
                .bankAccountName(request.getBankAccountName().trim())
                .status(ShopStatus.PENDING_APPROVAL)
                .build();

        Shop savedShop = shopRepository.save(shop);
        log.info("New shop registered [id={}, name='{}', ownerId={}] with status PENDING_APPROVAL",
                savedShop.getId(), savedShop.getShopName(), ownerId);

        return ShopResponse.fromEntity(savedShop);
    }

    @Override
    @Transactional
    public ShopResponse updateShop(Long ownerId, Long shopId, UpdateShopRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", shopId));

        if (ownerId != null && !shop.getOwnerId().equals(ownerId)) {
            User requestingUser = userRepository.findById(ownerId).orElse(null);
            if (requestingUser == null || requestingUser.getRole() != Role.ROLE_ADMIN) {
                throw new BadRequestException("Bạn không có quyền chỉnh sửa hồ sơ gian hàng này");
            }
        }

        if (StringUtils.hasText(request.getShopName()) && !request.getShopName().trim().equalsIgnoreCase(shop.getShopName())) {
            if (shopRepository.existsByShopName(request.getShopName().trim())) {
                throw new BadRequestException("Tên gian hàng '" + request.getShopName() + "' đã được sử dụng bởi shop khác");
            }
            shop.setShopName(request.getShopName().trim());
        }

        if (StringUtils.hasText(request.getWarehouseAddress())) {
            shop.setWarehouseAddress(request.getWarehouseAddress().trim());
        }
        if (StringUtils.hasText(request.getPhoneNumber())) {
            shop.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (StringUtils.hasText(request.getTaxCode())) {
            shop.setTaxCode(request.getTaxCode().trim());
        }
        if (StringUtils.hasText(request.getCitizenId())) {
            shop.setCitizenId(request.getCitizenId().trim());
        }
        if (StringUtils.hasText(request.getBusinessType())) {
            shop.setBusinessType(request.getBusinessType().trim());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            shop.setLogoUrl(request.getLogoUrl());
        }
        if (StringUtils.hasText(request.getBankCode())) {
            shop.setBankCode(request.getBankCode().trim());
        }
        if (StringUtils.hasText(request.getBankAccountNumber())) {
            shop.setBankAccountNumber(request.getBankAccountNumber().trim());
        }
        if (StringUtils.hasText(request.getBankAccountName())) {
            shop.setBankAccountName(request.getBankAccountName().trim());
        }

        // Nếu shop đang ở trạng thái REJECTED, khi sửa hồ sơ sẽ chuyển lại thành PENDING_APPROVAL để chờ duyệt lại
        if (shop.getStatus() == ShopStatus.REJECTED) {
            shop.setStatus(ShopStatus.PENDING_APPROVAL);
        }

        Shop updated = shopRepository.save(shop);
        log.info("Shop [id={}, name='{}'] profile updated successfully", updated.getId(), updated.getShopName());
        return ShopResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getMyShop(Long ownerId) {
        if (ownerId == null) {
            throw new BadRequestException("Yêu cầu chưa được xác thực danh tính");
        }
        Shop shop = shopRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "ownerId", ownerId));
        return ShopResponse.fromEntity(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));
        return ShopResponse.fromEntity(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShopByOwnerId(Long ownerId) {
        Shop shop = shopRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "ownerId", ownerId));
        return ShopResponse.fromEntity(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAll().stream()
                .map(ShopResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponse> getShopsByStatus(ShopStatus status) {
        return shopRepository.findByStatus(status).stream()
                .map(ShopResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopResponse approveShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.ACTIVE);
        Shop updated = shopRepository.save(shop);

        userRepository.findById(shop.getOwnerId()).ifPresent(user -> {
            user.setRole(Role.ROLE_SELLER);
            userRepository.save(user);
            log.info("Upgraded user [id={}] to ROLE_SELLER", user.getId());
        });

        log.info("Shop [id={}, name='{}'] APPROVED by Admin", updated.getId(), updated.getShopName());
        return ShopResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ShopResponse rejectShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.REJECTED);
        Shop updated = shopRepository.save(shop);
        log.info("Shop [id={}, name='{}'] REJECTED by Admin", updated.getId(), updated.getShopName());
        return ShopResponse.fromEntity(updated);
    }
}
