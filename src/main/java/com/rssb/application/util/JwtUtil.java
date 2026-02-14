package com.rssb.application.util;

import com.rssb.application.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String zonalId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(zonalId) // Store zonalId (String) directly as subject
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getZonalIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject); // zonalId is stored as subject (String)
    }
    
    // Deprecated: Use getZonalIdFromToken instead
    @Deprecated
    public Long getUserIdFromToken(String token) {
        String zonalId = getZonalIdFromToken(token);
        try {
            return Long.parseLong(zonalId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Zonal ID in token is not a number: " + zonalId);
        }
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("role", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token, String zonalId) {
        final String tokenZonalId = getZonalIdFromToken(token);
        return (tokenZonalId.equals(zonalId) && !isTokenExpired(token));
    }
    
    // Deprecated: Use validateToken(String, String) instead
    @Deprecated
    public Boolean validateToken(String token, Long userId) {
        final String tokenZonalId = getZonalIdFromToken(token);
        return (tokenZonalId.equals(String.valueOf(userId)) && !isTokenExpired(token));
    }
}

