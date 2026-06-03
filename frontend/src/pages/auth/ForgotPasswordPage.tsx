import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

const emailSchema = z.object({ email: z.string().email() })
const resetSchema = z.object({
  otp: z.string().regex(/^\d{6}$/),
  newPassword: z.string().min(6),
})

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const challengeToken = useAuthStore((s) => s.challengeToken)
  const setChallengeToken = useAuthStore((s) => s.setChallengeToken)
  const [step, setStep] = useState<'request' | 'reset'>(challengeToken ? 'reset' : 'request')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const emailForm = useForm<z.infer<typeof emailSchema>>({ resolver: zodResolver(emailSchema) })
  const resetForm = useForm<z.infer<typeof resetSchema>>({ resolver: zodResolver(resetSchema) })

  const requestReset = emailForm.handleSubmit(async (values) => {
    try {
      setError('')
      const res = await authService.forgotPassword(values.email)
      setChallengeToken(res.challengeToken)
      setMessage(res.message)
      setStep('reset')
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  const submitReset = resetForm.handleSubmit(async (values) => {
    if (!challengeToken) return
    try {
      setError('')
      await authService.resetPassword(challengeToken, values.otp, values.newPassword)
      setChallengeToken(null)
      navigate('/login')
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Reset password</CardTitle>
          <CardDescription>Request OTP via email, then set a new password.</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs value={step}>
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="request">Request OTP</TabsTrigger>
              <TabsTrigger value="reset">Reset</TabsTrigger>
            </TabsList>
            <TabsContent value="request">
              <form onSubmit={requestReset} className="space-y-4 pt-4">
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" {...emailForm.register('email')} />
                </div>
                <Button type="submit" className="w-full">Send OTP</Button>
              </form>
            </TabsContent>
            <TabsContent value="reset">
              <form onSubmit={submitReset} className="space-y-4 pt-4">
                <div className="space-y-2">
                  <Label htmlFor="otp">OTP</Label>
                  <Input id="otp" maxLength={6} {...resetForm.register('otp')} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="newPassword">New password</Label>
                  <Input id="newPassword" type="password" {...resetForm.register('newPassword')} />
                </div>
                <Button type="submit" className="w-full">Reset password</Button>
              </form>
            </TabsContent>
          </Tabs>
          {message && <p className="mt-4 text-sm text-green-600">{message}</p>}
          {error && <p className="mt-4 text-sm text-red-600">{error}</p>}
          <p className="mt-4 text-center text-sm">
            <Link to="/login" className="text-[var(--color-primary)] hover:underline">Back to login</Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
