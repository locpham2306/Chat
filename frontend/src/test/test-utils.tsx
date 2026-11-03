import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren, ReactElement } from 'react'
import { render, type RenderOptions } from '@testing-library/react'

export const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  })

export const createQueryWrapper = (queryClient = createTestQueryClient()) => {
  const QueryWrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )

  return { QueryWrapper, queryClient }
}

export const renderWithQueryClient = (
  ui: ReactElement,
  queryClient = createTestQueryClient(),
  options?: Omit<RenderOptions, 'wrapper'>
) => {
  const Wrapper = ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )

  return { ...render(ui, { wrapper: Wrapper, ...options }), queryClient }
}
