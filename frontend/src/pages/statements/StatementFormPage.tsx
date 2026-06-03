import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { statementService, templateService } from '@/services/statementService'
import { getErrorMessage } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const schema = z
  .object({
  bankCode: z.string().min(1),
  customerName: z.string().min(1),
  customerId: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.enum(['male', 'female']),
  email: z.string().email().optional().or(z.literal('')),
  address: z.string().min(1),
  city: z.string().min(1),
  state: z.string().min(1),
  pincode: z.string().min(1),
  accountNumber: z.string().min(1),
  accountType: z.string().min(1),
  branchName: z.string().min(1),
  ifscCode: z.string().min(1),
  fromDate: z.string().min(1),
  toDate: z.string().min(1),
  openingBalance: z.coerce.number<number>().min(0),
  salary: z.boolean(),
  salaryCompanyName: z.string().optional(),
  salaryAmount: z.coerce.number<number>().optional(),
  salaryDayOfMonth: z.coerce.number<number>().int().optional(),
  upi: z.boolean(),
  atm: z.boolean(),
  emi: z.boolean(),
  interest: z.boolean(),
  minTransactions: z.coerce.number<number>().min(1),
  maxTransactions: z.coerce.number<number>().min(1),
})
  .superRefine((data, ctx) => {
    if (data.bankCode === 'BOI' && !data.dateOfBirth?.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['dateOfBirth'],
        message: 'Date of birth is required for Bank of India statements',
      })
    }
    if (data.bankCode === 'BOI' && !data.customerId?.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['customerId'],
        message: 'Customer ID is required for Bank of India statements',
      })
    }
    if (data.salary) {
      if (!data.salaryCompanyName?.trim()) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['salaryCompanyName'],
          message: 'Company name is required when salary is selected',
        })
      }
      if (data.salaryAmount == null || Number.isNaN(data.salaryAmount) || data.salaryAmount <= 0) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['salaryAmount'],
          message: 'Monthly salary amount is required when salary is selected',
        })
      }
      const day = data.salaryDayOfMonth
      if (day == null || Number.isNaN(day) || day < 1 || day > 28) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['salaryDayOfMonth'],
          message: 'Salary credit day (1–28) is required when salary is selected',
        })
      }
    }
  })

type FormValues = z.infer<typeof schema>

