import type { CSSProperties } from 'react'
import { boiTokens } from '@/templates/shared/tokens/boi'
import type { StatementTransactionRow } from '@/templates/shared/types'
import { formatBoiBalance, formatBoiPlainAmount } from '@/templates/shared/formatters'

type BoiTransactionTableProps = {
  rows: StatementTransactionRow[]
}

const HEADERS = ['Sr No', 'Date', 'Remarks', 'Debit', 'Credit', 'Balance'] as const

/** Split remarks into exactly two lines — reference PDF breaks after 42 chars on line 1. */
function splitRemarksTwoLines(narration: string, line1Chars: number): [string, string] {
  const text = narration.toUpperCase()
  const line1 = text.slice(0, line1Chars)
  const line2 = text.slice(line1Chars) || '\u00A0'
  return [line1, line2]
}

function BoiRemarksCell({ narration }: { narration: string }) {
  const t = boiTokens.table
  const [line1, line2] = splitRemarksTwoLines(narration, t.remarksLine1Chars)

  return (
    <span
      className="block"
      style={{
        fontSize: t.fontSize,
        lineHeight: t.lineHeight,
        minHeight: `calc(${t.fontSize} * ${t.lineHeight} * 2)`,
        paddingRight: '8pt',
        wordBreak: 'break-all',
        overflowWrap: 'anywhere',
      }}
    >
      {line1}
      <br />
      {line2}
    </span>
  )
}

export function BoiTransactionTable({ rows }: BoiTransactionTableProps) {
  const t = boiTokens.table
  const border = `${t.borderWidth} solid ${boiTokens.colors.border}`

  const cellBase: CSSProperties = {
    fontSize: t.fontSize,
    lineHeight: t.lineHeight,
    color: boiTokens.colors.textDark,
    border,
    verticalAlign: 'top',
  }

  const headerCell: CSSProperties = {
    ...cellBase,
    backgroundColor: boiTokens.colors.white,
    fontWeight: 400,
    textAlign: 'left',
    padding: `${t.headerPadV} ${t.headerPadH}`,
    minHeight: t.headerMinHeight,
    verticalAlign: 'middle',
  }

  const bodyCell: CSSProperties = {
    ...cellBase,
    padding: `${t.bodyPadV} ${t.bodyPadH}`,
    minHeight: t.rowMinHeight,
  }

  return (
    <table
      className="border-collapse"
      style={{
        width: t.width,
        maxWidth: '100%',
        tableLayout: 'fixed',
        fontFamily: boiTokens.page.fontFamily,
      }}
    >
      <colgroup>
        {t.colWidths.map((width, i) => (
          <col key={i} style={{ width }} />
        ))}
      </colgroup>
      <thead>
        <tr>
          {HEADERS.map((label) => (
            <th key={label} style={headerCell}>
              {label}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.serial}>
            <td style={{ ...bodyCell, textAlign: 'left' }}>{row.serial}</td>
            <td style={{ ...bodyCell, textAlign: 'left' }}>{row.date}</td>
            <td style={{ ...bodyCell, textAlign: 'left' }}>
              <BoiRemarksCell narration={row.narration} />
            </td>
            <td
              style={{
                ...bodyCell,
                textAlign: 'right',
                paddingRight: t.amountPadRight,
              }}
            >
              {formatBoiPlainAmount(row.debit)}
            </td>
            <td
              style={{
                ...bodyCell,
                textAlign: 'right',
                paddingRight: t.amountPadRight,
              }}
            >
              {formatBoiPlainAmount(row.credit)}
            </td>
            <td
              style={{
                ...bodyCell,
                textAlign: 'right',
                paddingRight: t.amountPadRight,
                whiteSpace: 'nowrap',
              }}
            >
              {formatBoiBalance(row.balance)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
