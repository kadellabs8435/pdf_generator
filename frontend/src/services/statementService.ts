import { api } from '@/lib/api'
import type { BankTemplate, BulkJob, PageResponse, Statement, StatementDraftRequest, StatementStatus } from '@/types'

export const statementService = {
  createDraft: (data: StatementDraftRequest) =>
    api.post<Statement>('/statements/draft', data).then((r) => r.data),

  generateTransactions: (id: string) =>
    api.post<Statement>(`/statements/${id}/generate-transactions`).then((r) => r.data),

  previewPdf: async (id: string) => {
    const response = await api.post(`/statements/${id}/preview`, {}, { responseType: 'blob' })
    return response.data as Blob
  },

  /** Frontend HTML layout → backend OpenHTMLtoPDF (migration path; legacy preview unchanged). */
  previewLayoutPdf: async (id: string, html: string) => {
    const response = await api.post(`/statements/${id}/preview-layout`, { html }, { responseType: 'blob' })
    return response.data as Blob
  },

  downloadPdf: async (id: string) => {
    const response = await api.post(`/statements/${id}/download`, {}, { responseType: 'blob' })
    const disposition = response.headers['content-disposition'] as string | undefined
    let filename = 'statement.pdf'
    const match = disposition?.match(/filename=\"?([^\";]+)\"?/)
    if (match?.[1]) filename = match[1]
    return { blob: response.data as Blob, filename }
  },

  approve: (id: string) => api.post<Statement>(`/statements/${id}/approve`).then((r) => r.data),

  getById: (id: string) => api.get<Statement>(`/statements/${id}`).then((r) => r.data),

  getHistory: (params: { page?: number; size?: number; bankCode?: string; status?: StatementStatus }) =>
    api.get<PageResponse<Statement>>('/statements/history', { params }).then((r) => r.data),

  downloadBulkTemplate: async () => {
    const response = await api.get('/statements/bulk-template', { responseType: 'blob' })
    return response.data as Blob
  },

  bulkUpload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<BulkJob>('/statements/bulk-upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },

  getBulkJob: (jobId: string) => api.get<BulkJob>(`/statements/bulk-jobs/${jobId}`).then((r) => r.data),

  suggestNarrations: (context: string, transactionType: string) =>
    api.post<{ suggestions: string[]; message: string }>('/statements/suggest-narrations', { context, transactionType }).then((r) => r.data),
}

export const templateService = {
  list: () => api.get<BankTemplate[]>('/templates').then((r) => r.data),
}
