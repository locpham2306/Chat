import { useEffect } from 'react'
import { AlertCircle, LoaderCircle, RefreshCw } from 'lucide-react'
import { Navigate, Outlet } from 'react-router-dom'
import { Button } from '~/components/ui/button'
import { useProfileQuery } from '~/hooks/use-auth'
import { isUnauthorizedError } from '~/lib/api-error'
import { useAuthStore } from '~/stores/use-auth-store'

export const ProtectedRoute = () => {
  const login = useAuthStore((state) => state.login)
  const logout = useAuthStore((state) => state.logout)
  const profileQuery = useProfileQuery()
  const isUnauthorized = isUnauthorizedError(profileQuery.error)

  useEffect(() => {
    if (profileQuery.data) {
      login(profileQuery.data)
    } else if (isUnauthorized) {
      logout()
    }
  }, [isUnauthorized, login, logout, profileQuery.data])

  if (profileQuery.isPending) {
    return (
      <div role='status' className='flex min-h-screen items-center justify-center gap-2 text-sm text-muted-foreground'>
        <LoaderCircle className='h-4 w-4 animate-spin' aria-hidden='true' />
        Đang kiểm tra phiên đăng nhập...
      </div>
    )
  }

  if (isUnauthorized) {
    return <Navigate to='/login' replace />
  }

  if (profileQuery.isError) {
    return (
      <div className='flex min-h-screen flex-col items-center justify-center gap-3 p-6 text-center'>
        <AlertCircle className='h-8 w-8 text-destructive' aria-hidden='true' />
        <div>
          <p className='text-sm font-medium'>Không thể kiểm tra phiên đăng nhập</p>
          <p className='mt-1 text-xs text-muted-foreground'>Vui lòng kiểm tra kết nối và thử lại.</p>
        </div>
        <Button variant='outline' size='sm' onClick={() => profileQuery.refetch()}>
          <RefreshCw className='h-4 w-4' aria-hidden='true' />
          Thử lại
        </Button>
      </div>
    )
  }

  return <Outlet />
}
