import { Outlet } from 'react-router-dom'

export const AuthLayout = () => {
  return (
    <div className='min-h-screen bg-background text-foreground flex flex-col items-center justify-center p-4'>
      <div className='w-full max-w-sm space-y-6'>
        <div className='flex flex-col items-center space-y-2.5 text-center'>
          <div className='flex h-14 w-14 items-center justify-center rounded-xl bg-card border border-border/40 p-1 shadow-sm overflow-hidden'>
            <img src='/logo.png' alt='ChitChat' className='h-full w-full object-contain' />
          </div>
          <h1 className='text-2xl font-bold tracking-tight'>ChitChat</h1>
        </div>
        <div className='rounded-xl border bg-card p-6 shadow-sm'>
          <Outlet />
        </div>
      </div>
    </div>
  )
}
