import type { Statement } from '@/types'

/** Normalized view model for bank statement templates (frontend source of truth). */
export interface StatementViewModel {
  bankCode: string
  customerName: string
  customerId?: string
  address: string
  city: string
  state: string
  pincode: string
  accountNumber: string
  accountType: string
  branchName: string
  ifscCode: string
  periodFrom: string
  periodTo: string
  periodLabel: string
  openingBalance: number
  closingBalance: number
  transactions: StatementTransactionRow[]
  generatedOn: string
  crn: string
  micr: string
}

export interface StatementTransactionRow {
  serial: number
  date: string
  narration: string
  reference: string
  debit: number
  credit: number
  balance: number
}

export type StatementTemplateProps = {
  data: StatementViewModel
  watermark?: string
  /** Root element id used when capturing HTML for PDF export */
  exportRootId?: string
}

export function isSupportedLayoutBank(bankCode: string): boolean {
  return ['SBI', 'KOTAK', 'BOI'].includes(bankCode.toUpperCase())
}

export type StatementSource = Statement
