package com.rssb.application.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract user information from SecurityContext.
 */
public class UserContextUtil {
    
    /**
     * Get the current user's zonal ID from SecurityContext.
     * 
     * @return User's zonal ID (String) or null if not authenticated
     */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String) {
            return (String) auth.getPrincipal();
        }
        return null;
    }
    
    /**
     * Get the current user's role from SecurityContext.
     * 
     * @return User's role or null if not authenticated
     */
    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            String authority = auth.getAuthorities().iterator().next().getAuthority();
            // Remove "ROLE_" prefix if present
            return authority.startsWith("ROLE_") ? authority.substring(5) : authority;
        }
        return null;
    }
    
    /**
     * Check if user is authenticated.
     * 
     * @return true if user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }
}

