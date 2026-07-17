import { request } from '@/utils'

export default {
  page: (params = {}) => request.get('/http', { params }),
  create: data => request.post('/http', data),
  update: data => request.patch(`/http/${data.permissionId}`, data),
  delete: ids => request.delete(`/http/${ids}`),
}
