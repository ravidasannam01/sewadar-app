package com.rssb.application.service;

import com.rssb.application.dto.AddressResponse;
import com.rssb.application.dto.AddressRequest;

import java.util.List;

/**
 * Service interface for Address operations.
 */
public interface AddressService {
    List<AddressResponse> getAllAddresses();
    AddressResponse getAddressById(Long id);
    AddressResponse createAddress(AddressRequest request);
    AddressResponse updateAddress(Long id, AddressRequest request);
    void deleteAddress(Long id);
}

