import type { StatementViewModel } from '@/templates/shared/types'
import { KotakStatement } from '@/templates/kotak/KotakStatement'
import { SBIStatement } from '@/templates/sbi/SBIStatement'
import { BOIStatement } from '@/templates/boi/BOIStatement'

type BankStatementPreviewProps = {
  data: StatementViewModel
  watermark?: string
}

export function BankStatementPreview({ data, watermark }: BankStatementPreviewProps) {
  switch (data.bankCode) {
    case 'KOTAK':
      return <KotakStatement data={data} watermark={watermark} />
    case 'SBI':
      return <SBIStatement data={data} watermark={watermark} />
    case 'BOI':
      return <BOIStatement data={data} watermark={watermark} />
    default:
      return (
        <p className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          Layout preview is not available for bank {data.bankCode}. Use legacy PDF preview.
        </p>
      )
  }
}

export function getExportRootId(bankCode: string): string | null {
  switch (bankCode.toUpperCase()) {
    case 'KOTAK':
      return 'kotak-statement-export-root'
    case 'SBI':
      return 'sbi-statement-export-root'
    case 'BOI':
      return 'boi-statement-export-root'
    default:
      return null
  }
}
