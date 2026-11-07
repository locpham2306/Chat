import { AlertCircle, RefreshCw } from 'lucide-react'
import { Button } from '~/components/ui/button'

interface ConversationErrorProps {
  onRetry: () => void
}

export const ConversationError = ({ onRetry }: ConversationErrorProps) => {
  return (
    <div className='flex flex-col items-center justify-center p-6 text-center space-y-3 text-muted-foreground my-auto'>
      <div className='h-10 w-10 rounded-full bg-destructive/10 text-destructive flex items-center justify-center'>
        <AlertCircle className='h-5 w-5' />
      </div>
      <div>
        <p className='text-sm font-medium text-foreground'>Không tải được danh sách</p>
        <p className='text-xs text-muted-foreground mt-0.5'>Vui lòng thử lại sau</p>
      </div>
      <Button variant='outline' size='sm' onClick={onRetry} className='gap-1.5 text-xs'>
        <RefreshCw className='h-3.5 w-3.5' />
        Thử lại
      </Button>
    </div>
  )
}
