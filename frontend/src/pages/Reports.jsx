import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { MagnifyingGlass, Plus } from '@phosphor-icons/react'
import { api } from '../lib/api'
import { useAsync } from '../lib/useAsync'
import { formatMoney, formatDateTime } from '../lib/format'
import { useAuth } from '../auth'
import { Button, Empty, ErrorBox, PageHeader, RiskBadge, Skeleton, StatusBadge } from '../components/ui'
import Modal from '../components/Modal'
import ReportForm from '../components/ReportForm'

const selectClass = 'h-10 rounded-lg border border-line bg-surface px-3 text-sm text-ink hover:border-ink-3'

export default function Reports() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const filters = {
    q: params.get('q') ?? '',
    status: params.get('status') ?? '',
    riskLevel: params.get('riskLevel') ?? '',
    mine: params.get('mine') === 'true',
    page: Number(params.get('page') ?? 0),
  }
  const creating = params.get('new') === '1'
  const keyParams = new URLSearchParams(params)
  keyParams.delete('new')
  const listKey = keyParams.toString() // abrir o modal não recarrega a lista
  const { data, error, loading, reload } = useAsync(() => api.reports({ ...filters, size: 20 }), [listKey])

  const update = (patch) => {
    const next = new URLSearchParams(params)
    Object.entries({ page: '', ...patch }).forEach(([k, v]) => (v === '' || v === false || v == null ? next.delete(k) : next.set(k, v)))
    setParams(next, { replace: true })
  }
  const filtered = filters.q || filters.status || filters.riskLevel || filters.mine

  return (
    <>
      <PageHeader
        title="Reports"
        actions={user.role !== 'USER' && <Button onClick={() => update({ new: '1' })}><Plus size={16} weight="bold" aria-hidden /> New report</Button>}
      >
        {user.role === 'ADMIN' ? 'Every report in the system.' : 'Your reports plus every approved report.'}
      </PageHeader>

      <form className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_auto_auto_auto]" onSubmit={(e) => { e.preventDefault(); update({ q }) }} role="search">
        <label className="relative">
          <span className="sr-only">Search by title</span>
          <MagnifyingGlass size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" aria-hidden />
          <input value={q} onChange={(e) => setQ(e.target.value)} onBlur={() => q !== filters.q && update({ q })} placeholder="Search by title" className={`${selectClass} w-full pl-9`} />
        </label>
        <label>
          <span className="sr-only">Status</span>
          <select value={filters.status} onChange={(e) => update({ status: e.target.value })} className={`${selectClass} w-full`}>
            <option value="">Any status</option>
            {['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'].map((s) => <option key={s} value={s}>{s[0] + s.slice(1).toLowerCase()}</option>)}
          </select>
        </label>
        <label>
          <span className="sr-only">Risk level</span>
          <select value={filters.riskLevel} onChange={(e) => update({ riskLevel: e.target.value })} className={`${selectClass} w-full`}>
            <option value="">Any risk</option>
            {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((s) => <option key={s} value={s}>{s[0] + s.slice(1).toLowerCase()}</option>)}
          </select>
        </label>
        <label className="flex h-10 items-center gap-2 text-sm text-ink-2">
          <input type="checkbox" checked={filters.mine} onChange={(e) => update({ mine: e.target.checked })} className="h-4 w-4 accent-[var(--color-accent)]" />
          Only mine
        </label>
      </form>

      {error ? <ErrorBox error={error} onRetry={reload} />
        : loading ? <div className="grid gap-2">{[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-14" />)}</div>
        : data.content.length === 0 ? (
          <Empty title={filtered ? 'No report matches these filters' : 'No reports yet'}
            action={filtered ? <Button variant="secondary" onClick={() => { setQ(''); setParams({}) }}>Clear filters</Button> : null}>
            {filtered ? 'Try a different search or clear the filters.' : 'Create one to start tracking income, expenses and risk.'}
          </Empty>
        ) : (
          <div className="fade-in">
            {/* Tabela no desktop, cartões no mobile */}
            <div className="hidden overflow-hidden rounded-xl border border-line bg-surface md:block">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line text-ink-3">
                  <tr>
                    <th className="px-4 py-3 font-medium">Report</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 text-right font-medium">Income</th>
                    <th className="px-4 py-3 text-right font-medium">Expense</th>
                    <th className="px-4 py-3 font-medium">Risk</th>
                    <th className="px-4 py-3 font-medium">Updated</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {data.content.map((r) => (
                    <tr key={r.id} className="cursor-pointer hover:bg-line/30" onClick={() => navigate(`/reports/${r.id}`)}>
                      <td className="px-4 py-3">
                        <Link to={`/reports/${r.id}`} className="font-medium hover:text-accent" onClick={(e) => e.stopPropagation()}>{r.title}</Link>
                        <p className="text-xs text-ink-3">{r.period} by {r.owner}</p>
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={r.status} /></td>
                      <td className="px-4 py-3 text-right font-mono">{formatMoney(r.totalIncome)}</td>
                      <td className="px-4 py-3 text-right font-mono">{formatMoney(r.totalExpense)}</td>
                      <td className="px-4 py-3"><RiskBadge level={r.riskLevel} score={r.riskScore} /></td>
                      <td className="px-4 py-3 text-ink-2">{formatDateTime(r.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <ul className="grid gap-3 md:hidden">
              {data.content.map((r) => (
                <li key={r.id}>
                  <Link to={`/reports/${r.id}`} className="block rounded-xl border border-line bg-surface p-4 active:scale-[0.99]">
                    <div className="flex items-start justify-between gap-3">
                      <p className="font-medium">{r.title}</p>
                      <StatusBadge status={r.status} />
                    </div>
                    <p className="mt-0.5 text-xs text-ink-3">{r.period} by {r.owner}</p>
                    <div className="mt-3 flex items-end justify-between">
                      <div className="font-mono text-sm">
                        <p><span className="text-ink-3">In </span>{formatMoney(r.totalIncome)}</p>
                        <p><span className="text-ink-3">Out </span>{formatMoney(r.totalExpense)}</p>
                      </div>
                      <RiskBadge level={r.riskLevel} score={r.riskScore} />
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
            {data.totalPages > 1 && (
              <nav className="mt-6 flex items-center justify-between text-sm" aria-label="Pagination">
                <Button variant="secondary" disabled={data.page === 0} onClick={() => update({ page: data.page - 1 })}>Previous</Button>
                <span className="text-ink-2">Page {data.page + 1} of {data.totalPages}</span>
                <Button variant="secondary" disabled={data.page + 1 >= data.totalPages} onClick={() => update({ page: data.page + 1 })}>Next</Button>
              </nav>
            )}
          </div>
        )}

      {creating && (
        <Modal title="New report" onClose={() => update({ new: '' })}>
          <ReportForm submitLabel="Create report" onCancel={() => update({ new: '' })}
            onSubmit={async (form) => { const r = await api.createReport(form); navigate(`/reports/${r.id}`) }} />
        </Modal>
      )}
    </>
  )
}
