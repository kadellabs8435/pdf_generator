import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { LoginPage } from '@/pages/auth/LoginPage'
import { OtpPage } from '@/pages/auth/OtpPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { StatementFormPage } from '@/pages/statements/StatementFormPage'
import { StatementDetailPage } from '@/pages/statements/StatementDetailPage'
import { HistoryPage } from '@/pages/statements/HistoryPage'
import { BulkUploadPage } from '@/pages/statements/BulkUploadPage'
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage'
import { AdminTemplatesPage } from '@/pages/admin/AdminTemplatesPage'
import { AdminLogsPage } from '@/pages/admin/AdminLogsPage'

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/verify-otp" element={<OtpPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/statements/history" element={<HistoryPage />} />
          <Route path="/statements/:id" element={<StatementDetailPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN', 'STAFF']} />}>
          <Route path="/statements/new" element={<StatementFormPage />} />
          <Route path="/statements/bulk" element={<BulkUploadPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/templates" element={<AdminTemplatesPage />} />
          <Route path="/admin/logs" element={<AdminLogsPage />} />
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
