import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { statementService } from '@/services/statementService'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { StatusBadge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { StatementStatus } from '@/types'

export function HistoryPage() {
  const [bankCode, setBankCode] = useState<string>('')
  const [status, setStatus] = useState<StatementStatus | ''>('')

  const { data, isLoading } = useQuery({
    queryKey: ['statement-history', bankCode, status],
    queryFn: () => statementService.getHistory({
      page: 0,
      size: 50,
      bankCode: bankCode || undefined,
      status: status || undefined,
    }),
  })

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold">Statement History</h2>
        <p className="text-[var(--color-muted-foreground)]">Browse and reopen generated statements.</p>
      </div>

      <div className="flex flex-wrap gap-4">
        <Select value={bankCode || 'ALL'} onValueChange={(v) => setBankCode(v === 'ALL' ? '' : v)}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Bank" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Banks</SelectItem>
            {['SBI', 'HDFC', 'ICICI', 'AXIS', 'KOTAK', 'CANARA'].map((b) => (
              <SelectItem key={b} value={b}>{b}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={status || 'ALL'} onValueChange={(v) => setStatus(v === 'ALL' ? '' : v as StatementStatus)}>
          <SelectTrigger className="w-48"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Statuses</SelectItem>
            {(['DRAFT', 'PREVIEWED', 'GENERATED', 'APPROVED'] as StatementStatus[]).map((s) => (
              <SelectItem key={s} value={s}>{s}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <Card>
        <CardHeader><CardTitle>Statements</CardTitle></CardHeader>
        <CardContent className="overflow-x-auto">
          {isLoading ? <p>Loading...</p> : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left">
                  <th className="p-2">Customer</th>
                  <th className="p-2">Bank</th>
                  <th className="p-2">Period</th>
                  <th className="p-2">Status</th>
                  <th className="p-2">Created</th>
                  <th className="p-2"></th>
                </tr>
              </thead>
              <tbody>
                {(data?.content ?? []).map((s) => (
                  <tr key={s.id} className="border-b">
                    <td className="p-2">{s.customerDetails.customerName}</td>
                    <td className="p-2">{s.bankCode}</td>
                    <td className="p-2">{s.period.fromDate} to {s.period.toDate}</td>
                    <td className="p-2"><StatusBadge status={s.status} /></td>
                    <td className="p-2">{new Date(s.createdAt).toLocaleDateString()}</td>
                    <td className="p-2">
                      <Link to={`/statements/${s.id}`} className="text-[var(--color-primary)] hover:underline">Open</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {(data?.content ?? []).length === 0 && !isLoading && (
            <p className="text-sm text-[var(--color-muted-foreground)]">No statements found.</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
