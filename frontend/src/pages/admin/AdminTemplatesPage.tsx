import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { adminService } from '@/services/adminService'
import { getErrorMessage } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'

export function AdminTemplatesPage() {
  const queryClient = useQueryClient()
  const [error, setError] = useState('')
  const [form, setForm] = useState({ code: '', displayName: '', active: true })

  const { data: templates = [] } = useQuery({ queryKey: ['admin-templates'], queryFn: adminService.listTemplates })

  const createMutation = useMutation({
    mutationFn: () => adminService.createTemplate(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-templates'] })
      setForm({ code: '', displayName: '', active: true })
    },
    onError: (e) => setError(getErrorMessage(e)),
  })

  const toggleMutation = useMutation({
    mutationFn: (t: { id: string; code: string; displayName: string; active: boolean }) =>
      adminService.updateTemplate(t.id, { code: t.code, displayName: t.displayName, active: !t.active }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-templates'] }),
    onError: (e) => setError(getErrorMessage(e)),
  })

  return (
    <div className="space-y-6">
      <h2 className="text-3xl font-bold">Bank Templates</h2>

      <Card>
        <CardHeader><CardTitle>Add Template</CardTitle></CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-3">
          <div><Label>Code</Label><Input className="mt-2" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} /></div>
          <div><Label>Display Name</Label><Input className="mt-2" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></div>
          <div className="flex items-end gap-2">
            <label className="flex items-center gap-2 text-sm"><Checkbox checked={form.active} onCheckedChange={(v) => setForm({ ...form, active: Boolean(v) })} /> Active</label>
            <Button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}>Add</Button>
          </div>
        </CardContent>
      </Card>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <Card>
        <CardHeader><CardTitle>Templates</CardTitle></CardHeader>
        <CardContent>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-left"><th className="p-2">Code</th><th className="p-2">Name</th><th className="p-2">Active</th><th className="p-2"></th></tr>
            </thead>
            <tbody>
              {templates.map((t) => (
                <tr key={t.id} className="border-b">
                  <td className="p-2">{t.code}</td>
                  <td className="p-2">{t.displayName}</td>
                  <td className="p-2">{t.active ? 'Yes' : 'No'}</td>
                  <td className="p-2">
                    <Button size="sm" variant="outline" onClick={() => toggleMutation.mutate(t)}>
                      {t.active ? 'Deactivate' : 'Activate'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}
