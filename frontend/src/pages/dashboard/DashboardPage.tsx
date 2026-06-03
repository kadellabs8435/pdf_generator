import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { FileText, Upload, Users } from 'lucide-react'
import { dashboardService } from '@/services/dashboardService'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/authStore'

export function DashboardPage() {
  const hasRole = useAuthStore((s) => s.hasRole)

  const { data: stats } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: dashboardService.getStats,
  })

  const { data: activity } = useQuery({
    queryKey: ['dashboard-activity'],
    queryFn: () => dashboardService.getRecentActivity(0, 8),
  })

  const statCards = [
    { label: 'Total Users', value: stats?.totalUsers ?? 0 },
    { label: 'Generated PDFs', value: stats?.totalGeneratedPdfs ?? 0 },
    { label: 'Drafts', value: stats?.totalDrafts ?? 0 },
    { label: 'Approved', value: stats?.totalApproved ?? 0 },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-[var(--color-muted-foreground)]">Overview of platform activity and quick actions.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card) => (
          <Card key={card.label}>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium text-[var(--color-muted-foreground)]">{card.label}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold">{card.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-3">
            {hasRole('ADMIN', 'STAFF') && (
              <>
                <Button asChild>
                  <Link to="/statements/new"><FileText className="h-4 w-4" /> New Statement</Link>
                </Button>
                <Button asChild variant="secondary">
                  <Link to="/statements/bulk"><Upload className="h-4 w-4" /> Bulk Upload</Link>
                </Button>
              </>
            )}
            <Button asChild variant="outline">
              <Link to="/statements/history">View History</Link>
            </Button>
            {hasRole('ADMIN') && (
              <Button asChild variant="outline">
                <Link to="/admin/users"><Users className="h-4 w-4" /> Manage Users</Link>
              </Button>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {(activity?.content ?? []).map((item) => (
                <div key={item.id} className="rounded-md border p-3 text-sm">
                  <p className="font-medium">{item.action}</p>
                  <p className="text-[var(--color-muted-foreground)]">{item.details}</p>
                  <p className="mt-1 text-xs text-slate-400">{item.userName} · {new Date(item.createdAt).toLocaleString()}</p>
                </div>
              ))}
              {(activity?.content ?? []).length === 0 && (
                <p className="text-sm text-[var(--color-muted-foreground)]">No activity yet.</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
