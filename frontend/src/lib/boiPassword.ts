/** BOI PDF password: first 4 letters of first name (or full if shorter) + DDMM from DOB. */
export function computeBoiPdfPassword(customerName: string, dateOfBirth: string): string | null {
  if (!customerName?.trim() || !dateOfBirth?.trim()) return null

  const firstWord = customerName.trim().split(/\s+/)[0].toUpperCase()
  const namePart = firstWord.length >= 4 ? firstWord.slice(0, 4) : firstWord

  const [year, month, day] = dateOfBirth.split('-').map(Number)
  if (!year || !month || !day) return null

  const dd = String(day).padStart(2, '0')
  const mm = String(month).padStart(2, '0')
  return `${namePart}${dd}${mm}`
}
