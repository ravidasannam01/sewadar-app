package com.rssb.application.controller;

import com.rssb.application.dto.AddressRequest;
import com.rssb.application.dto.AddressResponse;
import com.rssb.application.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Address operations.
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AddressController {

    private final AddressService addressService;

    /**
     * Get all addresses.
     *
     * @return List of all addresses
     */
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
        log.info("GET /api/addresses - Fetching all addresses");
        List<AddressResponse> addresses = addressService.getAllAddresses();
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get an address by ID.
     *
     * @param id The address ID
     * @return The address response
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Long id) {
        log.info("GET /api/addresses/{} - Fetching address by id", id);
        AddressResponse address = addressService.getAddressById(id);
        return ResponseEntity.ok(address);
    }

    /**
     * Create a new address.
     *
     * @param request The address request DTO
     * @return The created address response
     */
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request) {
        log.info("POST /api/addresses - Creating new address");
        AddressResponse createdAddress = addressService.createAddress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
    }

    /**
     * Update an existing address.
     *
     * @param id      The address ID
     * @param request The address request DTO
     * @return The updated address response
     */
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        log.info("PUT /api/addresses/{} - Updating address", id);
        AddressResponse updatedAddress = addressService.updateAddress(id, request);
        return ResponseEntity.ok(updatedAddress);
    }

    /**
     * Delete an address by ID.
     *
     * @param id The address ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        log.info("DELETE /api/addresses/{} - Deleting address", id);
        addressService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}

