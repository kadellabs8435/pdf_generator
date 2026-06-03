import { useQuery } from '@tanstack/react-query'
import { adminService } from '@/services/adminService'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export function AdminLogsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['admin-logs'],
    queryFn: () => adminService.getLogs(0, 50),
  })

  return (
    <div className="space-y-6">
      <h2 className="text-3xl font-bold">Activity Logs</h2>
      <Card>
        <CardHeader><CardTitle>Recent Events</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          {isLoading ? <p>Loading...</p> : (data?.content ?? []).map((log) => (
            <div key={log.id} className="rounded-md border p-3 text-sm">
              <p className="font-medium">{log.action}</p>
              <p className="text-[var(--color-muted-foreground)]">{log.details}</p>
              <p className="mt-1 text-xs text-slate-400">{log.userName} · {new Date(log.createdAt).toLocaleString()}</p>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
