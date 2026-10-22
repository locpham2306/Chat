import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { MessageSquare, Moon, Sun, LogOut } from 'lucide-react'
import { Avatar, AvatarFallback } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import { useAuthStore } from '~/stores/use-auth-store'

export const MainLayout = () => {
  const [isDark, setIsDark] = useState(false)
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const navigate = useNavigate()

  const toggleTheme = () => {
    setIsDark(!isDark)
    document.documentElement.classList.toggle('dark')
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
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
            title='Đăng xuất (Test mock)'
            aria-label='Đăng xuất'
          >
            <LogOut className='h-4 w-4' />
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
