package com.rssb.application.service;

import com.rssb.application.dto.LoginRequest;
import com.rssb.application.dto.LoginResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.repository.SewadarRepository;
import com.rssb.application.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final SewadarRepository sewadarRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for zonal_id: {}", request.getZonalId());

        // Use zonalId directly as String (no parsing needed)
        Sewadar sewadar = sewadarRepository.findByZonalId(request.getZonalId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid zonal ID or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), sewadar.getPassword())) {
            throw new IllegalArgumentException("Invalid zonal ID or password");
        }

        // Generate JWT token with String zonalId
        String role = sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR";
        String token = jwtUtil.generateToken(sewadar.getZonalId(), role);

        // Map to response
        SewadarResponse sewadarResponse = SewadarResponse.builder()
                .zonalId(sewadar.getZonalId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .location(sewadar.getLocation())
                .role(role)
                .profession(sewadar.getProfession())
                .joiningDate(sewadar.getJoiningDate())
                .dateOfBirth(sewadar.getDateOfBirth())
                .emergencyContact(sewadar.getEmergencyContact())
                .emergencyContactRelationship(sewadar.getEmergencyContactRelationship())
                .photoUrl(sewadar.getPhotoUrl())
                .build();

        log.info("Login successful for sewadar: {}", sewadar.getZonalId());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .sewadar(sewadarResponse)
                .expiresIn(jwtUtil.getExpirationDateFromToken(token).getTime() / 1000)
                .build();
    }

    @Override
    public void logout(String token) {
        // For JWT, logout is handled client-side by removing the token
        // In a stateless system, we don't need server-side logout
        // But we can add token blacklisting if needed
        log.info("Logout requested for token");
    }

    @Override
    public Boolean validateToken(String token) {
        try {
            return !jwtUtil.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

