import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { useAuthStore } from '~/stores/use-auth-store'
import { mockCurrentUser } from './mocks/mock-data'

export const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0
      },
      mutations: {
        retry: false
      }
    }
  })

interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  queryClient?: QueryClient
  initialEntries?: string[]
}

export function renderWithProviders(
  ui: ReactElement,
  { queryClient = createTestQueryClient(), initialEntries = ['/'], ...renderOptions }: ExtendedRenderOptions = {}
) {
  // Set default auth user if not already set
  if (!useAuthStore.getState().user) {
    useAuthStore.setState({
      user: mockCurrentUser,
      isAuthenticated: true
    })
  }

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
      </QueryClientProvider>
    )
  }

  return {
    queryClient,
    ...render(ui, { wrapper: Wrapper, ...renderOptions })
  }
}
