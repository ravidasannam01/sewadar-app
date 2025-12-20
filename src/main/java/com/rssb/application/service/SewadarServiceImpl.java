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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

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
        
        // Check if this is the first sewadar (no incharge exists) - allow creating as incharge
        boolean noInchargeExists = sewadarRepository.findByRole(com.rssb.application.entity.Role.INCHARGE).isEmpty();
        
        Sewadar sewadar = mapToEntity(request, noInchargeExists);
        Sewadar savedSewadar = sewadarRepository.save(sewadar);
        log.info("Sewadar created with id: {} and role: {}", savedSewadar.getId(), savedSewadar.getRole());
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
        sewadar.setJoiningDate(request.getJoiningDate());
        sewadar.setProfession(request.getProfession());

        // Role cannot be changed via regular update - use promoteToIncharge endpoint

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

    @Override
    public SewadarResponse promoteToIncharge(Long sewadarId, Long inchargeId) {
        log.info("Incharge {} promoting sewadar {} to incharge", inchargeId, sewadarId);
        
        Sewadar incharge = sewadarRepository.findById(inchargeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", inchargeId));
        
        if (incharge.getRole() != com.rssb.application.entity.Role.INCHARGE) {
            throw new IllegalArgumentException("Only incharge can promote sewadars to incharge");
        }
        
        Sewadar sewadar = sewadarRepository.findById(sewadarId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", sewadarId));
        
        sewadar.setRole(com.rssb.application.entity.Role.INCHARGE);
        Sewadar updated = sewadarRepository.save(sewadar);
        log.info("Sewadar {} promoted to incharge", sewadarId);
        return mapToResponse(updated);
    }

    // Keep this method for backward compatibility but mark as deprecated
    @Deprecated
    private Sewadar mapToEntity(SewadarRequest request) {
        return mapToEntity(request, false);
    }

    private Sewadar mapToEntity(SewadarRequest request, boolean allowInchargeCreation) {
        Sewadar.SewadarBuilder builder = Sewadar.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dept(request.getDept())
                .mobile(request.getMobile())
                .remarks(request.getRemarks())
                .joiningDate(request.getJoiningDate())
                .profession(request.getProfession());

        // Role assignment logic:
        // 1. If no incharge exists and allowInchargeCreation=true, create as INCHARGE
        // 2. If role is explicitly requested and allowInchargeCreation=true, use it
        // 3. Otherwise, default to SEWADAR
        if (allowInchargeCreation) {
            if (request.getRole() != null && !request.getRole().isEmpty()) {
                try {
                    builder.role(com.rssb.application.entity.Role.valueOf(request.getRole().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    builder.role(com.rssb.application.entity.Role.SEWADAR);
                }
            } else {
                // First user becomes incharge automatically
                builder.role(com.rssb.application.entity.Role.INCHARGE);
                log.info("No incharge exists - creating first sewadar as INCHARGE");
            }
        } else {
            // Regular creation - always SEWADAR
            builder.role(com.rssb.application.entity.Role.SEWADAR);
        }

        // Encrypt password (default password if not provided)
        String password = request.getPassword() != null && !request.getPassword().isEmpty() 
                ? request.getPassword() 
                : "password123"; // Default password
        builder.password(passwordEncoder.encode(password));

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
                .remarks(sewadar.getRemarks())
                .role(sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR")
                .joiningDate(sewadar.getJoiningDate())
                .profession(sewadar.getProfession());

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

