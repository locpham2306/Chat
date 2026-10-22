import { Outlet } from 'react-router-dom'
import { MessageSquare } from 'lucide-react'

export const AuthLayout = () => {
  return (
    <div className='min-h-screen bg-background text-foreground flex flex-col items-center justify-center p-4'>
      <div className='w-full max-w-sm space-y-6'>
        <div className='flex flex-col items-center space-y-2 text-center'>
          <div className='flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm'>
            <MessageSquare className='h-6 w-6' />
          </div>
          <h1 className='text-xl font-bold tracking-tight'>Chat App</h1>
          <p className='text-xs text-muted-foreground'>BTL Lập Trình Mạng</p>
        </div>
        <div className='rounded-xl border bg-card p-6 shadow-sm'>
          <Outlet />
        </div>
      </div>
    </div>
  )
}
