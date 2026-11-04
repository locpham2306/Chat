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
      <div className='flex h-16 w-16 items-center justify-center rounded-2xl bg-card border border-border/40 p-1.5 shadow-xs mb-4 overflow-hidden'>
        <img src='/logo.png' alt='ChitChat' className='h-full w-full object-contain' />
      </div>
      <h2 className='text-base font-semibold text-foreground'>Chào mừng bạn đến với ChitChat</h2>
      <p className='text-xs text-muted-foreground mt-1.5 max-w-sm leading-relaxed'>
        Chọn một cuộc trò chuyện ở danh sách bên trái hoặc bắt đầu tin nhắn mới để trao đổi.
      </p>
    </div>
  )
}
