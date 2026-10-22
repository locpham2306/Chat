import { useEffect } from 'react'
import { LoaderCircle } from 'lucide-react'
import { Navigate, Outlet } from 'react-router-dom'
import { useProfileQuery } from '~/hooks/use-auth'
import { useAuthStore } from '~/stores/use-auth-store'

export const ProtectedRoute = () => {
  const login = useAuthStore((state) => state.login)
  const logout = useAuthStore((state) => state.logout)
  const profileQuery = useProfileQuery()

  useEffect(() => {
    if (profileQuery.data) {
      login(profileQuery.data)
    } else if (profileQuery.isError) {
      logout()
    }
  }, [login, logout, profileQuery.data, profileQuery.isError])

  if (profileQuery.isPending) {
    return (
      <div role='status' className='flex min-h-screen items-center justify-center gap-2 text-sm text-muted-foreground'>
        <LoaderCircle className='h-4 w-4 animate-spin' aria-hidden='true' />
        Đang kiểm tra phiên đăng nhập...
      </div>
    )
  }

  if (profileQuery.isError) {
    return <Navigate to='/login' replace />
  }

  return <Outlet />
}
