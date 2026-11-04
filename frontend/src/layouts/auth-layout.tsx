import { Outlet } from 'react-router-dom'

export const AuthLayout = () => {
  return (
    <div className='min-h-screen bg-background text-foreground flex flex-col items-center justify-center p-4'>
      <div className='w-full max-w-sm space-y-6'>
        <div className='flex flex-col items-center space-y-2 text-center'>
          <img src='/logo.png' alt='ChitChat' className='h-12 w-12 rounded-2xl object-cover shadow-sm' />
          <h1 className='text-xl font-bold tracking-tight'>ChitChat</h1>
          <p className='text-xs text-muted-foreground'>BTL Lập Trình Mạng</p>
        </div>
        <div className='rounded-xl border bg-card p-6 shadow-sm'>
          <Outlet />
        </div>
      </div>
    </div>
  )
}
