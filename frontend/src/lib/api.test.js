import { beforeEach, describe, expect, it, vi } from 'vitest'

const store = new Map()
globalThis.localStorage = {
  getItem: (k) => (store.has(k) ? store.get(k) : null),
  setItem: (k, v) => store.set(k, String(v)),
  removeItem: (k) => store.delete(k),
}

const { request, tokenStore, setUnauthorizedHandler, ApiError } = await import('./api.js')

const reply = (status, body) => vi.fn(async () => ({
  status,
  ok: status >= 200 && status < 300,
  json: async () => (body === undefined ? Promise.reject(new Error('no body')) : body),
}))

describe('request', () => {
  beforeEach(() => store.clear())

  it('sends the bearer token and JSON body', async () => {
    tokenStore.set('abc')
    const fetchImpl = reply(200, { ok: 1 })
    await request('/reports', { method: 'POST', body: { a: 1 }, fetchImpl })
    const [url, init] = fetchImpl.mock.calls[0]
    expect(url).toMatch(/\/reports$/)
    expect(init.headers.Authorization).toBe('Bearer abc')
    expect(init.body).toBe('{"a":1}')
  })

  it('clears the session and notifies on 401 with a token', async () => {
    tokenStore.set('expired')
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    await expect(request('/auth/me', { fetchImpl: reply(401, { detail: 'Authentication required' }) }))
      .rejects.toMatchObject({ status: 401, message: 'Authentication required' })
    expect(tokenStore.get()).toBeNull()
    expect(handler).toHaveBeenCalledOnce()
  })

  it('does not log out on a failed login (no token yet)', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    await expect(request('/auth/login', { method: 'POST', body: {}, fetchImpl: reply(401, { detail: 'Invalid email or password' }) }))
      .rejects.toBeInstanceOf(ApiError)
    expect(handler).not.toHaveBeenCalled()
  })

  it('exposes field errors and survives non-JSON and array bodies', async () => {
    const err = await request('/x', { fetchImpl: reply(400, { detail: 'Validation failed', errors: { email: 'bad' } }) }).catch((e) => e)
    expect(err.fieldErrors).toEqual({ email: 'bad' })
    const err2 = await request('/x', { fetchImpl: reply(502) }).catch((e) => e)
    expect(err2.message).toBe('Request failed (502)')
    const err3 = await request('/x', { fetchImpl: reply(500, []) }).catch((e) => e)
    expect(err3.message).toBe('Request failed (500)')
  })

  it('turns network failure into a readable error', async () => {
    const err = await request('/x', { fetchImpl: vi.fn(async () => { throw new TypeError('fetch failed') }) }).catch((e) => e)
    expect(err.status).toBe(0)
  })

  it('returns null on 204', async () => {
    expect(await request('/x', { method: 'DELETE', fetchImpl: reply(204) })).toBeNull()
  })
})
