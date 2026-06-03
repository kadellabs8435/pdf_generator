import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { statementService } from '@/services/statementService'
import { getErrorMessage } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { BulkJob } from '@/types'

export function BulkUploadPage() {
  const [file, setFile] = useState<File | null>(null)
  const [job, setJob] = useState<BulkJob | null>(null)
  const [error, setError] = useState('')

  const uploadMutation = useMutation({
    mutationFn: (f: File) => statementService.bulkUpload(f),
    onSuccess: (data) => setJob(data),
    onError: (e) => setError(getErrorMessage(e)),
  })

  const downloadTemplate = async () => {
    const blob = await statementService.downloadBulkTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'bulk-statement-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-3xl font-bold">Bulk Upload</h2>
        <p className="text-[var(--color-muted-foreground)]">Upload Excel file to generate multiple statements.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Upload Excel</CardTitle>
          <CardDescription>Download the template, fill rows, then upload .xlsx file.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button variant="outline" onClick={downloadTemplate}>Download Template</Button>
          <input type="file" accept=".xlsx,.xls" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          <Button
            disabled={!file || uploadMutation.isPending}
            onClick={() => file && uploadMutation.mutate(file)}
          >
            {uploadMutation.isPending ? 'Processing...' : 'Upload & Generate'}
          </Button>
          {error && <p className="text-sm text-red-600">{error}</p>}
        </CardContent>
      </Card>

      {job && (
        <Card>
          <CardHeader><CardTitle>Batch Result</CardTitle></CardHeader>
          <CardContent className="space-y-2 text-sm">
            <p>Status: <strong>{job.status}</strong></p>
            <p>Processed: {job.processedRows} / {job.totalRows}</p>
            <p>Success: {job.successCount} · Failures: {job.failureCount}</p>
            {job.errorReport && (
              <pre className="mt-4 max-h-60 overflow-auto rounded-md bg-slate-100 p-3 text-xs whitespace-pre-wrap">{job.errorReport}</pre>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
