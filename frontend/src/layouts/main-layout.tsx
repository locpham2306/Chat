import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { LoaderCircle, LogOut, MessageSquare, Moon, Sun } from 'lucide-react'
import { Avatar, AvatarFallback } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import { useLogoutMutation } from '~/hooks/use-auth'
import { useAuthStore } from '~/stores/use-auth-store'

export const MainLayout = () => {
  const [isDark, setIsDark] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const user = useAuthStore((state) => state.user)
  const logoutMutation = useLogoutMutation()
  const navigate = useNavigate()

  const toggleTheme = () => {
    setIsDark(!isDark)
    document.documentElement.classList.toggle('dark')
  }

  const handleLogout = () => {
    setLogoutError(null)
    logoutMutation.mutate(undefined, {
      onSuccess: () => navigate('/login', { replace: true }),
      onError: () => setLogoutError('Không thể đăng xuất. Vui lòng thử lại.')
    })
  }

  return (
    <div className='flex h-screen w-screen overflow-hidden bg-background text-foreground flex-col'>
      {/* Top Header */}
      <header className='flex h-14 items-center justify-between border-b px-4 shrink-0 bg-card'>
        <div className='flex items-center gap-3'>
          <div className='flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground'>
            <MessageSquare className='h-5 w-5' />
          </div>
          <div>
            <h1 className='text-sm font-semibold leading-none'>Chat App</h1>
            <p className='text-xs text-muted-foreground mt-0.5'>BTL LTM</p>
          </div>
        </div>

        <div className='flex items-center gap-3'>
          {logoutError && (
            <p
              role='alert'
              className='fixed right-4 top-16 z-50 max-w-[calc(100vw-2rem)] rounded-md border border-destructive/30 bg-background px-3 py-2 text-xs text-destructive shadow-sm'
            >
              {logoutError}
            </p>
          )}

          {user && (
            <div className='flex items-center gap-2'>
              <Avatar className='h-8 w-8'>
                <AvatarFallback>{(user.fullName || user.username || 'U').slice(0, 1).toUpperCase()}</AvatarFallback>
              </Avatar>
              <div className='hidden sm:block text-left'>
                <p className='text-xs font-medium leading-none'>{user.fullName || user.username || 'User'}</p>
                <div className='mt-0.5'>
                  <Badge variant={user.status === 'ONLINE' ? 'online' : 'offline'}>
                    {user.status === 'ONLINE' ? 'Online' : 'Offline'}
                  </Badge>
                </div>
              </div>
            </div>
          )}

          <Button variant='ghost' size='icon' onClick={toggleTheme} aria-label='Đổi giao diện sáng tối'>
            {isDark ? <Sun className='h-4 w-4' /> : <Moon className='h-4 w-4' />}
          </Button>

          <Button
            variant='ghost'
            size='icon'
            onClick={handleLogout}
            disabled={logoutMutation.isPending}
            title='Đăng xuất'
            aria-label='Đăng xuất'
          >
            {logoutMutation.isPending ? (
              <LoaderCircle className='h-4 w-4 animate-spin' aria-hidden='true' />
            ) : (
              <LogOut className='h-4 w-4' />
            )}
          </Button>
        </div>
      </header>

      {/* Main Workspace Area */}
      <div className='flex flex-1 overflow-hidden'>
        <Outlet />
      </div>
    </div>
  )
}
