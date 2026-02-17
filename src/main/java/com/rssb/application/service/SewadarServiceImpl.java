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
    public SewadarResponse getSewadarById(String zonalId) {
        log.info("Fetching sewadar with zonal_id: {}", zonalId);
        Sewadar sewadar = sewadarRepository.findByZonalId(zonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", zonalId));
        return mapToResponse(sewadar);
    }

    @Override
    public SewadarResponse createSewadar(SewadarRequest request) {
        log.info("Creating new sewadar: {}", request);
        
        // Check if this is the first sewadar (no incharge exists) - allow creating as incharge
        boolean noInchargeExists = sewadarRepository.findByRole(com.rssb.application.entity.Role.INCHARGE).isEmpty();
        
        Sewadar sewadar = mapToEntity(request, noInchargeExists);
        Sewadar savedSewadar = sewadarRepository.save(sewadar);
        log.info("Sewadar created with zonal_id: {} and role: {}", savedSewadar.getZonalId(), savedSewadar.getRole());
        return mapToResponse(savedSewadar);
    }

    @Override
    public SewadarResponse updateSewadar(String zonalId, SewadarRequest request) {
        log.info("Updating sewadar with zonal_id: {}", zonalId);
        Sewadar sewadar = sewadarRepository.findByZonalId(zonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", zonalId));
        
        // zonalId cannot be changed - it's the organizational identity

        sewadar.setFirstName(request.getFirstName());
        sewadar.setLastName(request.getLastName());
        sewadar.setLocation(request.getLocation());
        sewadar.setMobile(request.getMobile());
        sewadar.setRemarks(request.getRemarks());
        sewadar.setJoiningDate(request.getJoiningDate());
        sewadar.setProfession(request.getProfession());
        sewadar.setDateOfBirth(request.getDateOfBirth());
        sewadar.setEmergencyContact(request.getEmergencyContact());
        sewadar.setEmergencyContactRelationship(request.getEmergencyContactRelationship());
        sewadar.setPhotoUrl(request.getPhotoUrl());
        sewadar.setAadharNumber(request.getAadharNumber());
        sewadar.setFatherHusbandName(request.getFatherHusbandName());
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                sewadar.setGender(com.rssb.application.entity.Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}, skipping", request.getGender());
            }
        }
        sewadar.setScreenerCode(request.getScreenerCode());
        sewadar.setSatsangPlace(request.getSatsangPlace());
        sewadar.setEmailId(request.getEmailId());

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            log.info("Updating password for sewadar with zonal_id: {}", zonalId);
            sewadar.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Role cannot be changed via regular update - use promoteToIncharge endpoint

        // Handle address - create or update address if address fields are provided
        if (hasAddressFields(request)) {
            Address address = createOrUpdateAddress(sewadar.getAddress(), request);
            sewadar.setAddress(address);
        } else {
            // If no address fields provided, remove address relationship
            sewadar.setAddress(null);
        }

        // Handle languages - only update if provided, otherwise keep existing
        if (request.getLanguages() != null) {
            // Remove existing languages
            sewadar.getLanguages().clear();
            // Add new languages if list is not empty
            if (!request.getLanguages().isEmpty()) {
                for (String lang : request.getLanguages()) {
                    if (lang != null && !lang.trim().isEmpty()) {
                        com.rssb.application.entity.SewadarLanguage language = com.rssb.application.entity.SewadarLanguage.builder()
                                .sewadar(sewadar)
                                .language(lang.trim())
                                .build();
                        sewadar.getLanguages().add(language);
                    }
                }
            }
        }

        Sewadar updatedSewadar = sewadarRepository.save(sewadar);
        log.info("Sewadar updated with zonal_id: {}", updatedSewadar.getZonalId());
        return mapToResponse(updatedSewadar);
    }

    @Override
    public void deleteSewadar(String zonalId) {
        log.info("Deleting sewadar with zonal_id: {}", zonalId);
        Sewadar sewadar = sewadarRepository.findByZonalId(zonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", zonalId));
        sewadarRepository.delete(sewadar);
        log.info("Sewadar deleted with zonal_id: {}", zonalId);
    }

    @Override
    public SewadarResponse promoteToIncharge(String sewadarZonalId, String inchargeZonalId, String password) {
        log.info("Incharge {} promoting sewadar {} to incharge", inchargeZonalId, sewadarZonalId);
        
        Sewadar incharge = sewadarRepository.findByZonalId(inchargeZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", inchargeZonalId));
        
        if (!com.rssb.application.util.PermissionUtil.hasInchargePermission(incharge)) {
            throw new IllegalArgumentException("Only incharge or admin can promote sewadars to incharge");
        }
        
        // Verify incharge password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required to promote sewadar");
        }
        
        if (!passwordEncoder.matches(password, incharge.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));
        
        if (sewadar.getRole() == com.rssb.application.entity.Role.INCHARGE) {
            throw new IllegalArgumentException("Sewadar is already an incharge");
        }
        
        sewadar.setRole(com.rssb.application.entity.Role.INCHARGE);
        Sewadar updated = sewadarRepository.save(sewadar);
        log.info("Sewadar {} promoted to incharge", sewadarZonalId);
        return mapToResponse(updated);
    }

    @Override
    public SewadarResponse demoteToSewadar(String sewadarZonalId, String inchargeZonalId, String password) {
        log.info("Incharge {} demoting sewadar {} to sewadar", inchargeZonalId, sewadarZonalId);
        
        Sewadar incharge = sewadarRepository.findByZonalId(inchargeZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", inchargeZonalId));
        
        if (!com.rssb.application.util.PermissionUtil.hasInchargePermission(incharge)) {
            throw new IllegalArgumentException("Only incharge or admin can demote incharges to sewadar");
        }
        
        // Verify incharge password
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required to demote incharge");
        }
        
        if (!passwordEncoder.matches(password, incharge.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));
        
        if (sewadar.getRole() != com.rssb.application.entity.Role.INCHARGE) {
            throw new IllegalArgumentException("Sewadar is not an incharge");
        }
        
        // Prevent self-demotion
        if (sewadarZonalId.equals(inchargeZonalId)) {
            throw new IllegalArgumentException("Cannot demote yourself");
        }
        
        sewadar.setRole(com.rssb.application.entity.Role.SEWADAR);
        Sewadar updated = sewadarRepository.save(sewadar);
        log.info("Sewadar {} demoted to sewadar", sewadarZonalId);
        return mapToResponse(updated);
    }

    // Keep this method for backward compatibility but mark as deprecated
    @Deprecated
    private Sewadar mapToEntity(SewadarRequest request) {
        return mapToEntity(request, false);
    }

    private Sewadar mapToEntity(SewadarRequest request, boolean allowInchargeCreation) {
        // Check if zonalId already exists
        if (request.getZonalId() != null && !request.getZonalId().trim().isEmpty()) {
            sewadarRepository.findByZonalId(request.getZonalId())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Sewadar with zonal ID '" + request.getZonalId() + "' already exists");
                    });
        }
        
        Sewadar.SewadarBuilder builder = Sewadar.builder()
                .zonalId(request.getZonalId()) // Set zonalId from request (required)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .location(request.getLocation())
                .mobile(request.getMobile())
                .remarks(request.getRemarks())
                .joiningDate(request.getJoiningDate())
                .profession(request.getProfession())
                .dateOfBirth(request.getDateOfBirth())
                .emergencyContact(request.getEmergencyContact())
                .emergencyContactRelationship(request.getEmergencyContactRelationship())
                .photoUrl(request.getPhotoUrl())
                .aadharNumber(request.getAadharNumber())
                .fatherHusbandName(request.getFatherHusbandName())
                .screenerCode(request.getScreenerCode())
                .satsangPlace(request.getSatsangPlace())
                .emailId(request.getEmailId());
        
        // Handle gender enum
        if (request.getGender() != null && !request.getGender().trim().isEmpty()) {
            try {
                builder.gender(com.rssb.application.entity.Gender.valueOf(request.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid gender value: {}, skipping", request.getGender());
            }
        }

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

        Sewadar sewadar = builder.build();
        
        // Handle languages
        if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
            for (String lang : request.getLanguages()) {
                com.rssb.application.entity.SewadarLanguage language = com.rssb.application.entity.SewadarLanguage.builder()
                        .sewadar(sewadar)
                        .language(lang)
                        .build();
                sewadar.getLanguages().add(language);
            }
        }
        
        return sewadar;
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
                .zonalId(sewadar.getZonalId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .location(sewadar.getLocation())
                .mobile(sewadar.getMobile())
                .remarks(sewadar.getRemarks())
                .role(sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR")
                .joiningDate(sewadar.getJoiningDate())
                .profession(sewadar.getProfession())
                .dateOfBirth(sewadar.getDateOfBirth())
                .emergencyContact(sewadar.getEmergencyContact())
                .emergencyContactRelationship(sewadar.getEmergencyContactRelationship())
                .photoUrl(sewadar.getPhotoUrl())
                .aadharNumber(sewadar.getAadharNumber())
                .fatherHusbandName(sewadar.getFatherHusbandName())
                .gender(sewadar.getGender() != null ? sewadar.getGender().name() : null)
                .screenerCode(sewadar.getScreenerCode())
                .satsangPlace(sewadar.getSatsangPlace())
                .emailId(sewadar.getEmailId());
        
        // Map languages
        if (sewadar.getLanguages() != null && !sewadar.getLanguages().isEmpty()) {
            List<String> languages = sewadar.getLanguages().stream()
                    .map(com.rssb.application.entity.SewadarLanguage::getLanguage)
                    .collect(Collectors.toList());
            builder.languages(languages);
        }

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

