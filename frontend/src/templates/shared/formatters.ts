const KOTAK_DATE = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
function parseIsoDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function formatKotakDate(iso: string): string {
  if (!iso) return ''
  return KOTAK_DATE.format(parseIsoDate(iso)).replace(/ /g, ' ')
}

export function formatSbiDashDate(iso: string): string {
  if (!iso) return ''
  const d = parseIsoDate(iso)
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  return `${dd}-${mm}-${d.getFullYear()}`
}

export function formatAmount(value: number | undefined | null, wholeRupees = false): string {
  if (value == null || value === 0) return ''
  const abs = Math.abs(value)
  const n = wholeRupees ? Math.trunc(abs) : abs
  return n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatBalance(value: number | undefined | null): string {
  if (value == null) return '0.00'
  return value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** BOI debit/credit cells — plain amount without ₹ prefix. */
export function formatBoiPlainAmount(value: number | undefined | null): string {
  if (value == null || value === 0) return ''
  return Math.abs(value).toFixed(2)
}

/** BOI balance column — ₹ prefix with Indian grouping. */
export function formatBoiBalance(value: number | undefined | null): string {
  if (value == null) return ''
  return `₹ ${formatBalance(value)}`
}

export function deriveCrn(accountNumber: string): string {
  if (!accountNumber || accountNumber.length < 3) return 'xxxxxx000'
  return `xxxxxx${accountNumber.slice(-3)}`
}

export function deriveMicr(ifsc: string): string {
  if (!ifsc || ifsc.length < 6) return '000000000'
  return `${ifsc.slice(-6)}507`
}

export function formatPeriodKotak(from: string, to: string): string {
  return `${formatKotakDate(from)} - ${formatKotakDate(to)}`
}

export function formatGeneratedOn(): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date())
}
