import { api } from '@/lib/api'
import type { ActivityItem, DashboardStats, PageResponse } from '@/types'

export const dashboardService = {
  getStats: () => api.get<DashboardStats>('/dashboard/stats').then((r) => r.data),
  getRecentActivity: (page = 0, size = 10) =>
    api.get<PageResponse<ActivityItem>>('/dashboard/recent-activity', { params: { page, size } }).then((r) => r.data),
}
