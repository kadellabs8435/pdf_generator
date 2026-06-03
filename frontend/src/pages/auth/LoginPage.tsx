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

const emailSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
})

const mobileSchema = z.object({
  mobile: z.string().regex(/^\d{10}$/, 'Enter 10-digit mobile number'),
  password: z.string().min(6),
})

export function LoginPage() {
  const navigate = useNavigate()
  const setChallengeToken = useAuthStore((s) => s.setChallengeToken)
  const [error, setError] = useState('')

  const emailForm = useForm<z.infer<typeof emailSchema>>({ resolver: zodResolver(emailSchema) })
  const mobileForm = useForm<z.infer<typeof mobileSchema>>({ resolver: zodResolver(mobileSchema) })

  const handleEmailLogin = emailForm.handleSubmit(async (values) => {
    try {
      setError('')
      const res = await authService.loginEmail(values.email, values.password)
      setChallengeToken(res.challengeToken)
      navigate('/verify-otp')
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  const handleMobileLogin = mobileForm.handleSubmit(async (values) => {
    try {
      setError('')
      const res = await authService.loginMobile(values.mobile, values.password)
      setChallengeToken(res.challengeToken)
      navigate('/verify-otp')
    } catch (e) {
      setError(getErrorMessage(e))
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Sign in</CardTitle>
          <CardDescription>Use email or mobile with password, then verify OTP.</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue="email">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="email">Email</TabsTrigger>
              <TabsTrigger value="mobile">Mobile</TabsTrigger>
            </TabsList>
            <TabsContent value="email">
              <form onSubmit={handleEmailLogin} className="space-y-4 pt-4">
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input id="email" type="email" placeholder="admin@bankdemo.com" {...emailForm.register('email')} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <Input id="password" type="password" {...emailForm.register('password')} />
                </div>
                <Button type="submit" className="w-full">Continue</Button>
              </form>
            </TabsContent>
            <TabsContent value="mobile">
              <form onSubmit={handleMobileLogin} className="space-y-4 pt-4">
                <div className="space-y-2">
                  <Label htmlFor="mobile">Mobile</Label>
                  <Input id="mobile" placeholder="9876543210" {...mobileForm.register('mobile')} />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="mobile-password">Password</Label>
                  <Input id="mobile-password" type="password" {...mobileForm.register('password')} />
                </div>
                <Button type="submit" className="w-full">Continue</Button>
              </form>
            </TabsContent>
          </Tabs>
          {error && <p className="mt-4 text-sm text-red-600">{error}</p>}
          <p className="mt-4 text-center text-sm">
            <Link to="/forgot-password" className="text-[var(--color-primary)] hover:underline">Forgot password?</Link>
          </p>
          <div className="mt-6 rounded-md bg-slate-50 p-3 text-xs text-slate-600">
            <p><strong>Demo:</strong> admin@bankdemo.com / Admin@123</p>
            <p>staff@bankdemo.com / Staff@123 | viewer@bankdemo.com / Viewer@123</p>
            <p className="mt-1">OTP is printed in backend console (dev mode).</p>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
