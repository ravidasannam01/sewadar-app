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
        log.info("Login attempt for mobile: {}", request.getMobile());

        Sewadar sewadar = sewadarRepository.findByMobile(request.getMobile())
                .orElseThrow(() -> new IllegalArgumentException("Invalid mobile number or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), sewadar.getPassword())) {
            throw new IllegalArgumentException("Invalid mobile number or password");
        }

        // Generate JWT token
        String role = sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR";
        String token = jwtUtil.generateToken(sewadar.getId(), role);

        // Map to response
        SewadarResponse sewadarResponse = SewadarResponse.builder()
                .id(sewadar.getId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .dept(sewadar.getDept())
                .role(role)
                .profession(sewadar.getProfession())
                .joiningDate(sewadar.getJoiningDate())
                .build();

        log.info("Login successful for sewadar: {}", sewadar.getId());

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

