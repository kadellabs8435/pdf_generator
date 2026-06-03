import { boiTokens } from '@/templates/shared/tokens/boi'
import type { StatementTemplateProps } from '@/templates/shared/types'
import { PageWrapper } from '@/templates/shared/components/PageWrapper'
import { BankHeader } from '@/templates/shared/components/BankHeader'
import { BoiTransactionTable } from '@/templates/boi/BoiTransactionTable'
import { BoiFilterSection, BoiTransactionTypeLine } from '@/templates/boi/BoiFilterSection'
import { StatementFooter } from '@/templates/shared/components/Footer'
import { formatSbiDashDate, formatBalance } from '@/templates/shared/formatters'

export const BOI_EXPORT_ROOT_ID = 'boi-statement-export-root'

/**
 * BOI statement template — transaction table grid matches original PDF reference.
 */
export function BOIStatement({ data, watermark, exportRootId = BOI_EXPORT_ROOT_ID }: StatementTemplateProps) {
  const t = boiTokens
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
      <img src="/Bank_Of_India_Logo.png" alt="Bank of India" style={{ height: t.logo.height, marginBottom: '8px' }} />

      <BankHeader
        title="Account Statement"
        subtitle={`${formatSbiDashDate(data.periodFrom)} to ${formatSbiDashDate(data.periodTo)}`}
        titleSize="14px"
        subtitleSize="8px"
      />

      <div className="my-3 grid grid-cols-2 gap-4 text-[8px]">
        <div>
          <p className="font-bold">{data.customerName}</p>
          <p className="text-[#555555]">{data.address}</p>
          <p className="text-[#555555]">{data.city}, {data.state} - {data.pincode}</p>
        </div>
        <div>
          <p><span className="text-[#555555]">Account No. </span>{data.accountNumber}</p>
          <p><span className="text-[#555555]">Customer ID </span>{data.customerId ?? '—'}</p>
          <p><span className="text-[#555555]">IFSC </span>{data.ifscCode}</p>
        </div>
      </div>

      <BoiFilterSection periodFrom={data.periodFrom} periodTo={data.periodTo} />
      <BoiTransactionTypeLine />

      <div style={{ marginTop: '4px' }}>
        <BoiTransactionTable rows={data.transactions} />
      </div>

      <p className="mt-3 text-[7px] text-[#555555]">
        Closing Balance: {formatBalance(data.closingBalance)}
      </p>

      <StatementFooter generatedOn={data.generatedOn} />
      <p className="mt-6 text-center text-[10px] text-amber-700">
        BOI layout migration in progress — download PDF still uses backend iText + password protection
      </p>
    </PageWrapper>
  )
}
