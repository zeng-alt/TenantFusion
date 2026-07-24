import { usePermissionStore } from '@/store/modules/permission'
import { useUserStore } from '@/store/modules/user'

export function isSuperAdmin(target = null) {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const admin = permissionStore.admin || {}
  const targetId = target ?? userStore?.userId
  return String(admin?.id) === String(targetId)
}

export function isAdmin(target = null) {
  const permissionStore = usePermissionStore()
  const admin = permissionStore.admin || {}
  if (target != null) {
    return (String(admin?.id) === String(target) || admin?.code?.toLowerCase() === target?.toLowerCase())
  }
  const userStore = useUserStore()
  return String(userStore.userId) === String(admin?.id) || userStore.currentRole?.code?.toLowerCase() === admin?.code?.toLowerCase()
}

export function hasMenu(code) {
  if (isAdmin())
    return true
  const permissionStore = usePermissionStore()
  return permissionStore.accessRoutes.some(p => p.name === code)
}
