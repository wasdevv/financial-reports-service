import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { api, setUnauthorizedHandler, tokenStore } from './lib/api'

const AuthContext = createContext(null)

// O usuário nunca é lido do storage: no reload, /auth/me valida o token e traz o papel atual.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState(tokenStore.get() ? 'checking' : 'anonymous')

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    setStatus('anonymous')
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) return
    api.me()
      .then((u) => { setUser(u); setStatus('authenticated') })
      .catch((e) => { if (e.status === 401) logout(); else setStatus('offline') })
  }, [logout])

  const start = useCallback(({ token, user }) => {
    tokenStore.set(token)
    setUser(user)
    setStatus('authenticated')
  }, [])

  const value = {
    user,
    status,
    login: async (email, password) => start(await api.login(email, password)),
    register: async (data) => start(await api.register(data)),
    logout,
  }
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)

export function RequireAuth({ children, role }) {
  const { user, status } = useAuth()
  const location = useLocation()
  if (status === 'checking') return <div className="p-8 text-ink-3" role="status">Checking session...</div>
  if (status === 'offline') {
    return (
      <div className="mx-auto max-w-md p-8 text-center" role="alert">
        <p className="font-medium">Can't reach the API.</p>
        <p className="mt-1 text-sm text-ink-2">The free-tier server may be waking up. Reload in a few seconds.</p>
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (role && user.role !== role) return <Navigate to="/" replace />
  return children
}
