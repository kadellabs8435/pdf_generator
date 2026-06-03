import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthUser, Role } from '@/types'

interface AuthState {
  token: string | null
  user: AuthUser | null
  challengeToken: string | null
  setChallengeToken: (token: string | null) => void
  setAuth: (token: string, user: AuthUser) => void
  logout: () => void
  hasRole: (...roles: Role[]) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      challengeToken: null,
      setChallengeToken: (challengeToken) => set({ challengeToken }),
      setAuth: (token, user) => set({ token, user, challengeToken: null }),
      logout: () => set({ token: null, user: null, challengeToken: null }),
      hasRole: (...roles) => {
        const role = get().user?.role
        return role ? roles.includes(role) : false
      },
    }),
    {
      name: 'bank-statement-auth',
      onRehydrateStorage: () => (_state, error) => {
        if (error) {
          localStorage.removeItem('bank-statement-auth')
        }
      },
    }
  )
)
