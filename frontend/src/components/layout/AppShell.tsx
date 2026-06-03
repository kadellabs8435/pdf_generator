import { NavLink, useNavigate } from 'react-router-dom'
import { FileText, History, LayoutDashboard, LogOut, Settings, Upload, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/stores/authStore'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/statements/new', label: 'New Statement', icon: FileText, roles: ['ADMIN', 'STAFF'] as const },
  { to: '/statements/history', label: 'History', icon: History },
  { to: '/statements/bulk', label: 'Bulk Upload', icon: Upload, roles: ['ADMIN', 'STAFF'] as const },
  { to: '/admin/users', label: 'Users', icon: Users, roles: ['ADMIN'] as const },
  { to: '/admin/templates', label: 'Templates', icon: Settings, roles: ['ADMIN'] as const },
  { to: '/admin/logs', label: 'Activity Logs', icon: History, roles: ['ADMIN'] as const },
]

export function AppShell({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const hasRole = useAuthStore((s) => s.hasRole)
  const navigate = useNavigate()

  const visibleNav = navItems.filter((item) => !item.roles || item.roles.some((r) => hasRole(r)))

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="flex w-64 flex-col border-r bg-white">
        <div className="border-b p-6">
          <h1 className="text-lg font-bold text-[var(--color-primary)]">Bank Statement Generator</h1>
          <p className="text-xs text-[var(--color-muted-foreground)]">Demo and testing platform</p>
        </div>
        <nav className="flex-1 space-y-1 p-4">
          {visibleNav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-[var(--color-primary)] text-white' : 'text-slate-600 hover:bg-slate-100'
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t p-4">
          <p className="mb-2 text-sm font-medium">{user?.name}</p>
          <p className="mb-3 text-xs text-[var(--color-muted-foreground)]">{user?.role}</p>
          <Button
            variant="outline"
            size="sm"
            className="w-full"
            onClick={() => {
              logout()
              navigate('/login')
            }}
          >
            <LogOut className="h-4 w-4" />
            Logout
          </Button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-8">{children}</main>
    </div>
  )
}
