import { useParams } from 'react-router-dom'
import { MessageSquare, Users, Plus } from 'lucide-react'
import { Button } from '~/components/ui/button'

export const ChatPage = () => {
  const { conversationId } = useParams()

  return (
    <div className='flex h-full w-full overflow-hidden'>
      {/* Sidebar Placeholder cho FE-06 */}
      <aside className='w-80 border-r bg-card shrink-0 flex flex-col hidden sm:flex'>
        <div className='p-3 border-b flex items-center justify-between'>
          <span className='text-sm font-semibold'>Đoạn chat</span>
          <Button variant='ghost' size='icon' className='h-8 w-8' aria-label='Tạo nhóm mới'>
            <Plus className='h-4 w-4' />
          </Button>
        </div>
        <div className='flex-1 p-3 overflow-y-auto space-y-2 text-sm text-muted-foreground'>
          <div className='flex items-center gap-3 p-2.5 rounded-lg border bg-accent/30 cursor-pointer'>
            <div className='h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center text-primary shrink-0'>
              <Users className='h-4 w-4' />
            </div>
            <div className='flex-1 min-w-0'>
              <p className='font-medium text-foreground text-xs truncate'>Nhóm BTL Mạng</p>
              <p className='text-xs truncate'>Tài: Testing giao diện...</p>
            </div>
          </div>
          <p className='text-center text-xs py-4'>Chờ kết nối danh sách hội thoại (FE-06)</p>
        </div>
      </aside>

      {/* Vùng chat chính Placeholder cho FE-08 */}
      <section className='flex-1 flex flex-col items-center justify-center p-6 text-center bg-background'>
        <div className='flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground mb-3'>
          <MessageSquare className='h-6 w-6' />
        </div>
        <h2 className='text-base font-semibold'>
          {conversationId ? `Hội thoại #${conversationId}` : 'Chưa chọn cuộc trò chuyện'}
        </h2>
        <p className='text-xs text-muted-foreground mt-1 max-w-sm'>
          Chọn một cuộc trò chuyện ở danh sách bên trái hoặc bắt đầu tin nhắn mới.
        </p>
      </section>
    </div>
  )
}
