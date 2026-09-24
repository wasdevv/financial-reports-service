import { useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { ChartLineUp, Files, ShieldCheck, SignOut, List, X } from '@phosphor-icons/react'
import { useAuth } from '../auth'

export function Logo() {
  return (
    <span className="flex items-center gap-2 font-semibold tracking-tight">
      <img src="/favicon.svg" alt="" width="24" height="24" />
      Ledgerline
    </span>
  )
}

export default function Layout() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const links = [
    { to: '/', label: 'Dashboard', icon: ChartLineUp, end: true },
    { to: '/reports', label: 'Reports', icon: Files },
    ...(user.role === 'ADMIN' ? [{ to: '/admin', label: 'Admin', icon: ShieldCheck }] : []),
  ]
  const linkClass = ({ isActive }) =>
    `flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-accent-soft text-accent' : 'text-ink-2 hover:bg-line/50 hover:text-ink'
    }`

  return (
    <div className="min-h-[100dvh]">
      <header className="sticky top-0 z-20 border-b border-line bg-page/90 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center gap-6 px-4 md:px-6">
          <Logo />
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main">
            {links.map(({ to, label, icon: Icon, end }) => (
              <NavLink key={to} to={to} end={end} className={linkClass}>
                <Icon size={18} aria-hidden /> {label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto hidden items-center gap-3 md:flex">
            <div className="text-right leading-tight">
              <p className="text-sm font-medium">{user.fullName}</p>
              <p className="text-xs text-ink-3 capitalize">{user.role.toLowerCase()}</p>
            </div>
            <button onClick={logout} className="rounded-lg p-2 text-ink-2 hover:bg-line/50 hover:text-ink" aria-label="Sign out">
              <SignOut size={20} />
            </button>
          </div>
          <button
            className="ml-auto rounded-lg p-2 md:hidden"
            onClick={() => setOpen((o) => !o)}
            aria-expanded={open}
            aria-controls="mobile-nav"
            aria-label={open ? 'Close menu' : 'Open menu'}
          >
            {open ? <X size={22} /> : <List size={22} />}
          </button>
        </div>
        {open && (
          <nav id="mobile-nav" className="border-t border-line px-4 py-3 md:hidden" aria-label="Main">
            <div className="grid gap-1">
              {links.map(({ to, label, icon: Icon, end }) => (
                <NavLink key={to} to={to} end={end} className={linkClass} onClick={() => setOpen(false)}>
                  <Icon size={18} aria-hidden /> {label}
                </NavLink>
              ))}
            </div>
            <div className="mt-3 flex items-center justify-between border-t border-line pt-3">
              <span className="text-sm">{user.fullName} <span className="text-ink-3">({user.role.toLowerCase()})</span></span>
              <button onClick={logout} className="flex items-center gap-1 text-sm text-ink-2"><SignOut size={18} aria-hidden /> Sign out</button>
            </div>
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8 md:px-6">
        <Outlet />
      </main>
    </div>
  )
}
