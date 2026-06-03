import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { statementService } from '@/services/statementService'
import { getErrorMessage } from '@/lib/api'
import { useAuthStore } from '@/stores/authStore'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { StatusBadge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { computeBoiPdfPassword } from '@/lib/boiPassword'
import { toStatementViewModel } from '@/lib/statementViewModel'
import { captureStatementHtml } from '@/lib/exportStatementHtml'
import { BankStatementPreview, getExportRootId } from '@/templates/BankStatementPreview'
import { isSupportedLayoutBank } from '@/templates/shared/types'

export function StatementDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const hasRole = useAuthStore((s) => s.hasRole)
  const [pdfUrl, setPdfUrl] = useState<string | null>(null)
  const [layoutPdfUrl, setLayoutPdfUrl] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [previewTab, setPreviewTab] = useState<'layout' | 'legacy'>('layout')

  const { data: statement, isLoading } = useQuery({
    queryKey: ['statement', id],
    queryFn: () => statementService.getById(id!),
    enabled: !!id,
  })

  const layoutViewModel = useMemo(
    () => (statement ? toStatementViewModel(statement) : null),
    [statement]
  )
  const supportsLayout = statement ? isSupportedLayoutBank(statement.bankCode) : false
  const exportRootId = statement ? getExportRootId(statement.bankCode) : null

  const generateMutation = useMutation({
    mutationFn: () => statementService.generateTransactions(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['statement', id] }),
    onError: (e) => setError(getErrorMessage(e)),
  })

  const previewMutation = useMutation({
    mutationFn: () => statementService.previewPdf(id!),
    onSuccess: (blob) => {
      if (pdfUrl) URL.revokeObjectURL(pdfUrl)
      setPdfUrl(URL.createObjectURL(blob))
      setPreviewTab('legacy')
      queryClient.invalidateQueries({ queryKey: ['statement', id] })
    },
    onError: (e) => setError(getErrorMessage(e)),
  })

  const layoutPdfMutation = useMutation({
    mutationFn: async () => {
      if (!exportRootId) throw new Error('Layout export not supported for this bank')
      const html = captureStatementHtml(exportRootId)
      return statementService.previewLayoutPdf(id!, html)
    },
    onSuccess: (blob) => {
      if (layoutPdfUrl) URL.revokeObjectURL(layoutPdfUrl)
      setLayoutPdfUrl(URL.createObjectURL(blob))
      setPreviewTab('layout')
    },
    onError: (e) => setError(getErrorMessage(e)),
  })

  const downloadMutation = useMutation({
    mutationFn: () => statementService.downloadPdf(id!),
    onSuccess: ({ blob, filename }) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.click()
      URL.revokeObjectURL(url)
      queryClient.invalidateQueries({ queryKey: ['statement', id] })
    },
    onError: (e) => setError(getErrorMessage(e)),
  })

  const approveMutation = useMutation({
    mutationFn: () => statementService.approve(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['statement', id] }),
    onError: (e) => setError(getErrorMessage(e)),
  })

  useEffect(() => () => {
    if (pdfUrl) URL.revokeObjectURL(pdfUrl)
    if (layoutPdfUrl) URL.revokeObjectURL(layoutPdfUrl)
  }, [pdfUrl, layoutPdfUrl])

  if (isLoading || !statement) {
    return <p>Loading statement...</p>
  }

  const hasTransactions = (statement.transactions?.length ?? 0) > 0

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-bold">Statement {statement.id.slice(-6)}</h2>
          <p className="text-[var(--color-muted-foreground)]">{statement.customerDetails.customerName} · {statement.bankCode}</p>
        </div>
        <StatusBadge status={statement.status} />
      </div>

      <Card>
        <CardHeader><CardTitle>Actions</CardTitle></CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          {hasRole('ADMIN', 'STAFF') && (
            <>
              <Button onClick={() => generateMutation.mutate()} disabled={generateMutation.isPending}>
                {generateMutation.isPending ? 'Generating...' : 'Generate Transactions'}
              </Button>
              {supportsLayout && (
                <Button
                  variant="secondary"
                  onClick={() => layoutPdfMutation.mutate()}
                  disabled={layoutPdfMutation.isPending || !hasTransactions}
                >
                  {layoutPdfMutation.isPending ? 'Rendering...' : 'Preview Layout PDF'}
                </Button>
              )}
              <Button variant="secondary" onClick={() => previewMutation.mutate()} disabled={previewMutation.isPending || !hasTransactions}>
                Preview Legacy PDF
              </Button>
              <Button variant="outline" onClick={() => downloadMutation.mutate()} disabled={downloadMutation.isPending || !hasTransactions}>
                {statement.bankCode === 'SBI'
                  ? 'Download SBI PDF'
                  : statement.bankCode === 'KOTAK'
                    ? 'Download Kotak PDF'
                    : statement.bankCode === 'BOI'
                      ? 'Download BOI PDF'
                      : 'Download PDF'}
              </Button>
            </>
          )}
          {hasRole('ADMIN', 'STAFF') && statement.status !== 'APPROVED' && (
            <Button variant="secondary" onClick={() => approveMutation.mutate()} disabled={approveMutation.isPending}>
              Approve for Viewers
            </Button>
          )}
          <Button variant="ghost" onClick={() => navigate('/statements/history')}>Back to History</Button>
        </CardContent>
      </Card>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {statement.bankCode === 'BOI' && statement.customerDetails.dateOfBirth && (
        <Card>
          <CardHeader><CardTitle>BOI PDF Password</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>Password format: first 4 letters of your first name + DDMM of date of birth (no spaces).</p>
            <p className="text-[var(--color-muted-foreground)]">Example: Pavan Singh, DOB 24/01/1998 → PAVA2401</p>
            {computeBoiPdfPassword(statement.customerDetails.customerName, statement.customerDetails.dateOfBirth) && (
              <p className="rounded-md border bg-[var(--color-muted)]/30 p-3 font-mono text-base">
                {computeBoiPdfPassword(statement.customerDetails.customerName, statement.customerDetails.dateOfBirth)}
              </p>
            )}
            <p className="text-[var(--color-muted-foreground)]">
              Preview in the browser opens without a password. After you download and save the file, use this password to open it locally.
            </p>
          </CardContent>
        </Card>
      )}

      {hasRole('ADMIN', 'STAFF') && statement.bankCode !== 'SBI' && statement.bankCode !== 'KOTAK' && statement.bankCode !== 'BOI' && (
        <p className="rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
          All PDFs include a SAMPLE watermark. Data is synthetic and for demo/testing only.
        </p>
      )}

      {hasTransactions && (
        <Card>
          <CardHeader><CardTitle>Transactions ({statement.transactions.length})</CardTitle></CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left">
                  <th className="p-2">Date</th>
                  <th className="p-2">Narration</th>
                  <th className="p-2">Debit</th>
                  <th className="p-2">Credit</th>
                  <th className="p-2">Balance</th>
                </tr>
              </thead>
              <tbody>
                {statement.transactions.map((txn, i) => (
                  <tr key={i} className="border-b">
                    <td className="p-2">{txn.date}</td>
                    <td className="p-2">{txn.narration}</td>
                    <td className="p-2">{txn.debit > 0 ? txn.debit.toFixed(2) : '-'}</td>
                    <td className="p-2">{txn.credit > 0 ? txn.credit.toFixed(2) : '-'}</td>
                    <td className="p-2">{txn.balance?.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {hasTransactions && supportsLayout && layoutViewModel && (
        <Card>
          <CardHeader>
            <CardTitle>Statement Preview</CardTitle>
            <p className="text-sm text-[var(--color-muted-foreground)]">
              React layout preview (source of truth during migration). Download still uses legacy backend PDF.
            </p>
          </CardHeader>
          <CardContent>
            <Tabs value={previewTab} onValueChange={(v) => setPreviewTab(v as 'layout' | 'legacy')}>
              <TabsList>
                <TabsTrigger value="layout">Layout Preview</TabsTrigger>
                <TabsTrigger value="legacy">Legacy PDF</TabsTrigger>
              </TabsList>

              <TabsContent value="layout" className="space-y-4">
                <div className="overflow-x-auto rounded-md border bg-[var(--color-muted)]/20 p-4">
                  <BankStatementPreview data={layoutViewModel} watermark="SAMPLE" />
                </div>
                {layoutPdfUrl && (
                  <iframe
                    src={layoutPdfUrl}
                    title="Layout PDF Preview"
                    className="h-[700px] w-full rounded-md border"
                  />
                )}
              </TabsContent>

              <TabsContent value="legacy">
                {pdfUrl ? (
                  <iframe src={pdfUrl} title="Legacy PDF Preview" className="h-[700px] w-full rounded-md border" />
                ) : (
                  <p className="text-sm text-[var(--color-muted-foreground)]">
                    Click &quot;Preview Legacy PDF&quot; to view the backend iText PDF.
                  </p>
                )}
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      )}

      {hasTransactions && !supportsLayout && pdfUrl && (
        <Card>
          <CardHeader><CardTitle>PDF Preview</CardTitle></CardHeader>
          <CardContent>
            <iframe src={pdfUrl} title="PDF Preview" className="h-[700px] w-full rounded-md border" />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
