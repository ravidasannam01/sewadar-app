/**
 * Utility functions for role-based access control
 */

/**
 * Check if user has incharge-level permissions (ADMIN or INCHARGE)
 * @param {Object} user - User object with role property
 * @returns {boolean} - True if user is ADMIN or INCHARGE
 */
export const isAdminOrIncharge = (user) => {
  return user?.role === 'ADMIN' || user?.role === 'INCHARGE'
}

/**
 * Check if user is ADMIN
 * @param {Object} user - User object with role property
 * @returns {boolean} - True if user is ADMIN
 */
export const isAdmin = (user) => {
  return user?.role === 'ADMIN'
}

/**
 * Check if user is INCHARGE (but not ADMIN)
 * @param {Object} user - User object with role property
 * @returns {boolean} - True if user is INCHARGE
 */
export const isIncharge = (user) => {
  return user?.role === 'INCHARGE'
}

/**
 * Check if user is SEWADAR
 * @param {Object} user - User object with role property
 * @returns {boolean} - True if user is SEWADAR
 */
export const isSewadar = (user) => {
  return user?.role === 'SEWADAR'
}

