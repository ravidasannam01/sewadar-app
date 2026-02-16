package com.rssb.application.util;

import com.rssb.application.entity.Role;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.entity.Program;

/**
 * Utility class for permission checks.
 * Role hierarchy: ADMIN > INCHARGE > SEWADAR
 * - ADMIN: All permissions (can do everything)
 * - INCHARGE: SEWADAR permissions + incharge permissions (can do everything sewadars can do, plus manage programs)
 * - SEWADAR: Basic permissions (apply to programs, view own data, etc.)
 */
public class PermissionUtil {

    /**
     * Check if a user has permission to perform sewadar-level actions.
     * ADMIN, INCHARGE, and SEWADAR all have sewadar permissions.
     * 
     * @param user The user performing the action
     * @return true if user has sewadar permissions (ADMIN, INCHARGE, or SEWADAR)
     */
    public static boolean hasSewadarPermission(Sewadar user) {
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN || 
               user.getRole() == Role.INCHARGE || 
               user.getRole() == Role.SEWADAR;
    }

    /**
     * Check if a user has permission to perform an action.
     * ADMIN always has permission.
     * 
     * @param user The user performing the action
     * @return true if user has permission (ADMIN or INCHARGE)
     */
    public static boolean hasInchargePermission(Sewadar user) {
        if (user == null) {
            return false;
        }
        return user.getRole() == Role.ADMIN || user.getRole() == Role.INCHARGE;
    }

    /**
     * Check if a user can manage a specific program.
     * ADMIN can manage any program.
     * INCHARGE can only manage programs they created.
     * 
     * @param user The user performing the action
     * @param program The program to check
     * @return true if user can manage the program
     */
    public static boolean canManageProgram(Sewadar user, Program program) {
        if (user == null || program == null) {
            return false;
        }
        
        // ADMIN can manage any program
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        
        // INCHARGE can only manage programs they created
        if (user.getRole() == Role.INCHARGE) {
            return program.getCreatedBy() != null && 
                   program.getCreatedBy().getZonalId().equals(user.getZonalId());
        }
        
        return false;
    }

    /**
     * Check if a user can manage a program by comparing zonal IDs.
     * ADMIN can manage any program.
     * INCHARGE can only manage programs they created.
     * 
     * @param userZonalId The zonal ID of the user
     * @param userRole The role of the user
     * @param programCreatorZonalId The zonal ID of the program creator
     * @return true if user can manage the program
     */
    public static boolean canManageProgram(String userZonalId, Role userRole, String programCreatorZonalId) {
        if (userZonalId == null || programCreatorZonalId == null) {
            return false;
        }
        
        // ADMIN can manage any program
        if (userRole == Role.ADMIN) {
            return true;
        }
        
        // INCHARGE can only manage programs they created
        if (userRole == Role.INCHARGE) {
            return userZonalId.equals(programCreatorZonalId);
        }
        
        return false;
    }

    /**
     * Check if a role is ADMIN or INCHARGE.
     * 
     * @param role The role to check
     * @return true if role is ADMIN or INCHARGE
     */
    public static boolean isAdminOrIncharge(Role role) {
        return role == Role.ADMIN || role == Role.INCHARGE;
    }
}

