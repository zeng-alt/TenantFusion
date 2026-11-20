import { request } from '@/utils'

const base = '/admin/v1/abac'

export default {
  permissions: () => request.get(`${base}/permissions`),
  metadata: params => request.get(`${base}/metadata`, { params }),
  policies: permission => request.get(`${base}/policies`, { params: { permission } }),
  simulate: command => request.post(`${base}/simulations`, command),
  create: command => request.post(`${base}/policies`, command),
  update: (id, command) => request.put(`${base}/policies/${id}`, command),
  delete: id => request.delete(`${base}/policies/${id}`),
  refresh: permission => request.post(`${base}/policies/refresh`, null, { params: { permission } }),
}
