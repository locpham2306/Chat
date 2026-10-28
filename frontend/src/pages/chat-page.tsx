import axios from 'axios'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { ChatEmptyState, ChatLayout } from '~/components/chat-layout'
import { ConversationList } from '~/components/conversation-list/conversation-list'
import { DirectConversationDraft } from '~/components/direct-conversation'
import { CreateGroupDialog } from '~/components/group-create'
import { UserSearchDialog } from '~/components/user-search'
import { useDirectConversationLookupQuery, useDirectConversationMutation } from '~/hooks/use-conversation'
import { useRealtimeMessageSync } from '~/hooks/use-realtime-message-sync'
import { getApiErrorMessage } from '~/lib/api-error'
import { cn } from '~/lib/utils'
import type { User } from '~/types/user.type'

interface DirectConversationLocationState {
  recipient?: User
  chatNotFound?: boolean
}

export const ChatPage = () => {
  const { conversationId } = useParams()
  const [searchParams] = useSearchParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [isUserSearchOpen, setIsUserSearchOpen] = useState(false)
  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false)
  const selectionVersion = useRef(0)
  const directConversationMutation = useDirectConversationMutation()
  const recipientId = searchParams.get('recipientId')
  const locationState = location.state as DirectConversationLocationState | null
  const recipient = locationState?.recipient
  const chatNotFound = Boolean(locationState?.chatNotFound)

  const { data: lookupData, isLoading: isLookingUp, error: lookupError } = useDirectConversationLookupQuery(recipientId)

  useRealtimeMessageSync({
    activeConversationId: conversationId,
    onMessageAck: (payload) => {
      if (!conversationId && recipientId && payload.conversationId) {
        navigate(`/chat/${payload.conversationId}`, { replace: true })
      }
    }
  })

  useEffect(() => {
    if (!recipientId) return

    if (lookupData?.kind === 'existing') {
      navigate(`/chat/${lookupData.conversationId}`, { replace: true })
    }
  }, [recipientId, lookupData, navigate])

  useEffect(() => {
    if (!recipientId || !lookupError) return

    if (axios.isAxiosError(lookupError)) {
      const status = lookupError.response?.status
      if (status === 400 || status === 404) {
        navigate('/chat', { replace: true, state: { chatNotFound: true } })
      }
    }
  }, [recipientId, lookupError, navigate])

  const draftRecipient: User | undefined = lookupData?.peer
    ? {
        id: lookupData.peer.id,
        username: lookupData.peer.username,
        fullName: lookupData.peer.fullName,
        avatarFileId: lookupData.peer.avatarFileId,
        status: (lookupData.peer.status as User['status']) || 'OFFLINE',
        lastSeen: null
      }
    : recipient

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
          onOpenCreateGroup={() => setIsCreateGroupOpen(true)}
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
          isLookingUp ? (
            <div className='flex flex-1 h-full items-center justify-center bg-background'>
              <div className='flex flex-col items-center gap-2'>
                <div className='h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent' />
                <span className='text-xs text-muted-foreground'>Đang tải cuộc trò chuyện...</span>
              </div>
            </div>
          ) : (
            <DirectConversationDraft recipientId={recipientId} recipient={draftRecipient} onBack={handleBackToList} />
          )
        ) : (
          <ChatEmptyState notice={chatNotFound ? 'Không tìm thấy người dùng hoặc cuộc hội thoại.' : undefined} />
        )}
      </section>

      <UserSearchDialog
        open={isUserSearchOpen}
        onOpenChange={handleUserSearchOpenChange}
        onSelectUser={handleSelectUser}
        selectingUserId={directConversationMutation.isPending ? directConversationMutation.variables : undefined}
        selectionError={getApiErrorMessage(directConversationMutation.error)}
      />

      <CreateGroupDialog
        open={isCreateGroupOpen}
        onOpenChange={setIsCreateGroupOpen}
        onSuccess={(newGroup) => {
          navigate(`/chat/${newGroup.id}`)
        }}
      />
    </div>
  )
}
