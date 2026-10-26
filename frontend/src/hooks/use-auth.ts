import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import authApi from '~/apis/auth.api'
import { isUnauthorizedError } from '~/lib/api-error'
import { wsClient } from '~/lib/websocket-client'
import { useAuthStore } from '~/stores/use-auth-store'
import { useWebSocketStore } from '~/stores/use-websocket-store'
import type { LoginBody, LoginResponse, RegisterBody } from '~/types/auth.type'
import type { User } from '~/types/user.type'

export const profileQueryKey = ['auth', 'profile'] as const

export const getProfile = async (): Promise<User> => {
  const response = await authApi.getProfile()
  return response.data.content
}

const login = async (body: LoginBody): Promise<LoginResponse> => {
  const response = await authApi.login(body)
  return response.data.content
}

const register = async (body: RegisterBody): Promise<User> => {
  const response = await authApi.register(body)
  return response.data.content
}

export const logoutSession = async (queryClient: QueryClient) => {
  try {
    await authApi.logout()
  } catch (error) {
    if (!isUnauthorizedError(error)) {
      throw error
    }
  }

  wsClient.disconnect()
  useWebSocketStore.getState().reset()
  useAuthStore.getState().logout()
  queryClient.clear()
}

export const useLoginMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: login,
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: profileQueryKey })
    }
  })
}

export const useRegisterMutation = () => {
  return useMutation({
    mutationFn: register
  })
}

export const useLogoutMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => logoutSession(queryClient)
  })
}

export const useProfileQuery = () => {
  return useQuery({
    queryKey: profileQueryKey,
    queryFn: getProfile,
    staleTime: 30_000,
    retry: false
  })
}
