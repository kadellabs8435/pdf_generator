import { api } from '@/lib/api'
import type { AuthUser } from '@/types'

export interface OtpChallenge {
  challengeToken: string
  message: string
}

export const authService = {
  loginEmail: (email: string, password: string) =>
    api.post<OtpChallenge>('/auth/login/email', { email, password }).then((r) => r.data),

  loginMobile: (mobile: string, password: string) =>
    api.post<OtpChallenge>('/auth/login/mobile', { mobile, password }).then((r) => r.data),

  verifyOtp: (challengeToken: string, otp: string) =>
    api.post<{ token: string; userId: string; name: string; email?: string; mobile?: string; role: AuthUser['role'] }>(
      '/auth/verify-otp',
      { challengeToken, otp }
    ).then((r) => r.data),

  forgotPassword: (email: string) =>
    api.post<OtpChallenge>('/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (challengeToken: string, otp: string, newPassword: string) =>
    api.post('/auth/reset-password', { challengeToken, otp, newPassword }),
}
