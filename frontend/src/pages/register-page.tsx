import { Link, useNavigate } from 'react-router-dom'
import { Button } from '~/components/ui/button'
import { Input } from '~/components/ui/input'
import { useAuthStore } from '~/stores/use-auth-store'

export const RegisterPage = () => {
  const login = useAuthStore((state) => state.login)
  const navigate = useNavigate()

  const handleMockRegister = (e: React.FormEvent) => {
    e.preventDefault()
    login()
    navigate('/')
  }

  return (
    <form onSubmit={handleMockRegister} className='space-y-4'>
      <div className='space-y-1 text-center'>
        <h2 className='text-lg font-semibold'>Tạo tài khoản</h2>
        <p className='text-xs text-muted-foreground'>Điền thông tin để bắt đầu</p>
      </div>

      <div className='space-y-3'>
        <div className='space-y-1'>
          <label htmlFor='fullName' className='text-xs font-medium text-muted-foreground'>
            Họ và tên
          </label>
          <Input id='fullName' placeholder='Nhập họ và tên...' defaultValue='Nguyễn Văn Tài' />
        </div>
        <div className='space-y-1'>
          <label htmlFor='reg-username' className='text-xs font-medium text-muted-foreground'>
            Tài khoản
          </label>
          <Input id='reg-username' placeholder='Nhập username...' defaultValue='tai' />
        </div>
        <div className='space-y-1'>
          <label htmlFor='reg-password' className='text-xs font-medium text-muted-foreground'>
            Mật khẩu
          </label>
          <Input id='reg-password' type='password' placeholder='Nhập password...' defaultValue='123456' />
        </div>
      </div>

      <Button type='submit' className='w-full'>
        Đăng ký
      </Button>

      <p className='text-center text-xs text-muted-foreground'>
        Đã có tài khoản?{' '}
        <Link to='/login' className='text-primary hover:underline font-medium'>
          Đăng nhập
        </Link>
      </p>
    </form>
  )
}
