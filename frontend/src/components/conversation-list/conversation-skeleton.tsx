export const ConversationSkeleton = () => {
  return (
    <div className='space-y-2 p-2'>
      {Array.from({ length: 5 }).map((_, index) => (
        <div key={index} className='flex items-center gap-3 p-2.5 rounded-lg border bg-card/50 animate-pulse'>
          <div className='h-10 w-10 rounded-full bg-muted shrink-0' />
          <div className='flex-1 space-y-2'>
            <div className='h-3.5 w-24 bg-muted rounded' />
            <div className='h-3 w-36 bg-muted/60 rounded' />
          </div>
        </div>
      ))}
    </div>
  )
}
