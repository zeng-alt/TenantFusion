import { request } from '@/utils'

export default {
  create: data => request.post('/dict/type', data),
  read: (params = {}) => request.get('/dict/type', { params }),
  update: data => request.patch(`/dict/type/${data.dictTypeId}`, data),
  delete: ids => request.delete(`/dict/type/${ids}`),

  createData: data => request.post('/dict/data', data),
  readData: (params = {}) => request.get('/dict/data', { params }),
  updateData: data => request.patch(`/dict/data/${data.dictDataId}`, data),
  deleteData: ids => request.delete(`/dict/data/${ids}`),
  sortData: data => request.put('/dict/data/sort', data),
}
