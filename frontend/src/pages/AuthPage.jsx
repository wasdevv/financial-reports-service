import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { ShieldCheck, Scales, ClockCounterClockwise } from '@phosphor-icons/react'
import { useAuth } from '../auth'
import { Button, ErrorBox, Field } from '../components/ui'
import { Logo } from '../components/Layout'

const POINTS = [
  { icon: Scales, title: 'Risk scored on the server', body: 'Every income and expense line recalculates a 0 to 100 score with the factors that caused it.' },
  { icon: ShieldCheck, title: 'Approval with separation of duties', body: 'Analysts submit, admins review, and nobody approves their own report.' },
  { icon: ClockCounterClockwise, title: 'Audit trail in the same transaction', body: 'If the change is saved, the log entry is too. There is no endpoint to write one by hand.' },
]

export default function AuthPage({ mode }) {
  const isLogin = mode === 'login'
  const { user, login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ username: '', fullName: '', email: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={location.state?.from || '/'} replace />

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const fe = error?.fieldErrors ?? {}

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (isLogin) await login(form.email, form.password)
      else await register(form)
      navigate(location.state?.from || '/', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid min-h-[100dvh] lg:grid-cols-[1.1fr_1fr]">
      <section className="hidden flex-col justify-between bg-accent-strong p-12 text-white lg:flex dark:bg-accent-soft dark:text-ink">
        <span className="flex items-center gap-2 text-lg font-semibold">
          <img src="/favicon.svg" alt="" width="28" height="28" /> Ledgerline
        </span>
        <div className="max-w-md">
          <h1 className="text-4xl font-semibold leading-tight tracking-tight">Financial reports that explain their own risk.</h1>
          <ul className="mt-10 grid gap-7">
            {POINTS.map(({ icon: Icon, title, body }) => (
              <li key={title} className="flex gap-4">
                <Icon size={24} className="mt-0.5 shrink-0 opacity-90" aria-hidden />
                <div>
                  <p className="font-medium">{title}</p>
                  <p className="mt-1 text-sm opacity-80">{body}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
        <p className="text-sm opacity-80">Spring Boot, PostgreSQL and React. <a className="underline underline-offset-2 hover:opacity-100" href="https://github.com/wasdevv/financial-reports-service">Source on GitHub</a>.</p>
      </section>

      <section className="flex items-center justify-center px-4 py-12">
        <div className="fade-in w-full max-w-sm">
          <div className="mb-8 lg:hidden"><Logo /></div>
          <h2 className="text-2xl font-semibold tracking-tight">{isLogin ? 'Sign in' : 'Create your account'}</h2>
          <p className="mt-1 text-sm text-ink-2">
            {isLogin ? 'Use the account you registered with.' : 'New accounts start as analysts and can create reports right away.'}
          </p>

          <form onSubmit={onSubmit} className="mt-8 grid gap-4" noValidate>
            {!isLogin && (
              <>
                <Field id="fullName" label="Full name" autoComplete="name" required value={form.fullName} onChange={set('fullName')} error={fe.fullName} />
                <Field id="username" label="Username" autoComplete="username" required value={form.username} onChange={set('username')} error={fe.username} hint="Letters, numbers, dot, dash or underscore." />
              </>
            )}
            <Field id="email" label="Email" type="email" autoComplete="email" required value={form.email} onChange={set('email')} error={fe.email} />
            <Field id="password" label="Password" type="password" autoComplete={isLogin ? 'current-password' : 'new-password'} required value={form.password} onChange={set('password')} error={fe.password} hint={isLogin ? undefined : 'At least 8 characters.'} />
            {error && !Object.keys(fe).length && <ErrorBox error={error} />}
            <Button type="submit" disabled={busy} className="mt-2 w-full">
              {busy ? (isLogin ? 'Signing in...' : 'Creating account...') : isLogin ? 'Sign in' : 'Create account'}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-ink-2">
            {isLogin ? 'No account yet? ' : 'Already registered? '}
            <Link to={isLogin ? '/register' : '/login'} state={location.state} className="font-medium text-accent hover:underline">
              {isLogin ? 'Create one' : 'Sign in'}
            </Link>
          </p>
        </div>
      </section>
    </div>
  )
}
