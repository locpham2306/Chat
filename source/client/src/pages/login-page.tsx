import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '~/components/ui/button'
import { Input } from '~/components/ui/input'
import { useLoginMutation } from '~/hooks/use-auth'
import { getApiError, getApiErrorMessage } from '~/lib/api-error'
import { loginSchema, type LoginBody } from '~/types/auth.type'

export const LoginPage = () => {
  const [showPassword, setShowPassword] = useState(false)
  const [serverFocusField, setServerFocusField] = useState<keyof LoginBody | null>(null)
  const navigate = useNavigate()
  const location = useLocation()
  const loginMutation = useLoginMutation()
  const successMessage = (location.state as { successMessage?: string } | null)?.successMessage
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    formState: { errors }
  } = useForm<LoginBody>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: ''
    }
  })

  useEffect(() => {
    if (loginMutation.isPending || !serverFocusField) return

    setFocus(serverFocusField)
  }, [loginMutation.isPending, serverFocusField, setFocus])

  const onSubmit = (body: LoginBody) => {
    setServerFocusField(null)
    loginMutation.mutate(body, {
      onSuccess: () => {
        navigate('/chat', { replace: true })
      },
      onError: (error: unknown) => {
        const apiError = getApiError(error)

        const supportedFieldErrors =
          apiError?.fieldErrors.filter(
            (fieldError) => fieldError.field === 'username' || fieldError.field === 'password'
          ) ?? []

        supportedFieldErrors.forEach((fieldError, index) => {
          const field = fieldError.field as keyof LoginBody
          setError(field, { type: 'server', message: fieldError.message })
          if (index === 0) setServerFocusField(field)
        })
      }
    })
  }

  const apiError = getApiError(loginMutation.error)
  const submitError =
    apiError?.status === 401 ? 'Tài khoản hoặc mật khẩu không đúng' : getApiErrorMessage(loginMutation.error)

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className='space-y-4'
      noValidate
      aria-labelledby='login-title'
      aria-describedby='login-description'
      aria-busy={loginMutation.isPending}
    >
      <div className='space-y-1 text-center'>
        <h2 id='login-title' className='text-lg font-semibold'>
          Đăng nhập
        </h2>
        <p id='login-description' className='text-xs text-muted-foreground'>
          Nhập tài khoản để tiếp tục
        </p>
      </div>

      {successMessage && (
        <p
          role='status'
          className='rounded-md border border-primary/30 bg-primary/10 px-3 py-2 text-xs text-foreground'
        >
          {successMessage}
        </p>
      )}

      {submitError && (
        <p
          role='alert'
          className='rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs text-auth-error-foreground'
        >
          {submitError}
        </p>
      )}

      <div className='space-y-3'>
        <div className='space-y-1'>
          <label htmlFor='username' className='text-xs font-medium text-muted-foreground'>
            Tài khoản
          </label>
          <Input
            id='username'
            placeholder='Nhập username...'
            autoComplete='username'
            aria-invalid={Boolean(errors.username)}
            aria-describedby={errors.username ? 'username-error' : undefined}
            disabled={loginMutation.isPending}
            {...register('username')}
          />
          {errors.username?.message && (
            <p id='username-error' role='alert' className='text-xs text-auth-error-foreground'>
              {errors.username.message}
            </p>
          )}
        </div>
        <div className='space-y-1'>
          <label htmlFor='password' className='text-xs font-medium text-muted-foreground'>
            Mật khẩu
          </label>
          <div className='relative'>
            <Input
              id='password'
              type={showPassword ? 'text' : 'password'}
              className='pr-10'
              placeholder='Nhập password...'
              autoComplete='current-password'
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'password-error' : undefined}
              disabled={loginMutation.isPending}
              {...register('password')}
            />
            <button
              type='button'
              className='absolute inset-y-0 right-0 flex w-10 items-center justify-center text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50'
              aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              aria-pressed={showPassword}
              disabled={loginMutation.isPending}
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
            <p id='password-error' role='alert' className='text-xs text-auth-error-foreground'>
              {errors.password.message}
            </p>
          )}
        </div>
      </div>

      <Button type='submit' className='w-full' disabled={loginMutation.isPending}>
        {loginMutation.isPending && <LoaderCircle className='animate-spin' aria-hidden='true' />}
        {loginMutation.isPending ? 'Đang đăng nhập...' : 'Đăng nhập'}
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
