import { Link, useNavigate } from 'react-router-dom'
import { Button } from '~/components/ui/button'
import { Input } from '~/components/ui/input'
import { useAuthStore } from '~/stores/use-auth-store'

export const LoginPage = () => {
  const login = useAuthStore((state) => state.login)
  const navigate = useNavigate()

  const handleMockLogin = (e: React.FormEvent) => {
    e.preventDefault()
    login()
    navigate('/')
  }

  return (
    <form onSubmit={handleMockLogin} className='space-y-4'>
      <div className='space-y-1 text-center'>
        <h2 className='text-lg font-semibold'>Đăng nhập</h2>
        <p className='text-xs text-muted-foreground'>Nhập tài khoản để tiếp tục</p>
      </div>

      <div className='space-y-3'>
        <div className='space-y-1'>
          <label htmlFor='username' className='text-xs font-medium text-muted-foreground'>
            Tài khoản
          </label>
          <Input id='username' placeholder='Nhập username...' defaultValue='tai' />
        </div>
        <div className='space-y-1'>
          <label htmlFor='password' className='text-xs font-medium text-muted-foreground'>
            Mật khẩu
          </label>
          <Input id='password' type='password' placeholder='Nhập password...' defaultValue='123456' />
        </div>
      </div>

      <Button type='submit' className='w-full'>
        Đăng nhập
      </Button>

      <p className='text-center text-xs text-muted-foreground'>
        Chưa có tài khoản?{' '}
        <Link to='/register' className='text-primary hover:underline font-medium'>
          Đăng ký ngay
        </Link>
      </p>
    </form>
  )
}
