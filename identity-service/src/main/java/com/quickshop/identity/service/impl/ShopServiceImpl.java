package com.quickshop.identity.service.impl;

import com.quickshop.common.event.ShopApprovedEvent;
import com.quickshop.common.event.ShopRejectedEvent;
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
import com.quickshop.identity.producer.ShopEventProducer;
import com.quickshop.identity.repository.ShopRepository;
import com.quickshop.identity.repository.UserRepository;
import com.quickshop.identity.service.AuditLogService;
import com.quickshop.identity.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ShopEventProducer shopEventProducer;
    private final AuditLogService auditLogService;

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

        auditLogService.logSuccess(ownerId, owner.getUsername(), "SHOP_REGISTER", "Shop", savedShop.getId(),
                "Đăng ký mở gian hàng mới: " + savedShop.getShopName());

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
        return approveShop(id, null);
    }

    @Override
    @Transactional
    public ShopResponse approveShop(Long id, Long adminId) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.ACTIVE);
        shop.setRejectionReason(null);
        Shop updated = shopRepository.save(shop);

        User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
        if (owner != null) {
            owner.setRole(Role.ROLE_SELLER);
            userRepository.save(owner);
            log.info("Upgraded user [id={}, email={}] to ROLE_SELLER", owner.getId(), owner.getEmail());
        }

        // Phát sự kiện ShopApprovedEvent qua RabbitMQ
        ShopApprovedEvent approvedEvent = ShopApprovedEvent.builder()
                .shopId(updated.getId())
                .ownerId(updated.getOwnerId())
                .shopName(updated.getShopName())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerFullName(owner != null ? owner.getFullName() : null)
                .businessType(updated.getBusinessType())
                .citizenId(updated.getCitizenId())
                .approvedBy(adminId)
                .approvedAt(LocalDateTime.now())
                .build();
        shopEventProducer.publishShopApproved(approvedEvent);

        // Ghi Audit Log duyệt Shop
        auditLogService.logSuccess(adminId, resolveAdminUsername(adminId), "SHOP_APPROVED", "Shop", updated.getId(),
                "Phê duyệt hồ sơ gian hàng '" + updated.getShopName() + "' và cấp quyền ROLE_SELLER cho chủ shop");

        log.info("Shop [id={}, name='{}'] APPROVED by Admin [id={}]", updated.getId(), updated.getShopName(), adminId);
        return ShopResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ShopResponse rejectShop(Long id) {
        return rejectShop(id, null, null);
    }

    @Override
    @Transactional
    public ShopResponse rejectShop(Long id, Long adminId, String reason) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(reason);
        Shop updated = shopRepository.save(shop);

        User owner = userRepository.findById(shop.getOwnerId()).orElse(null);

        // Phát sự kiện ShopRejectedEvent qua RabbitMQ
        ShopRejectedEvent rejectedEvent = ShopRejectedEvent.builder()
                .shopId(updated.getId())
                .ownerId(updated.getOwnerId())
                .shopName(updated.getShopName())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerFullName(owner != null ? owner.getFullName() : null)
                .reason(reason)
                .rejectedBy(adminId)
                .rejectedAt(LocalDateTime.now())
                .build();
        shopEventProducer.publishShopRejected(rejectedEvent);

        // Ghi Audit Log từ chối Shop
        auditLogService.logSuccess(adminId, resolveAdminUsername(adminId), "SHOP_REJECTED", "Shop", updated.getId(),
                "Từ chối hồ sơ gian hàng '" + updated.getShopName() + "'. Lý do: " + (reason != null ? reason : "Không đạt tiêu chuẩn kiểm duyệt"));

        log.info("Shop [id={}, name='{}'] REJECTED by Admin [id={}]. Reason: {}", updated.getId(), updated.getShopName(), adminId, reason);
        return ShopResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ShopResponse suspendShop(Long id, Long adminId, String reason) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        String suspendNote = (reason != null && !reason.isBlank())
                ? reason.trim()
                : "Gian hàng tạm đình chỉ bởi quản trị viên";
        shop.setStatus(ShopStatus.SUSPENDED);
        shop.setRejectionReason(suspendNote);
        Shop updated = shopRepository.save(shop);

        auditLogService.logSuccess(adminId, resolveAdminUsername(adminId), "SHOP_SUSPENDED", "Shop", updated.getId(),
                "Tạm khóa gian hàng '" + updated.getShopName() + "'. Lý do: " + suspendNote);

        log.info("Shop [id={}, name='{}'] SUSPENDED by Admin [id={}]. Reason: {}", updated.getId(), updated.getShopName(), adminId, suspendNote);
        return ShopResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ShopResponse activateShop(Long id, Long adminId) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));

        shop.setStatus(ShopStatus.ACTIVE);
        shop.setRejectionReason(null);
        Shop updated = shopRepository.save(shop);

        User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
        if (owner != null && owner.getRole() == Role.ROLE_BUYER) {
            owner.setRole(Role.ROLE_SELLER);
            userRepository.save(owner);
        }

        auditLogService.logSuccess(adminId, resolveAdminUsername(adminId), "SHOP_ACTIVATED", "Shop", updated.getId(),
                "Mở khóa hoạt động trở lại cho gian hàng '" + updated.getShopName() + "'");

        log.info("Shop [id={}, name='{}'] ACTIVATED by Admin [id={}]", updated.getId(), updated.getShopName(), adminId);
        return ShopResponse.fromEntity(updated);
    }

    private String resolveAdminUsername(Long adminId) {
        return adminId != null
                ? userRepository.findById(adminId).map(User::getUsername).orElse("admin")
                : "system_admin";
    }
}
