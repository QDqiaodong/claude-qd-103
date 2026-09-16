import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const granaryApi = {
  list: (params) => http.get('/granaries', { params }),
  create: (data) => http.post('/granaries', data),
  update: (id, data) => http.put(`/granaries/${id}`, data)
}

export const batchApi = {
  list: (params) => http.get('/batches', { params }),
  create: (data) => http.post('/batches', data),
  update: (id, data) => http.put(`/batches/${id}`, data)
}

export const tempApi = {
  list: (params) => http.get('/temps', { params }),
  create: (data) => http.post('/temps', data)
}

export const moveApi = {
  list: (params) => http.get('/moves', { params }),
  create: (data) => http.post('/moves', data),
  execute: (id) => http.post(`/moves/${id}/execute`)
}

export default http
