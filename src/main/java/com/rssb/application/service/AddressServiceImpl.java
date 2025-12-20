package com.rssb.application.service;

import com.rssb.application.dto.AddressRequest;
import com.rssb.application.dto.AddressResponse;
import com.rssb.application.entity.Address;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Address operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses() {
        log.info("Fetching all addresses");
        return addressRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long id) {
        log.info("Fetching address with id: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));
        return mapToResponse(address);
    }

    @Override
    public AddressResponse createAddress(AddressRequest request) {
        log.info("Creating new address: {}", request);
        Address address = mapToEntity(request);
        Address savedAddress = addressRepository.save(address);
        log.info("Address created with id: {}", savedAddress.getId());
        return mapToResponse(savedAddress);
    }

    @Override
    public AddressResponse updateAddress(Long id, AddressRequest request) {
        log.info("Updating address with id: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));

        address.setAddress1(request.getAddress1());
        address.setAddress2(request.getAddress2());
        address.setEmail(request.getEmail());

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated with id: {}", updatedAddress.getId());
        return mapToResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(Long id) {
        log.info("Deleting address with id: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));
        addressRepository.delete(address);
        log.info("Address deleted with id: {}", id);
    }

    private Address mapToEntity(AddressRequest request) {
        return Address.builder()
                .address1(request.getAddress1())
                .address2(request.getAddress2())
                .email(request.getEmail())
                .build();
    }

    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .address1(address.getAddress1())
                .address2(address.getAddress2())
                .email(address.getEmail())
                .build();
    }
}

