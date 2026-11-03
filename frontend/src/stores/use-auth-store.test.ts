import { expect, test } from 'vitest'
import type { User } from '../types/user.type.ts'

const persistedValues = new Map<string, string>()
let storageWriteCount = 0

Object.defineProperty(globalThis, 'sessionStorage', {
  configurable: true,
  value: {
    getItem: (key: string) => persistedValues.get(key) ?? null,
    setItem: (key: string, value: string) => {
      storageWriteCount++
      persistedValues.set(key, value)
    },
    removeItem: (key: string) => persistedValues.delete(key),
    clear: () => persistedValues.clear(),
    key: (index: number) => [...persistedValues.keys()][index] ?? null,
    get length() {
      return persistedValues.size
    }
  }
})

test('auth store chỉ giữ user profile trong bộ nhớ và không ghi sessionStorage', async () => {
  persistedValues.clear()
  storageWriteCount = 0
  const { useAuthStore } = await import('./use-auth-store')
  const user: User = {
    id: 'u1',
    username: 'huy',
    fullName: 'Huy',
    avatarFileId: null,
    status: 'ONLINE',
    lastSeen: null
  }

  useAuthStore.getState().login(user)

  expect(useAuthStore.getState().isAuthenticated).toBe(true)
  expect(useAuthStore.getState().user).toEqual(user)
  expect(storageWriteCount).toBe(0)

  useAuthStore.getState().logout()
  expect(useAuthStore.getState().isAuthenticated).toBe(false)
  expect(useAuthStore.getState().user).toBeNull()
})
