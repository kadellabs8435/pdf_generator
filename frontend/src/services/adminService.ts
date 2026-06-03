import { api } from '@/lib/api'
import type { ActivityItem, BankTemplate, PageResponse, Role, UserRecord } from '@/types'

export const adminService = {
  listUsers: () => api.get<UserRecord[]>('/admin/users').then((r) => r.data),
  createUser: (data: { name: string; email?: string; mobile?: string; password: string; role: Role; active: boolean }) =>
    api.post<UserRecord>('/admin/users', data).then((r) => r.data),
  updateUser: (id: string, data: { name: string; email?: string; mobile?: string; password?: string; role: Role; active: boolean }) =>
    api.put<UserRecord>(`/admin/users/${id}`, data).then((r) => r.data),
  deleteUser: (id: string) => api.delete(`/admin/users/${id}`),

  listTemplates: () => api.get<BankTemplate[]>('/admin/templates').then((r) => r.data),
  createTemplate: (data: { code: string; displayName: string; active: boolean }) =>
    api.post<BankTemplate>('/admin/templates', data).then((r) => r.data),
  updateTemplate: (id: string, data: { code: string; displayName: string; active: boolean }) =>
    api.put<BankTemplate>(`/admin/templates/${id}`, data).then((r) => r.data),
  deleteTemplate: (id: string) => api.delete(`/admin/templates/${id}`),

  getLogs: (page = 0, size = 20) =>
    api.get<PageResponse<ActivityItem>>('/admin/logs', { params: { page, size } }).then((r) => r.data),
}
