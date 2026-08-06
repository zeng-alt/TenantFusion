import { request } from '@/utils'

export default {
  create: data => request.post('/admin/v1/dict/type', data),
  read: (params = {}) => request.get('/admin/v1/dict/type', { params }),
  update: data => request.patch(`/admin/v1/dict/type/${data.dictTypeId}`, data),
  delete: ids => request.delete(`/admin/v1/dict/type/${ids}`),

  createData: data => request.post('/admin/v1/dict/data', data),
  readData: (params = {}) => request.get('/admin/v1/dict/data', { params }),
  updateData: data => request.patch(`/admin/v1/dict/data/${data.dictDataId}`, data),
  deleteData: ids => request.delete(`/admin/v1/dict/data/${ids}`),
  sortData: data => request.put('/admin/v1/dict/data/sort', data),
}