export function StatementFormPage() {
  const navigate = useNavigate()
  const [error, setError] = useState('')

  const { data: templates = [] } = useQuery({
    queryKey: ['templates'],
    queryFn: templateService.list,
  })

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      bankCode: 'SBI',
      gender: 'male',
      accountType: 'Savings',
      openingBalance: 50000,
      salary: true,
      salaryCompanyName: '',
      salaryAmount: 45000,
      salaryDayOfMonth: 1,
      upi: true,
      atm: true,
      emi: true,
      interest: true,
      minTransactions: 8,
      maxTransactions: 20,
    },
  })

  const salaryEnabled = form.watch('salary')

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      setError('')
      const draft = await statementService.createDraft({
        bankCode: values.bankCode,
        customerDetails: {
          customerName: values.customerName,
          ...(values.customerId ? { customerId: values.customerId.trim() } : {}),
          ...(values.dateOfBirth ? { dateOfBirth: values.dateOfBirth } : {}),
          gender: values.gender,
          email: values.email || undefined,
          address: values.address,
          city: values.city,
          state: values.state,
          pincode: values.pincode,
        },
        accountDetails: {
          accountNumber: values.accountNumber,
          accountType: values.accountType,
          branchName: values.branchName,
          ifscCode: values.ifscCode,
        },
        period: { fromDate: values.fromDate, toDate: values.toDate },
        openingBalance: values.openingBalance,
        transactionSettings: {
          salary: values.salary,
          ...(values.salary
            ? {
                salaryCompanyName: values.salaryCompanyName?.trim(),
                salaryAmount: values.salaryAmount,
                salaryDayOfMonth: values.salaryDayOfMonth,
              }
            : {}),
          upi: values.upi,
          atm: values.atm,
          emi: values.emi,
          interest: values.interest,
          minTransactions: values.minTransactions,
          maxTransactions: values.maxTransactions,
        },
      })
      navigate(`/statements/${draft.id}`)
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h2 className="text-3xl font-bold">Create Statement</h2>
        <p className="text-[var(--color-muted-foreground)]">Fill customer, account, and transaction settings.</p>
      </div>

      <form onSubmit={onSubmit} className="space-y-6">
        <Card>
          <CardHeader><CardTitle>Bank Template</CardTitle></CardHeader>
          <CardContent>
            <Label>Bank</Label>
            <Select value={form.watch('bankCode')} onValueChange={(v) => form.setValue('bankCode', v)}>
              <SelectTrigger className="mt-2"><SelectValue placeholder="Select bank" /></SelectTrigger>
              <SelectContent>
                {templates.map((t) => (
                  <SelectItem key={t.code} value={t.code}>{t.displayName}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Customer Details</CardTitle></CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div><Label>Customer Name</Label><Input className="mt-2" {...form.register('customerName')} /></div>
            {form.watch('bankCode') === 'BOI' && (
              <div>
                <Label>Customer ID <span className="text-red-600">*</span></Label>
                <Input className="mt-2" {...form.register('customerId')} />
                {form.formState.errors.customerId && (
                  <p className="mt-1 text-sm text-red-600">{form.formState.errors.customerId.message}</p>
                )}
              </div>
            )}
            <div>
              <Label>Date of Birth {form.watch('bankCode') === 'BOI' && <span className="text-red-600">*</span>}</Label>
              <Input type="date" className="mt-2" {...form.register('dateOfBirth')} />
              {form.formState.errors.dateOfBirth && (
                <p className="mt-1 text-sm text-red-600">{form.formState.errors.dateOfBirth.message}</p>
              )}
            </div>
            <div><Label>Gender</Label>
              <Select value={form.watch('gender')} onValueChange={(v) => form.setValue('gender', v as 'male' | 'female')}>
                <SelectTrigger className="mt-2"><SelectValue placeholder="Select gender" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="male">Male</SelectItem>
                  <SelectItem value="female">Female</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div><Label>Email</Label><Input type="email" className="mt-2" {...form.register('email')} /></div>
            <div><Label>Pincode</Label><Input className="mt-2" {...form.register('pincode')} /></div>
            <div className="md:col-span-2"><Label>Address</Label><Input className="mt-2" {...form.register('address')} /></div>
            <div><Label>City</Label><Input className="mt-2" {...form.register('city')} /></div>
            <div><Label>State</Label><Input className="mt-2" {...form.register('state')} /></div>
            {form.watch('bankCode') === 'BOI' && (
              <div className="md:col-span-2 rounded-md border border-blue-200 bg-blue-50 p-3 text-sm text-blue-900">
                <p className="font-medium">BOI downloaded PDF password</p>
                <p className="mt-1">Used when you download and open the saved PDF (preview in browser does not need a password).</p>
                <p className="mt-1">Format: first 4 letters of first name + DDMM of date of birth.</p>
                <p className="mt-1 text-blue-800">Example: Pavan Singh, DOB 24/01/1998 → <strong>PAVA2401</strong></p>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Account Details</CardTitle></CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div><Label>Account Number</Label><Input className="mt-2" {...form.register('accountNumber')} /></div>
            <div><Label>Account Type</Label><Input className="mt-2" {...form.register('accountType')} /></div>
            <div><Label>Branch Name</Label><Input className="mt-2" {...form.register('branchName')} /></div>
            <div><Label>IFSC Code</Label><Input className="mt-2" {...form.register('ifscCode')} /></div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Statement Period &amp; Balance</CardTitle></CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div><Label>From Date</Label><Input type="date" className="mt-2" {...form.register('fromDate')} /></div>
            <div><Label>To Date</Label><Input type="date" className="mt-2" {...form.register('toDate')} /></div>
            <div><Label>Opening Balance</Label><Input type="number" className="mt-2" {...form.register('openingBalance')} /></div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Transaction Settings</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap gap-6">
              {(['salary', 'upi', 'atm', 'emi', 'interest'] as const).map((key) => (
                <label key={key} className="flex items-center gap-2 text-sm capitalize">
                  <Checkbox checked={form.watch(key)} onCheckedChange={(v) => form.setValue(key, Boolean(v))} />
                  {key}
                </label>
              ))}
            </div>

            {salaryEnabled && (
              <div className="space-y-4 rounded-md border border-[var(--color-border)] bg-[var(--color-muted)]/20 p-4">
                <p className="text-sm font-medium">Salary details</p>
                <p className="text-sm text-[var(--color-muted-foreground)]">
                  Each month on the chosen day, a NEFT credit from your employer is added to the statement
                  {form.watch('bankCode') === 'BOI' ? ' (BOI-style NEFT remarks).' : '.'}
                </p>
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="md:col-span-2">
                    <Label>Company name <span className="text-red-600">*</span></Label>
                    <Input
                      className="mt-2"
                      placeholder="Employer / company name"
                      {...form.register('salaryCompanyName')}
                    />
                    {form.formState.errors.salaryCompanyName && (
                      <p className="mt-1 text-sm text-red-600">{form.formState.errors.salaryCompanyName.message}</p>
                    )}
                  </div>
                  <div>
                    <Label>Monthly salary amount <span className="text-red-600">*</span></Label>
                    <Input
                      type="number"
                      step="0.01"
                      min={1}
                      className="mt-2"
                      {...form.register('salaryAmount')}
                    />
                    {form.formState.errors.salaryAmount && (
                      <p className="mt-1 text-sm text-red-600">{form.formState.errors.salaryAmount.message}</p>
                    )}
                  </div>
                  <div>
                    <Label>Salary credit day (1–28) <span className="text-red-600">*</span></Label>
                    <Input
                      type="number"
                      min={1}
                      max={28}
                      className="mt-2"
                      {...form.register('salaryDayOfMonth')}
                    />
                    {form.formState.errors.salaryDayOfMonth && (
                      <p className="mt-1 text-sm text-red-600">{form.formState.errors.salaryDayOfMonth.message}</p>
                    )}
                  </div>
                </div>
              </div>
            )}

            <div className="grid gap-4 md:grid-cols-2">
              <div><Label>Min Transactions</Label><Input type="number" className="mt-2" {...form.register('minTransactions')} /></div>
              <div><Label>Max Transactions</Label><Input type="number" className="mt-2" {...form.register('maxTransactions')} /></div>
            </div>
          </CardContent>
        </Card>

        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" size="lg">Save Draft &amp; Continue</Button>
      </form>
    </div>
  )
}
