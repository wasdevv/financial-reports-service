import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../lib/api'
import { useAsync } from '../lib/useAsync'
import { formatDateTime } from '../lib/format'
import { useAuth } from '../auth'
import { ErrorBox, PageHeader, Skeleton, StatusBadge, RiskBadge, Empty } from '../components/ui'

const ACTION_LABEL = {
  USER_REGISTERED: 'registered',
  USER_ROLE_CHANGED: 'changed a role',
  USER_ENABLED: 'enabled a user',
  USER_DISABLED: 'disabled a user',
  REPORT_CREATED: 'created a report',
  REPORT_UPDATED: 'edited a report',
  REPORT_DELETED: 'deleted a report',
  REPORT_SUBMITTED: 'submitted a report',
  REPORT_APPROVED: 'approved a report',
  REPORT_REJECTED: 'rejected a report',
  RECORD_ADDED: 'added a line',
  RECORD_DELETED: 'removed a line',
}

function Users() {
  const { user: me } = useAuth()
  const { data, error, loading, reload, setData } = useAsync(() => api.users(), [])
  const [rowError, setRowError] = useState(null)

  async function change(u, patch) {
    setRowError(null)
    try {
      const updated = await api.updateUser(u.id, patch)
      setData(data.map((x) => (x.id === u.id ? updated : x)))
    } catch (e) {
      setRowError(e)
    }
  }

  if (error) return <ErrorBox error={error} onRetry={reload} />
  if (loading) return <Skeleton className="h-48" />
  return (
    <>
      {rowError && <div className="mb-3"><ErrorBox error={rowError} /></div>}
      <ul className="divide-y divide-line rounded-xl border border-line bg-surface">
        {data.map((u) => (
          <li key={u.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{u.fullName} <span className="font-normal text-ink-3">@{u.username}</span></p>
              <p className="truncate text-xs text-ink-3">{u.email}</p>
            </div>
            {u.id === me.id ? <span className="text-xs text-ink-3">You</span> : (
              <>
                <label className="sr-only" htmlFor={`role-${u.id}`}>Role for {u.username}</label>
                <select id={`role-${u.id}`} value={u.role} onChange={(e) => change(u, { role: e.target.value })}
                  className="h-9 rounded-lg border border-line bg-surface px-2 text-sm">
                  <option value="USER">Viewer</option>
                  <option value="ANALYST">Analyst</option>
                  <option value="ADMIN">Admin</option>
                </select>
                <label className="flex items-center gap-2 text-sm text-ink-2">
                  <input type="checkbox" checked={u.active} onChange={(e) => change(u, { active: e.target.checked })} className="h-4 w-4 accent-[var(--color-accent)]" />
                  Active
                </label>
              </>
            )}
          </li>
        ))}
      </ul>
    </>
  )
}

function Queue() {
  const { data, error, loading, reload } = useAsync(() => api.reports({ status: 'PENDING', size: 50 }), [])
  if (error) return <ErrorBox error={error} onRetry={reload} />
  if (loading) return <Skeleton className="h-32" />
  if (data.content.length === 0) return <Empty title="Nothing to review">Submitted reports show up here.</Empty>
  return (
    <ul className="grid gap-2">
      {data.content.map((r) => (
        <li key={r.id}>
          <Link to={`/reports/${r.id}`} className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3 hover:border-ink-3">
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium">{r.title}</span>
              <span className="text-xs text-ink-3">{r.period} by {r.owner}</span>
            </span>
            <StatusBadge status={r.status} />
            <RiskBadge level={r.riskLevel} score={r.riskScore} />
          </Link>
        </li>
      ))}
    </ul>
  )
}

function AuditTrail() {
  const { data, error, loading, reload } = useAsync(() => api.auditLogs(), [])
  if (error) return <ErrorBox error={error} onRetry={reload} />
  if (loading) return <Skeleton className="h-64" />
  return (
    <ol className="grid gap-3">
      {data.content.map((a) => (
        <li key={a.id} className="grid gap-0.5 text-sm sm:grid-cols-[11rem_1fr]">
          <time className="font-mono text-xs text-ink-3 sm:pt-0.5">{formatDateTime(a.createdAt)}</time>
          <p>
            <span className="font-medium">{a.actor}</span> <span className="text-ink-2">{ACTION_LABEL[a.action] ?? a.action}</span>
            {a.entityType === 'REPORT' && a.action !== 'REPORT_DELETED' && <> <Link to={`/reports/${a.entityId}`} className="text-accent hover:underline">#{a.entityId}</Link></>}
            {a.details && <span className="block text-xs text-ink-3">{a.details}</span>}
          </p>
        </li>
      ))}
    </ol>
  )
}

export default function Admin() {
  return (
    <div className="fade-in">
      <PageHeader title="Admin">Review submitted reports, manage roles and follow the audit trail.</PageHeader>
      <div className="grid gap-10 lg:grid-cols-2">
        <div className="grid content-start gap-10">
          <section><h2 className="mb-3 font-medium">Review queue</h2><Queue /></section>
          <section><h2 className="mb-3 font-medium">Users</h2><Users /></section>
        </div>
        <section><h2 className="mb-3 font-medium">Audit trail</h2><AuditTrail /></section>
      </div>
    </div>
  )
}
