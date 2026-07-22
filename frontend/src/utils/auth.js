import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore } from '@/store/modules/user'

export function isSuperAdmin(target = null) {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const admin = permissionStore.admin || {}
  const targetId = target ?? userStore?.userId
  return admin?.id === targetId
}

export function isAdmin(target = null) {
  const permissionStore = usePermissionStore()
  const admin = permissionStore.admin || {}
  if (target != null) {
    return (admin?.id === target || admin?.code?.toLowerCase() === target?.toLowerCase())
  }
  const userStore = useUserStore()
  return userStore.userId !== admin?.id || userStore.currentRole?.code?.toLowerCase() === admin?.code?.toLowerCase()
}
