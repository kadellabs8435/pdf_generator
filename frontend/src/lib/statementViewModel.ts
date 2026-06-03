import type { Statement } from '@/types'
import type { StatementViewModel, StatementTransactionRow } from '@/templates/shared/types'
import {
  deriveCrn,
  deriveMicr,
  formatGeneratedOn,
  formatKotakDate,
  formatPeriodKotak,
  formatSbiDashDate,
} from '@/templates/shared/formatters'

export function toStatementViewModel(statement: Statement): StatementViewModel {
  const bank = statement.bankCode.toUpperCase()
  const account = statement.accountDetails
  const customer = statement.customerDetails
  const period = statement.period

  const formatTxnDate = (iso: string) => {
    if (bank === 'SBI' || bank === 'BOI') return formatSbiDashDate(iso)
    return formatKotakDate(iso)
  }

  const transactions: StatementTransactionRow[] = (statement.transactions ?? []).map((txn, i) => ({
    serial: i + 1,
    date: formatTxnDate(txn.date),
    narration: txn.narration,
    reference: txn.reference,
    debit: txn.debit ?? 0,
    credit: txn.credit ?? 0,
    balance: txn.balance ?? 0,
  }))

  const closing =
    statement.closingBalance ??
    (transactions.length > 0 ? transactions[transactions.length - 1].balance : statement.openingBalance)

  return {
    bankCode: bank,
    customerName: customer.customerName,
    customerId: customer.customerId,
    address: customer.address,
    city: customer.city,
    state: customer.state,
    pincode: customer.pincode,
    accountNumber: account.accountNumber,
    accountType: account.accountType,
    branchName: account.branchName,
    ifscCode: account.ifscCode,
    periodFrom: period.fromDate,
    periodTo: period.toDate,
    periodLabel: formatPeriodKotak(period.fromDate, period.toDate),
    openingBalance: statement.openingBalance,
    closingBalance: closing,
    transactions,
    generatedOn: formatGeneratedOn(),
    crn: deriveCrn(account.accountNumber),
    micr: deriveMicr(account.ifscCode),
  }
}
