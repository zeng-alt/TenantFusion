import { request } from '@/utils'

export default {
  create: data => request.post('/dept', data),
  read: (params = {}) => request.get('/dept', { params }),
  update: data => request.patch(`/dept/${data.deptId}`, data),
  delete: ids => request.delete(`/dept/${ids}`),
  tree: () => request.get('/dept/tree'),
}
