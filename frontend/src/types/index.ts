export type Role = 'ADMIN' | 'STAFF' | 'VIEWER'

export type StatementStatus = 'DRAFT' | 'PREVIEWED' | 'GENERATED' | 'APPROVED'

export interface AuthUser {
  userId: string
  name: string
  email?: string
  mobile?: string
  role: Role
}

export interface CustomerDetails {
  customerName: string
  customerId?: string
  dateOfBirth?: string
  gender?: string
  email?: string
  address: string
  city: string
  state: string
  pincode: string
}

export interface AccountDetails {
  accountNumber: string
  accountType: string
  branchName: string
  ifscCode: string
}

export interface StatementPeriod {
  fromDate: string
  toDate: string
}

export interface TransactionSettings {
  salary: boolean
  salaryCompanyName?: string
  salaryAmount?: number
  salaryDayOfMonth?: number
  upi: boolean
  atm: boolean
  emi: boolean
  interest: boolean
  minTransactions: number
  maxTransactions: number
}

export interface Transaction {
  date: string
  narration: string
  reference: string
  type: string
  debit: number
  credit: number
  balance: number
}

export interface StatementDraftRequest {
  bankCode: string
  customerDetails: CustomerDetails
  accountDetails: AccountDetails
  period: StatementPeriod
  openingBalance: number
  transactionSettings: TransactionSettings
}

export interface Statement {
  id: string
  bankCode: string
  status: StatementStatus
  customerDetails: CustomerDetails
  accountDetails: AccountDetails
  period: StatementPeriod
  openingBalance: number
  closingBalance?: number
  transactionSettings: TransactionSettings
  transactions: Transaction[]
  pdfPath?: string
  createdByUserId: string
  createdAt: string
  updatedAt: string
}

export interface DashboardStats {
  totalUsers: number
  totalGeneratedPdfs: number
  totalDrafts: number
  totalApproved: number
}

export interface ActivityItem {
  id: string
  userName: string
  action: string
  details: string
  createdAt: string
}

export interface BankTemplate {
  id: string
  code: string
  displayName: string
  active: boolean
}

export interface UserRecord {
  id: string
  name: string
  email?: string
  mobile?: string
  role: Role
  active: boolean
}

export interface BulkJob {
  id: string
  status: string
  totalRows: number
  processedRows: number
  successCount: number
  failureCount: number
  errorReport?: string
  createdAt: string
  completedAt?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
