const ACTIVE_TENANT_KEY = 'tenantfusion_active_tenant'

export function getActiveTenant() {
  return sessionStorage.getItem(ACTIVE_TENANT_KEY)
}

export function setActiveTenant(tenant) {
  sessionStorage.setItem(ACTIVE_TENANT_KEY, tenant)
}

export function clearActiveTenant() {
  sessionStorage.removeItem(ACTIVE_TENANT_KEY)
}
