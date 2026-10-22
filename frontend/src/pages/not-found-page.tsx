import { Link } from 'react-router-dom'
import { Button } from '~/components/ui/button'

export const NotFoundPage = () => {
  return (
    <div className='flex h-screen w-full flex-col items-center justify-center p-4 text-center'>
      <h1 className='text-4xl font-bold'>404</h1>
      <p className='mt-2 text-sm text-muted-foreground'>Không tìm thấy trang này</p>
      <div className='mt-6'>
        <Button asChild variant='default'>
          <Link to='/'>Về trang chủ</Link>
        </Button>
      </div>
    </div>
  )
}
