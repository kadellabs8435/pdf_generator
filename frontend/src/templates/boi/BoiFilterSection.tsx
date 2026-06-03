import type { CSSProperties } from 'react'
import { boiTokens } from '@/templates/shared/tokens/boi'
import { formatSbiDashDate } from '@/templates/shared/formatters'

type BoiFilterSectionProps = {
  periodFrom: string
  periodTo: string
}

export function BoiFilterSection({ periodFrom, periodTo }: BoiFilterSectionProps) {
  const f = boiTokens.filter
  const rows = [
    { label: 'Transaction Date', from: formatSbiDashDate(periodFrom), to: formatSbiDashDate(periodTo) },
    { label: 'Amount', from: '-', to: '-' },
    { label: 'Cheque', from: '-', to: '-' },
  ] as const

  const rowStyle: CSSProperties = {
    display: 'flex',
    alignItems: 'baseline',
    marginBottom: f.rowGap,
    fontSize: f.fontSize,
    lineHeight: f.lineHeight,
    fontFamily: boiTokens.page.fontFamily,
    color: boiTokens.colors.textDark,
  }

  return (
    <div style={{ width: f.width, maxWidth: '100%', marginTop: f.sectionMarginTop }}>
      {rows.map((row, index) => (
        <div key={row.label} style={{ ...rowStyle, marginTop: index === 0 ? 0 : 0 }}>
          <span style={{ width: f.labelWidth, flexShrink: 0, fontWeight: 700 }}>{row.label}</span>
          <span style={{ width: f.fromWidth, flexShrink: 0, fontWeight: 700 }}>
            from: {row.from}
          </span>
          <span style={{ flexShrink: 0, fontWeight: 700, marginLeft: f.toGap }}>
            to: {row.to}
          </span>
        </div>
      ))}
    </div>
  )
}

export function BoiTransactionTypeLine() {
  return (
    <p
      style={{
        marginTop: '7px',
        marginBottom: '6px',
        fontSize: boiTokens.filter.fontSize,
        fontWeight: 700,
        fontFamily: boiTokens.page.fontFamily,
        color: boiTokens.colors.textDark,
      }}
    >
      Transaction type: All
    </p>
  )
}
