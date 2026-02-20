/**
 * Calculate age in years from date of birth.
 * @param {string|Date|null} dateOfBirth - Date of birth (YYYY-MM-DD or Date)
 * @returns {string|number} Age in years, or empty string if no valid DOB
 */
export function getAgeFromDateOfBirth(dateOfBirth) {
  if (!dateOfBirth) return ''
  const birth = new Date(dateOfBirth)
  if (isNaN(birth.getTime())) return ''
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age >= 0 ? age : ''
}
