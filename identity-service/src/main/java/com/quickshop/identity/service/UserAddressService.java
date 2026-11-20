package com.quickshop.identity.service;

import com.quickshop.identity.dto.AddressRequest;
import com.quickshop.identity.dto.AddressResponse;

import java.util.List;

public interface UserAddressService {

    List<AddressResponse> getUserAddresses(Long userId);

    AddressResponse getAddressById(Long userId, Long addressId);

    AddressResponse createAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);

    AddressResponse getDefaultAddress(Long userId);
}
