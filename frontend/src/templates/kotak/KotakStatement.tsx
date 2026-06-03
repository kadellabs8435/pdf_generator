import { kotakTokens } from '@/templates/shared/tokens/kotak'
import type { StatementTemplateProps } from '@/templates/shared/types'
import { PageWrapper } from '@/templates/shared/components/PageWrapper'
import { BankHeader } from '@/templates/shared/components/BankHeader'
import { KotakCustomerInfoSection } from '@/templates/shared/components/CustomerInfoSection'
import { RedSectionBar } from '@/templates/shared/components/RedSectionBar'
import { TransactionTable, kotakTransactionColumns } from '@/templates/shared/components/TransactionTable'
import { StatementFooter } from '@/templates/shared/components/Footer'
import { KotakLogoRow } from '@/templates/kotak/KotakLogoRow'

export const KOTAK_EXPORT_ROOT_ID = 'kotak-statement-export-root'

export function KotakStatement({ data, watermark, exportRootId = KOTAK_EXPORT_ROOT_ID }: StatementTemplateProps) {
  const t = kotakTokens
  const accountType = data.accountType || 'Savings'

  return (
    <PageWrapper
      exportRootId={exportRootId}
      watermark={watermark}
      style={{
        width: t.page.width,
        minHeight: t.page.minHeight,
        paddingTop: t.page.marginTop,
        paddingLeft: t.page.marginLeft,
        paddingRight: t.page.marginRight,
        paddingBottom: t.page.marginBottom,
        fontFamily: t.page.fontFamily,
      }}
    >
      <KotakLogoRow />

      <BankHeader
        title="Account Statement"
        subtitle={data.periodLabel}
        titleSize={t.title.fontSize}
        titleWeight={t.title.fontWeight}
        subtitleSize={t.title.dateSize}
        className="mb-2.5"
      />

      <KotakCustomerInfoSection
        customerName={data.customerName}
        crn={data.crn}
        address={data.address}
        city={data.city}
        state={data.state}
        pincode={data.pincode}
        micr={data.micr}
        ifsc={data.ifscCode}
        accountNumber={data.accountNumber}
        accountType={data.accountType}
        branchName={data.branchName}
        columnGap={t.customer.columnGap}
      />

      <div style={{ marginTop: t.summary.insetH === '12px' ? '14px' : '14px' }}>
        <RedSectionBar
          title={`${accountType} Account Transactions`}
          backgroundColor={t.colors.red}
          height={t.redBar.height}
          fontSize={t.redBar.fontSize}
        />
        <TransactionTable
          columns={kotakTransactionColumns}
          rows={data.transactions}
          openingBalance={data.openingBalance}
          headerBg={t.colors.headerGray}
          headerHeight={t.table.headerHeight}
          rowMinHeight={t.table.rowMinHeight}
          fontSize={t.body.tableFontSize}
          headerFontSize={t.body.tableHeaderSize}
          verticalBordersInHeader
          verticalBordersInBody={false}
          borderColor={t.colors.border}
          wholeRupees={false}
        />
      </div>

      <StatementFooter generatedOn={data.generatedOn} />
    </PageWrapper>
  )
}
