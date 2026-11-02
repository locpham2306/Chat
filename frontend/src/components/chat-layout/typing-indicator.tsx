interface TypingIndicatorProps {
  text: string
  visible: boolean
}

export const TypingIndicator = ({ text, visible }: TypingIndicatorProps) => {
  if (!visible || !text) return null

  return (
    <div
      role='status'
      aria-live='polite'
      className='flex items-center gap-2 px-1 py-1 text-xs text-muted-foreground transition-all duration-200 animate-in fade-in slide-in-from-bottom-1'
    >
      <div className='flex items-center gap-1 rounded-full bg-muted/80 px-2.5 py-1.5 border border-border/40 shadow-xs'>
        <span
          className='h-1.5 w-1.5 rounded-full bg-foreground/60 animate-bounce'
          style={{ animationDelay: '-0.32s' }}
        />
        <span
          className='h-1.5 w-1.5 rounded-full bg-foreground/60 animate-bounce'
          style={{ animationDelay: '-0.16s' }}
        />
        <span className='h-1.5 w-1.5 rounded-full bg-foreground/60 animate-bounce' />
      </div>
      <span className='text-[11px] font-medium text-muted-foreground select-none'>{text}</span>
    </div>
  )
}
