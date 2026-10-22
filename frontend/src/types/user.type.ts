export type UserStatus = 'ONLINE' | 'OFFLINE'

export interface User {
  id: string
  username: string
  fullName: string
  avatarFileId: string | null
  status: UserStatus
  lastSeen: string | null
}
