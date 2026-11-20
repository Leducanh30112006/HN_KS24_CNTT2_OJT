package com.quickshop.identity.service.impl;

import com.quickshop.common.exception.ResourceNotFoundException;
import com.quickshop.identity.dto.AddressRequest;
import com.quickshop.identity.dto.AddressResponse;
import com.quickshop.identity.entity.UserAddress;
import com.quickshop.identity.repository.UserAddressRepository;
import com.quickshop.identity.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "id", addressId));
        return AddressResponse.fromEntity(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        long existingCount = userAddressRepository.countByUserId(userId);
        boolean shouldBeDefault = existingCount == 0 || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && existingCount > 0) {
            userAddressRepository.resetDefaultAddressForUser(userId);
        }

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .recipientName(request.getRecipientName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .streetAddress(request.getStreetAddress().trim())
                .ward(request.getWard() != null ? request.getWard().trim() : null)
                .district(request.getDistrict() != null ? request.getDistrict().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .fullAddress(request.getFullAddress() != null && !request.getFullAddress().trim().isEmpty()
                        ? request.getFullAddress().trim()
                        : null)
                .tag(request.getTag() != null && !request.getTag().trim().isEmpty()
                        ? request.getTag().trim()
                        : "Nhà riêng")
                .isDefault(shouldBeDefault)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        address.generateFullAddressIfEmpty();
        UserAddress saved = userAddressRepository.save(address);
        log.info("Đã tạo địa chỉ mới #{} cho user #{}, tọa độ: lat={}, lon={}",
                saved.getId(), userId, saved.getLatitude(), saved.getLongitude());
        return AddressResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "id", addressId));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            userAddressRepository.resetDefaultAddressForUser(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && address.isDefault()) {
            // Không được hủy default nếu đây là địa chỉ duy nhất
            if (userAddressRepository.countByUserId(userId) > 1) {
                address.setDefault(false);
            }
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setWard(request.getWard() != null ? request.getWard().trim() : null);
        address.setDistrict(request.getDistrict() != null ? request.getDistrict().trim() : null);
        address.setCity(request.getCity() != null ? request.getCity().trim() : null);

        if (request.getFullAddress() != null && !request.getFullAddress().trim().isEmpty()) {
            address.setFullAddress(request.getFullAddress().trim());
        } else {
            address.setFullAddress(null);
            address.generateFullAddressIfEmpty();
        }

        if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
            address.setTag(request.getTag().trim());
        }

        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        UserAddress updated = userAddressRepository.save(address);
        log.info("Đã cập nhật địa chỉ #{} cho user #{}, tọa độ: lat={}, lon={}",
                updated.getId(), userId, updated.getLatitude(), updated.getLongitude());
        return AddressResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "id", addressId));

        boolean wasDefault = address.isDefault();
        userAddressRepository.delete(address);
        log.info("Đã xóa địa chỉ #{} của user #{}", addressId, userId);

        // Nếu xóa địa chỉ mặc định, tự động gán địa chỉ còn lại đầu tiên làm mặc định
        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setDefault(true);
                userAddressRepository.save(newDefault);
                log.info("Đã tự động đặt địa chỉ #{} làm mặc định mới cho user #{}", newDefault.getId(), userId);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", "id", addressId));

        if (!address.isDefault()) {
            userAddressRepository.resetDefaultAddressForUser(userId);
            address.setDefault(true);
            address = userAddressRepository.save(address);
        }
        return AddressResponse.fromEntity(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(AddressResponse::fromEntity)
                .orElseGet(() -> {
                    List<UserAddress> list = userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
                    return list.isEmpty() ? null : AddressResponse.fromEntity(list.get(0));
                });
    }
}
