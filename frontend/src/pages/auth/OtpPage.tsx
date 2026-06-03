import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/stores/authStore'
import { getErrorMessage } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

const schema = z.object({
  otp: z.string().regex(/^\d{6}$/, 'Enter 6-digit OTP'),
})

export function OtpPage() {
  const navigate = useNavigate()
  const challengeToken = useAuthStore((s) => s.challengeToken)
  const token = useAuthStore((s) => s.token)
  const setAuth = useAuthStore((s) => s.setAuth)
  const [error, setError] = useState('')

  const form = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) })

  useEffect(() => {
    // Only redirect when there is no active OTP session AND user is not already authenticated.
    // setAuth clears challengeToken on success — without the token check this effect sent users back to login.
    if (!challengeToken && !token) {
      navigate('/login')
    }
  }, [challengeToken, token, navigate])

  if (!challengeToken && !token) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
        <p className="text-sm text-[var(--color-muted-foreground)]">Redirecting to sign in...</p>
      </div>
    )
  }

  const onSubmit = form.handleSubmit(async (values) => {
    if (!challengeToken) return
    try {
      setError('')
      const res = await authService.verifyOtp(challengeToken, values.otp)
      setAuth(res.token, {
        userId: res.userId,
        name: res.name,
        email: res.email,
        mobile: res.mobile,
        role: res.role,
      })
      navigate('/dashboard')
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Verify OTP</CardTitle>
          <CardDescription>Enter the 6-digit OTP from backend console logs.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="otp">OTP</Label>
              <Input id="otp" placeholder="123456" maxLength={6} {...form.register('otp')} />
            </div>
            {error && <p className="text-sm text-red-600">{error}</p>}
            <Button type="submit" className="w-full">Verify &amp; Sign in</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
