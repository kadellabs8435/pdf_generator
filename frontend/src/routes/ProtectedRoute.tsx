import { Navigate, Outlet } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuthStore } from '@/stores/authStore'
import type { Role } from '@/types'
import { AppShell } from '@/components/layout/AppShell'

interface ProtectedRouteProps {
  roles?: Role[]
}

export function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const token = useAuthStore((s) => s.token)
  const hasRole = useAuthStore((s) => s.hasRole)
  const [hydrated, setHydrated] = useState(useAuthStore.persist.hasHydrated())

  useEffect(() => {
    if (useAuthStore.persist.hasHydrated()) {
      setHydrated(true)
      return
    }

    const unsub = useAuthStore.persist.onFinishHydration(() => setHydrated(true))
    const timeout = window.setTimeout(() => setHydrated(true), 500)

    return () => {
      unsub()
      window.clearTimeout(timeout)
    }
  }, [])

  if (!hydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center text-sm text-[var(--color-muted-foreground)]">
        Loading...
      </div>
    )
  }

  if (!token) {
    return <Navigate to="/login" replace />
  }

  if (roles && !hasRole(...roles)) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <AppShell>
      <Outlet />
    </AppShell>
  )
}
