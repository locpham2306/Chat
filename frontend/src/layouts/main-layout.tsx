import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { LoaderCircle, LogOut, Moon, Sun } from 'lucide-react'
import { ProfileSettingsDialog } from '~/components/profile-settings'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import { useLogoutMutation } from '~/hooks/use-auth'
import { useAuthStore } from '~/stores/use-auth-store'

export const MainLayout = () => {
  const [isDark, setIsDark] = useState(false)
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [isProfileSettingsOpen, setIsProfileSettingsOpen] = useState(false)
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
          <img src='/logo.png' alt='ChitChat' className='h-9 w-9 rounded-xl object-cover shadow-xs' />
          <div>
            <h1 className='text-sm font-semibold leading-none'>ChitChat</h1>
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
            <button
              type='button'
              onClick={() => setIsProfileSettingsOpen(true)}
              className='flex items-center gap-2 rounded-md px-1 py-0.5 text-left transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring'
              title='Chỉnh sửa hồ sơ'
              aria-label='Chỉnh sửa hồ sơ'
            >
              <Avatar className='h-8 w-8'>
                {user.avatarFileId && <AvatarImage src={`/api/files/${user.avatarFileId}`} alt={user.fullName} />}
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
            </button>
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

      {user && (
        <ProfileSettingsDialog open={isProfileSettingsOpen} onOpenChange={setIsProfileSettingsOpen} user={user} />
      )}

      {/* Main Workspace Area */}
      <div className='flex flex-1 overflow-hidden'>
        <Outlet />
      </div>
    </div>
  )
}
