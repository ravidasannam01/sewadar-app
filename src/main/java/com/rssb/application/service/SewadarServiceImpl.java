package com.rssb.application.service;

import com.rssb.application.dto.AddressResponse;
import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Address;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.AddressRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Sewadar operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SewadarServiceImpl implements SewadarService {

    private final SewadarRepository sewadarRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SewadarResponse> getAllSewadars() {
        log.info("Fetching all sewadars");
        return sewadarRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SewadarResponse getSewadarById(Long id) {
        log.info("Fetching sewadar with id: {}", id);
        Sewadar sewadar = sewadarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", id));
        return mapToResponse(sewadar);
    }

    @Override
    public SewadarResponse createSewadar(SewadarRequest request) {
        log.info("Creating new sewadar: {}", request);
        Sewadar sewadar = mapToEntity(request);
        Sewadar savedSewadar = sewadarRepository.save(sewadar);
        log.info("Sewadar created with id: {}", savedSewadar.getId());
        return mapToResponse(savedSewadar);
    }

    @Override
    public SewadarResponse updateSewadar(Long id, SewadarRequest request) {
        log.info("Updating sewadar with id: {}", id);
        Sewadar sewadar = sewadarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", id));

        sewadar.setFirstName(request.getFirstName());
        sewadar.setLastName(request.getLastName());
        sewadar.setDept(request.getDept());
        sewadar.setMobile(request.getMobile());
        sewadar.setRemarks(request.getRemarks());

        // Handle address - create or update address if address fields are provided
        if (hasAddressFields(request)) {
            Address address = createOrUpdateAddress(sewadar.getAddress(), request);
            sewadar.setAddress(address);
        } else {
            // If no address fields provided, remove address relationship
            sewadar.setAddress(null);
        }

        Sewadar updatedSewadar = sewadarRepository.save(sewadar);
        log.info("Sewadar updated with id: {}", updatedSewadar.getId());
        return mapToResponse(updatedSewadar);
    }

    @Override
    public void deleteSewadar(Long id) {
        log.info("Deleting sewadar with id: {}", id);
        Sewadar sewadar = sewadarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", id));
        sewadarRepository.delete(sewadar);
        log.info("Sewadar deleted with id: {}", id);
    }

    private Sewadar mapToEntity(SewadarRequest request) {
        Sewadar.SewadarBuilder builder = Sewadar.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dept(request.getDept())
                .mobile(request.getMobile())
                .remarks(request.getRemarks());

        // Create address if address fields are provided
        if (hasAddressFields(request)) {
            Address address = createOrUpdateAddress(null, request);
            builder.address(address);
        }

        return builder.build();
    }

    /**
     * Check if request has any address fields filled
     */
    private boolean hasAddressFields(SewadarRequest request) {
        return (request.getAddress1() != null && !request.getAddress1().trim().isEmpty()) ||
               (request.getAddress2() != null && !request.getAddress2().trim().isEmpty()) ||
               (request.getEmail() != null && !request.getEmail().trim().isEmpty());
    }

    /**
     * Create a new address or update existing address with the provided fields
     */
    private Address createOrUpdateAddress(Address existingAddress, SewadarRequest request) {
        if (existingAddress != null) {
            // Update existing address
            if (request.getAddress1() != null && !request.getAddress1().trim().isEmpty()) {
                existingAddress.setAddress1(request.getAddress1().trim());
            }
            if (request.getAddress2() != null) {
                existingAddress.setAddress2(request.getAddress2().trim().isEmpty() ? null : request.getAddress2().trim());
            }
            if (request.getEmail() != null) {
                existingAddress.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
            }
            return addressRepository.save(existingAddress);
        } else {
            // Create new address - address1 is required, others are optional
            String address1 = request.getAddress1() != null ? request.getAddress1().trim() : "";
            if (address1.isEmpty()) {
                // If no address1 but other fields exist, use a placeholder
                address1 = "Address not specified";
            }
            
            Address newAddress = Address.builder()
                    .address1(address1)
                    .address2(request.getAddress2() != null && !request.getAddress2().trim().isEmpty() 
                            ? request.getAddress2().trim() : null)
                    .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() 
                            ? request.getEmail().trim() : null)
                    .build();
            return addressRepository.save(newAddress);
        }
    }

    private SewadarResponse mapToResponse(Sewadar sewadar) {
        SewadarResponse.SewadarResponseBuilder builder = SewadarResponse.builder()
                .id(sewadar.getId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .dept(sewadar.getDept())
                .mobile(sewadar.getMobile())
                .remarks(sewadar.getRemarks());

        if (sewadar.getAddress() != null) {
            Address address = sewadar.getAddress();
            AddressResponse addressResponse = AddressResponse.builder()
                    .id(address.getId())
                    .address1(address.getAddress1())
                    .address2(address.getAddress2())
                    .email(address.getEmail())
                    .build();
            builder.address(addressResponse);
        }

        return builder.build();
    }
}

