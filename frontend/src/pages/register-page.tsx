import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, LoaderCircle } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '~/components/ui/button'
import { Input } from '~/components/ui/input'
import { useRegisterMutation } from '~/hooks/use-auth'
import { getApiError, getApiErrorMessage } from '~/lib/api-error'
import { registerSchema, type RegisterBody } from '~/types/auth.type'

export const RegisterPage = () => {
  const [showPassword, setShowPassword] = useState(false)
  const navigate = useNavigate()
  const registerMutation = useRegisterMutation()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors }
  } = useForm<RegisterBody>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: '',
      username: '',
      password: ''
    }
  })

  const onSubmit = (body: RegisterBody) => {
    registerMutation.mutate(body, {
      onSuccess: () => {
        navigate('/login', {
          replace: true,
          state: { successMessage: 'Đăng ký thành công. Bạn có thể đăng nhập ngay.' }
        })
      },
      onError: (error) => {
        const apiError = getApiError(error)

        if (apiError?.status === 409) {
          setError('username', { type: 'server', message: 'Tài khoản đã tồn tại' })
          return
        }

        apiError?.fieldErrors.forEach((fieldError) => {
          if (fieldError.field === 'fullName' || fieldError.field === 'username' || fieldError.field === 'password') {
            setError(fieldError.field, { type: 'server', message: fieldError.message })
          }
        })
      }
    })
  }

  const apiError = getApiError(registerMutation.error)
  const submitError = apiError?.status === 409 ? undefined : getApiErrorMessage(registerMutation.error)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className='space-y-4' noValidate>
      <div className='space-y-1 text-center'>
        <h2 className='text-lg font-semibold'>Tạo tài khoản</h2>
        <p className='text-xs text-muted-foreground'>Điền thông tin để bắt đầu</p>
      </div>

      {submitError && (
        <p role='alert' className='rounded-md border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700'>
          {submitError}
        </p>
      )}

      <div className='space-y-3'>
        <div className='space-y-1'>
          <label htmlFor='fullName' className='text-xs font-medium text-muted-foreground'>
            Họ và tên
          </label>
          <Input
            id='fullName'
            placeholder='Nhập họ và tên...'
            autoComplete='name'
            aria-invalid={Boolean(errors.fullName)}
            aria-describedby={errors.fullName ? 'full-name-error' : undefined}
            disabled={registerMutation.isPending}
            {...register('fullName')}
          />
          {errors.fullName?.message && (
            <p id='full-name-error' role='alert' className='text-xs text-red-600'>
              {errors.fullName.message}
            </p>
          )}
        </div>
        <div className='space-y-1'>
          <label htmlFor='reg-username' className='text-xs font-medium text-muted-foreground'>
            Tài khoản
          </label>
          <Input
            id='reg-username'
            placeholder='Nhập username...'
            autoComplete='username'
            aria-invalid={Boolean(errors.username)}
            aria-describedby={errors.username ? 'reg-username-error' : undefined}
            disabled={registerMutation.isPending}
            {...register('username')}
          />
          {errors.username?.message && (
            <p id='reg-username-error' role='alert' className='text-xs text-red-600'>
              {errors.username.message}
            </p>
          )}
        </div>
        <div className='space-y-1'>
          <label htmlFor='reg-password' className='text-xs font-medium text-muted-foreground'>
            Mật khẩu
          </label>
          <div className='relative'>
            <Input
              id='reg-password'
              type={showPassword ? 'text' : 'password'}
              className='pr-10'
              placeholder='Nhập password...'
              autoComplete='new-password'
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'reg-password-error' : undefined}
              disabled={registerMutation.isPending}
              {...register('password')}
            />
            <button
              type='button'
              className='absolute inset-y-0 right-0 flex w-10 items-center justify-center text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50'
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              aria-pressed={showPassword}
              disabled={registerMutation.isPending}
              onClick={() => setShowPassword((current) => !current)}
            >
              {showPassword ? (
                <EyeOff className='h-4 w-4' aria-hidden='true' />
              ) : (
                <Eye className='h-4 w-4' aria-hidden='true' />
              )}
            </button>
          </div>
          {errors.password?.message && (
            <p id='reg-password-error' role='alert' className='text-xs text-red-600'>
              {errors.password.message}
            </p>
          )}
        </div>
      </div>

      <Button type='submit' className='w-full' disabled={registerMutation.isPending}>
        {registerMutation.isPending && <LoaderCircle className='animate-spin' aria-hidden='true' />}
        {registerMutation.isPending ? 'Đang đăng ký...' : 'Đăng ký'}
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
