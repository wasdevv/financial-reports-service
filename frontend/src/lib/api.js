// Cliente fino sobre fetch. O token vive no localStorage; um 401 em qualquer chamada
// autenticada limpa a sessão e avisa o AuthProvider (token expirado, usuário desativado).
const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')
const TOKEN_KEY = 'fr.token'

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn }

export const tokenStore = {
  get: () => { try { return localStorage.getItem(TOKEN_KEY) } catch { return null } },
  set: (t) => { try { localStorage.setItem(TOKEN_KEY, t) } catch { /* modo privado */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY) } catch { /* idem */ } },
}

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export async function request(path, { method = 'GET', body, fetchImpl = fetch } = {}) {
  const token = tokenStore.get()
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetchImpl(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    throw new ApiError(0, "Can't reach the API. It may be waking up, try again in a few seconds.")
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (res.ok) return data

  if (res.status === 401 && token) {
    tokenStore.clear()
    onUnauthorized()
  }
  const message = (data && typeof data === 'object' && data.detail) || `Request failed (${res.status})`
  const fieldErrors = (data && typeof data === 'object' && data.errors) || {}
  throw new ApiError(res.status, message, fieldErrors)
}

export const api = {
  login: (email, password) => request('/auth/login', { method: 'POST', body: { email, password } }),
  register: (data) => request('/auth/register', { method: 'POST', body: data }),
  me: () => request('/auth/me'),
  reports: (params = {}) => {
    const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null && v !== false))
    return request(`/reports${qs.size ? `?${qs}` : ''}`)
  },
  report: (id) => request(`/reports/${id}`),
  createReport: (data) => request('/reports', { method: 'POST', body: data }),
  updateReport: (id, data) => request(`/reports/${id}`, { method: 'PUT', body: data }),
  deleteReport: (id) => request(`/reports/${id}`, { method: 'DELETE' }),
  addRecord: (id, data) => request(`/reports/${id}/records`, { method: 'POST', body: data }),
  deleteRecord: (id, recordId) => request(`/reports/${id}/records/${recordId}`, { method: 'DELETE' }),
  submit: (id) => request(`/reports/${id}/submit`, { method: 'POST' }),
  approve: (id, note) => request(`/reports/${id}/approve`, { method: 'POST', body: { note } }),
  reject: (id, note) => request(`/reports/${id}/reject`, { method: 'POST', body: { note } }),
  summary: () => request('/analytics/summary'),
  users: () => request('/admin/users'),
  updateUser: (id, data) => request(`/admin/users/${id}`, { method: 'PATCH', body: data }),
  auditLogs: () => request('/admin/audit-logs?size=100'),
}
