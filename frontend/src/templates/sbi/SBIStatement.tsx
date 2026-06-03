import { sbiTokens } from '@/templates/shared/tokens/sbi'
import type { StatementTemplateProps } from '@/templates/shared/types'
import { PageWrapper } from '@/templates/shared/components/PageWrapper'
import { BankHeader } from '@/templates/shared/components/BankHeader'
import { RedSectionBar } from '@/templates/shared/components/RedSectionBar'
import { TransactionTable } from '@/templates/shared/components/TransactionTable'
import { StatementFooter } from '@/templates/shared/components/Footer'
import { formatSbiDashDate, formatBalance } from '@/templates/shared/formatters'

export const SBI_EXPORT_ROOT_ID = 'sbi-statement-export-root'

const sbiColumns = [
  { key: 'date', header: 'Txn Date', width: '54px', align: 'center' as const, render: (r: { date: string }) => r.date },
  { key: 'narration', header: 'Description', width: '154px', align: 'left' as const, render: (r: { narration: string }) => r.narration },
  { key: 'reference', header: 'Ref No./Cheque No.', width: '54px', align: 'left' as const, render: (r: { reference: string }) => r.reference },
  { key: 'debit', header: 'Debit', width: '77px', align: 'right' as const, render: () => '' },
  { key: 'credit', header: 'Credit', width: '77px', align: 'right' as const, render: () => '' },
  { key: 'balance', header: 'Balance', width: '85px', align: 'right' as const, render: () => '' },
]

/**
 * SBI statement template — Phase 2 scaffold.
 * Expand sections to match SbiPdfService during SBI migration.
 */
export function SBIStatement({ data, watermark, exportRootId = SBI_EXPORT_ROOT_ID }: StatementTemplateProps) {
  const t = sbiTokens
  return (
    <PageWrapper
      exportRootId={exportRootId}
      watermark={watermark}
      style={{
        width: t.page.width,
        minHeight: t.page.minHeight,
        padding: `${t.page.marginTop} ${t.page.marginRight} 20px ${t.page.marginLeft}`,
        fontFamily: t.page.fontFamily,
      }}
    >
      <div className="mb-2 flex items-center gap-3">
        <img src="/sbiLogo.png" alt="SBI" style={{ height: '32px' }} />
        <BankHeader
          title="STATE BANK OF INDIA"
          subtitle={`Statement Period: ${formatSbiDashDate(data.periodFrom)} to ${formatSbiDashDate(data.periodTo)}`}
          titleSize={t.title.fontSize}
          subtitleSize="8px"
        />
      </div>

      <div className="mb-2 rounded border border-[#b8c9e0] bg-[#e8eef7] p-1 text-[8.5px] font-bold">
        Account Summary — {data.customerName}
      </div>

      <RedSectionBar
        title="Statement of Account"
        backgroundColor={t.colors.primary}
        height="23px"
        fontSize="11px"
      />

      <TransactionTable
        columns={sbiColumns}
        rows={data.transactions}
        openingBalance={data.openingBalance}
        headerBg={t.colors.primary}
        headerHeight={t.table.headerMinHeight}
        rowMinHeight={t.table.rowMinHeight}
        showOpeningRow={false}
      />

      <p className="mt-4 text-center text-[8px]">
        Closing Balance: {formatBalance(data.closingBalance)}
      </p>

      <StatementFooter generatedOn={data.generatedOn} />
      <p className="mt-6 text-center text-[10px] text-amber-700">
        SBI layout migration in progress — tune tokens in templates/shared/tokens/sbi.ts
      </p>
    </PageWrapper>
  )
}
