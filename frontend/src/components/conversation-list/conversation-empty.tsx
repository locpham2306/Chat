import { MessageSquare } from 'lucide-react'

export const ConversationEmpty = () => {
  return (
    <div className='flex flex-col items-center justify-center p-6 text-center space-y-2 text-muted-foreground my-auto'>
      <div className='h-10 w-10 rounded-full bg-muted flex items-center justify-center'>
        <MessageSquare className='h-5 w-5' />
      </div>
      <p className='text-sm font-medium text-foreground'>Chưa có hội thoại nào</p>
      <p className='text-xs'>Bắt đầu nhắn tin hoặc tạo nhóm mới</p>
    </div>
  )
}
