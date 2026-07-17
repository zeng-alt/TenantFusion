import { request } from '@/utils'

export default {
  // Menu
  getMenuTree: () => request.get('/menu/resource/tree/menu'),
  createMenu: data => request.post('/menu', data),
  updateMenu: (id, data) => request.patch(`/menu/${id}`, data),
  deleteMenu: ids => request.delete(`/menu/${ids}`),

  // HTTP Resource
  associateHttp: data => request.post('/resource/http/associate', data),
  disconnectHttp: id => request.patch(`/resource/http/${id}/disconnect`),
}
