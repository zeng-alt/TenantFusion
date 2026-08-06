import { request } from '@/utils'

export default {
  page: (params = {}) => request.get('/admin/v1/http', { params }),
  create: data => request.post('/admin/v1/http', data),
  update: data => request.patch(`/admin/v1/http/${data.permissionId}`, data),
  delete: ids => request.delete(`/admin/v1/http/${ids}`),
}
