import type { StatementTransactionRow } from '@/templates/shared/types'
import { formatAmount, formatBalance } from '@/templates/shared/formatters'

export type TransactionColumn = {
  key: string
  header: string
  width: string
  align?: 'left' | 'center' | 'right'
  render: (row: StatementTransactionRow, openingBalance: number) => string
}

type TransactionTableProps = {
  columns: TransactionColumn[]
  rows: StatementTransactionRow[]
  openingBalance: number
  headerBg?: string
  headerColor?: string
  headerHeight?: string
  rowMinHeight?: string
  fontSize?: string
  headerFontSize?: string
  verticalBordersInHeader?: boolean
  verticalBordersInBody?: boolean
  borderColor?: string
  showOpeningRow?: boolean
  wholeRupees?: boolean
}

export function TransactionTable({
  columns,
  rows,
  openingBalance,
  headerBg = '#8e8e8e',
  headerColor = '#ffffff',
  headerHeight = '22px',
  rowMinHeight = '15px',
  fontSize = '7px',
  headerFontSize = '8.5px',
  verticalBordersInHeader = true,
  verticalBordersInBody = false,
  borderColor = '#bdbdbd',
  showOpeningRow = true,
  wholeRupees = false,
}: TransactionTableProps) {
  const fmtDr = (v: number) => formatAmount(v, wholeRupees)
  const fmtCr = (v: number) => formatAmount(v, wholeRupees)

  return (
    <table className="w-full border-collapse" style={{ fontSize }}>
      <thead>
        <tr style={{ backgroundColor: headerBg, color: headerColor }}>
          {columns.map((col, i) => (
            <th
              key={col.key}
              className="font-bold text-center"
              style={{
                width: col.width,
                minHeight: headerHeight,
                padding: '5px 4px',
                borderRight: verticalBordersInHeader && i < columns.length - 1 ? '0.5px solid #fff' : undefined,
              }}
            >
              <span style={{ fontSize: headerFontSize }}>{col.header}</span>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {showOpeningRow && (
          <tr style={{ minHeight: rowMinHeight, borderTop: `0.5px solid ${borderColor}`, borderBottom: `0.5px solid ${borderColor}` }}>
            <td className="text-center font-bold" style={{ padding: '1px 4px' }}>-</td>
            <td className="text-center" style={{ padding: '1px 4px' }}>-</td>
            <td className="text-center font-bold" style={{ padding: '1px 4px' }}>Opening Balance</td>
            <td style={{ padding: '1px 4px' }}>-</td>
            <td className="text-right" style={{ padding: '1px 4px' }}>-</td>
            <td className="text-right" style={{ padding: '1px 4px' }}>-</td>
            <td className="text-right" style={{ padding: '1px 4px' }}>{formatBalance(openingBalance)}</td>
          </tr>
        )}
        {rows.map((row) => (
          <tr
            key={row.serial}
            style={{
              minHeight: rowMinHeight,
              borderBottom: `0.5px solid ${borderColor}`,
            }}
          >
            {columns.map((col, i) => {
              return (
                <td
                  key={col.key}
                  className={
                    col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : 'text-left'
                  }
                  style={{
                    padding: '1px 4px',
                    verticalAlign: 'top',
                    lineHeight: 1.12,
                    borderRight:
                      verticalBordersInBody && i < columns.length - 1 ? `0.5px solid ${borderColor}` : undefined,
                  }}
                >
                  {col.key === 'debit' ? fmtDr(row.debit) : col.key === 'credit' ? fmtCr(row.credit) : col.key === 'balance' ? formatBalance(row.balance) : col.render(row, openingBalance)}
                </td>
              )
            })}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export const kotakTransactionColumns: TransactionColumn[] = [
  { key: 'serial', header: '#', width: '34px', align: 'center', render: (r) => String(r.serial) },
  { key: 'date', header: 'Date', width: '92px', align: 'center', render: (r) => r.date },
  { key: 'narration', header: 'Description', width: '235px', align: 'left', render: (r) => r.narration },
  { key: 'reference', header: 'Chq/Ref. No.', width: '128px', align: 'left', render: (r) => r.reference },
  { key: 'debit', header: 'Withdrawal (Dr.)', width: '95px', align: 'right', render: () => '' },
  { key: 'credit', header: 'Deposit (Cr.)', width: '95px', align: 'right', render: () => '' },
  { key: 'balance', header: 'Balance', width: '95px', align: 'right', render: () => '' },
]
