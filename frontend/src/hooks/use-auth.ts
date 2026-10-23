import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import authApi from '~/apis/auth.api'
import { isUnauthorizedError } from '~/lib/api-error'
import { useAuthStore } from '~/stores/use-auth-store'
import type { LoginBody, RegisterBody } from '~/types/auth.type'

export const profileQueryKey = ['auth', 'profile'] as const

const getProfile = async () => {
  const response = await authApi.getProfile()
  return response.data
}

export const logoutSession = async (queryClient: QueryClient) => {
  try {
    await authApi.logout()
  } catch (error) {
    if (!isUnauthorizedError(error)) {
      throw error
    }
  }

  useAuthStore.getState().logout()
  queryClient.clear()
}

export const useLoginMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: LoginBody) => authApi.login(body),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: profileQueryKey })
    }
  })
}

export const useRegisterMutation = () => {
  return useMutation({
    mutationFn: (body: RegisterBody) => authApi.register(body)
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
