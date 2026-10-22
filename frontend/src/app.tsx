import { useState } from 'react'
import { Avatar, AvatarFallback } from '~/components/ui/avatar'
import { Badge } from '~/components/ui/badge'
import { Button } from '~/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from '~/components/ui/dialog'
import { Input } from '~/components/ui/input'
import { MessageSquare, Moon, Sun } from 'lucide-react'

const App = () => {
  const [isDark, setIsDark] = useState(false)

  const toggleTheme = () => {
    setIsDark(!isDark)
    document.documentElement.classList.toggle('dark')
  }

  return (
    <main className='min-h-screen bg-background text-foreground flex flex-col items-center justify-center p-4'>
      <div className='w-full max-w-md space-y-5 rounded-xl border bg-card p-5 shadow-sm'>
        <header className='flex items-center justify-between border-b pb-3'>
          <div className='flex items-center gap-2.5'>
            <div className='flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground'>
              <MessageSquare className='h-5 w-5' />
            </div>
            <div>
              <h1 className='text-base font-semibold leading-tight'>Chat BTL LTM</h1>
              <p className='text-xs text-muted-foreground'>Phòng chat demo</p>
            </div>
          </div>
          <Button variant='ghost' size='icon' onClick={toggleTheme} aria-label='Đổi giao diện sáng tối'>
            {isDark ? <Sun className='h-4 w-4' /> : <Moon className='h-4 w-4' />}
          </Button>
        </header>

        <section className='space-y-4'>
          <div className='flex items-center gap-3 rounded-lg border p-3'>
            <Avatar>
              <AvatarFallback>T</AvatarFallback>
            </Avatar>
            <div className='flex-1 min-w-0'>
              <div className='flex items-center gap-2'>
                <span className='text-sm font-medium'>Tài</span>
                <Badge variant='online'>Online</Badge>
              </div>
              <p className='text-xs text-muted-foreground truncate'>Testing</p>
            </div>
          </div>

          <div className='space-y-1.5'>
            <label htmlFor='test-input' className='text-xs font-medium text-muted-foreground'>
              Tin nhắn nhanh
            </label>
            <Input id='test-input' placeholder='Nhập tin nhắn thử...' />
          </div>

          <div className='flex flex-wrap items-center gap-2 pt-1'>
            <Button variant='default'>Gửi tin</Button>
            <Button variant='secondary'>Hủy</Button>

            <Dialog>
              <DialogTrigger asChild>
                <Button variant='outline'>Xem chi tiết</Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Thông báo</DialogTitle>
                  <DialogDescription>Test</DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant='default'>Done</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </section>
      </div>
    </main>
  )
}

export default App
