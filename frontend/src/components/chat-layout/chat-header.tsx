import { ArrowLeft, MoreVertical, Users, User as UserIcon } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from '~/components/ui/avatar'
import { Button } from '~/components/ui/button'

interface ChatHeaderProps {
  title: string
  isGroup?: boolean
  subtitle?: string
  avatarUrl?: string | null
  onBack?: () => void
  onOpenInfo?: () => void
}

export const ChatHeader = ({ title, isGroup = false, subtitle, avatarUrl, onBack, onOpenInfo }: ChatHeaderProps) => {
  return (
    <div className='h-14 border-b px-4 flex items-center justify-between bg-card/50 backdrop-blur shrink-0'>
      <div className='flex items-center gap-3 min-w-0'>
        {onBack && (
          <Button
            variant='ghost'
            size='icon'
            onClick={onBack}
            className='h-8 w-8 sm:hidden shrink-0'
            aria-label='Quay lại danh sách'
          >
            <ArrowLeft className='h-4 w-4' />
          </Button>
        )}

        <Avatar className='h-9 w-9 shrink-0'>
          {avatarUrl && <AvatarImage src={avatarUrl} alt={title} />}
          <AvatarFallback className={isGroup ? 'bg-primary/10 text-primary' : undefined}>
            {isGroup ? <Users className='h-4 w-4' /> : <UserIcon className='h-4 w-4' />}
          </AvatarFallback>
        </Avatar>

        <div className='min-w-0'>
          <h2 className='text-sm font-semibold truncate leading-tight'>{title}</h2>
          {subtitle && <p className='text-[11px] text-muted-foreground truncate leading-tight mt-0.5'>{subtitle}</p>}
        </div>
      </div>

      {onOpenInfo && (
        <div className='flex shrink-0 items-center gap-1'>
          <Button
            variant='ghost'
            size='icon'
            onClick={onOpenInfo}
            className='h-8 w-8 text-muted-foreground hover:text-foreground'
            aria-label='Tùy chọn hội thoại'
            title='Tùy chọn hội thoại'
          >
            <MoreVertical className='h-4 w-4' />
          </Button>
        </div>
      )}
    </div>
  )
}
