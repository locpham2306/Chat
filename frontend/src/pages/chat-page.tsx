import { useRef, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ChatEmptyState, ChatLayout } from '~/components/chat-layout'
import { ConversationList } from '~/components/conversation-list/conversation-list'
import { DirectConversationDraft } from '~/components/direct-conversation'
import { UserSearchDialog } from '~/components/user-search'
import { useDirectConversationMutation } from '~/hooks/use-conversation'
import { getApiErrorMessage } from '~/lib/api-error'
import { cn } from '~/lib/utils'
import type { User } from '~/types/user.type'

interface DirectConversationLocationState {
  recipient?: User
}

export const ChatPage = () => {
  const { conversationId } = useParams()
  const [searchParams] = useSearchParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [isUserSearchOpen, setIsUserSearchOpen] = useState(false)
  const selectionVersion = useRef(0)
  const directConversationMutation = useDirectConversationMutation()
  const recipientId = searchParams.get('recipientId')
  const recipient = (location.state as DirectConversationLocationState | null)?.recipient
  const hasOpenChat = Boolean(conversationId || recipientId)

  const handleSelectConversation = (id: string) => {
    selectionVersion.current += 1
    directConversationMutation.reset()
    navigate(`/chat/${id}`)
  }

  const handleBackToList = () => {
    selectionVersion.current += 1
    directConversationMutation.reset()
    navigate('/chat')
  }

  const handleUserSearchOpenChange = (open: boolean) => {
    setIsUserSearchOpen(open)
    if (!open) {
      selectionVersion.current += 1
      directConversationMutation.reset()
    }
  }

  const handleSelectUser = async (user: User) => {
    const currentVersion = selectionVersion.current + 1
    selectionVersion.current = currentVersion

    try {
      const result = await directConversationMutation.mutateAsync(user.id)
      if (selectionVersion.current !== currentVersion) return

      setIsUserSearchOpen(false)
      if (result.kind === 'existing') {
        navigate(`/chat/${result.conversationId}`)
        return
      }

      navigate(`/chat?recipientId=${encodeURIComponent(result.recipientId)}`, {
        state: { recipient: user } satisfies DirectConversationLocationState
      })
    } catch {
      // Mutation state keeps the dialog open and exposes the API error for retry.
    }
  }

  return (
    <div className='flex h-full w-full overflow-hidden'>
      {/* Sidebar: Danh sách hội thoại */}
      <aside
        className={cn(
          'w-full sm:w-80 border-r bg-card shrink-0 h-full flex flex-col',
          hasOpenChat ? 'hidden sm:flex' : 'flex'
        )}
      >
        <ConversationList
          activeId={conversationId}
          onSelectConversation={handleSelectConversation}
          onOpenUserSearch={() => setIsUserSearchOpen(true)}
        />
      </aside>

      {/* Vùng chat chính (FE-08) */}
      <section
        className={cn(
          'flex-1 flex-col h-full bg-background',
          hasOpenChat ? 'flex' : 'hidden sm:flex items-center justify-center'
        )}
      >
        {conversationId ? (
          <ChatLayout conversationId={conversationId} onBack={handleBackToList} />
        ) : recipientId ? (
          <DirectConversationDraft recipientId={recipientId} recipient={recipient} onBack={handleBackToList} />
        ) : (
          <ChatEmptyState />
        )}
      </section>

      <UserSearchDialog
        open={isUserSearchOpen}
        onOpenChange={handleUserSearchOpenChange}
        onSelectUser={handleSelectUser}
        selectingUserId={directConversationMutation.isPending ? directConversationMutation.variables : undefined}
        selectionError={getApiErrorMessage(directConversationMutation.error)}
      />
    </div>
  )
}
