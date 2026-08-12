import { request } from '@/utils'

export default {
  read: (params = {}) => request.get('/admin/v1/log/oper/list', { params }),
  detail: id => request.get(`/admin/v1/log/oper/${id}`),
}
