interface ChatEmptyStateProps {
  notice?: string
}

export const ChatEmptyState = ({ notice }: ChatEmptyStateProps) => {
  return (
    <div className='flex-1 flex flex-col items-center justify-center p-6 text-center text-muted-foreground bg-background'>
      {notice && (
        <div className='mb-6 rounded-lg bg-destructive/10 px-4 py-2.5 text-xs text-destructive font-medium border border-destructive/20 max-w-sm'>
          {notice}
        </div>
      )}
      <img src='/logo.png' alt='ChitChat' className='h-16 w-16 rounded-3xl object-cover mb-4 shadow-sm' />
      <h2 className='text-base font-semibold text-foreground'>Chào mừng bạn đến với ChitChat</h2>
      <p className='text-xs text-muted-foreground mt-1.5 max-w-sm leading-relaxed'>
        Chọn một cuộc trò chuyện ở danh sách bên trái hoặc bắt đầu tin nhắn mới để trao đổi.
      </p>
    </div>
  )
}
