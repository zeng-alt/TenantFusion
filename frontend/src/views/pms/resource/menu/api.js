import { request } from '@/utils'

export default {
  // Menu
  getMenuTree: () => request.get('/admin/v1/menu/resource/tree/menu'),
  createMenu: data => request.post('/admin/v1/menu', data),
  updateMenu: data => request.patch(`/admin/v1/menu/${data.id}`, data),
  deleteMenu: ids => request.delete(`/admin/v1/menu/${ids}`),

  // HTTP Resource
  associateHttp: data => request.post('/admin/v1/resource/http/associate', data),
  disconnectHttp: id => request.patch(`/admin/v1/resource/http/${id}/disconnect`),
}
