export const parseBackendLocalDateTime = (value) => {
  if (!value) return null
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return null
  return d
}

/**
 * Returns a human-friendly countdown string with hours+minutes precision.
 * Example: "5h 03m", "12m", "<1m"
 */
export const formatCountdownHm = (deadlineDate, nowMs = Date.now()) => {
  if (!deadlineDate) return null
  const msLeft = deadlineDate.getTime() - nowMs
  if (msLeft <= 0) return '0m'

  const totalMinutes = Math.floor(msLeft / 60000)
  if (totalMinutes <= 0) return '<1m'

  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60

  if (hours > 0) {
    return `${hours}h ${String(minutes).padStart(2, '0')}m`
  }
  return `${minutes}m`
}

export const isPastDeadline = (deadlineDate, nowMs = Date.now()) => {
  if (!deadlineDate) return false
  return nowMs > deadlineDate.getTime()
}


