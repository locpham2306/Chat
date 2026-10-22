import { create } from 'zustand'
import type { User } from '~/types/user.type'

const MOCK_USER: User = {
  id: 'u1',
  username: 'tai',
  fullName: 'Tài',
  avatarFileId: null,
  status: 'ONLINE',
  lastSeen: new Date().toISOString()
}

interface AuthState {
  isAuthenticated: boolean
  user: User | null
  login: () => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  isAuthenticated: true,
  user: MOCK_USER,
  login: () => set({ isAuthenticated: true, user: MOCK_USER }),
  logout: () => set({ isAuthenticated: false, user: null })
}))
