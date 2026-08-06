import { request } from '@/utils'

export default {
  create: data => request.post('/admin/v1/dept', data),
  read: (params = {}) => request.get('/admin/v1/dept', { params }),
  update: data => request.patch(`/admin/v1/dept/${data.deptId}`, data),
  delete: ids => request.delete(`/admin/v1/dept/${ids}`),
  tree: () => request.get('/admin/v1/dept/tree'),
}
