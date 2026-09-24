import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Link, Route, Routes } from 'react-router'
import './index.css'
import { AuthProvider, RequireAuth } from './auth'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import Dashboard from './pages/Dashboard'
import Reports from './pages/Reports'
import ReportDetail from './pages/ReportDetail'
import Admin from './pages/Admin'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route element={<RequireAuth><Layout /></RequireAuth>}>
            <Route index element={<Dashboard />} />
            <Route path="reports" element={<Reports />} />
            <Route path="reports/:id" element={<ReportDetail />} />
            <Route path="admin" element={<RequireAuth role="ADMIN"><Admin /></RequireAuth>} />
            <Route path="*" element={<p className="text-ink-2">Page not found. <Link className="text-accent underline" to="/">Go to dashboard</Link></p>} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
