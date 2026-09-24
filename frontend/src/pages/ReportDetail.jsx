import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ArrowLeft, Trash, PaperPlaneTilt, Check, X, PencilSimple } from '@phosphor-icons/react'
import { api } from '../lib/api'
import { useAsync } from '../lib/useAsync'
import { formatDate, formatDateTime, formatMoney, net } from '../lib/format'
import { Button, Empty, ErrorBox, Field, RiskBadge, Skeleton, StatusBadge } from '../components/ui'
import Modal from '../components/Modal'
import ReportForm from '../components/ReportForm'

const today = () => new Date().toISOString().slice(0, 10)

function RecordForm({ onAdd }) {
  const empty = { recordDate: today(), type: 'EXPENSE', category: '', amount: '', description: '' }
  const [form, setForm] = useState(empty)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const fe = error?.fieldErrors ?? {}
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onAdd({ ...form, amount: form.amount === '' ? null : form.amount, description: form.description || null })
      setForm({ ...empty, recordDate: form.recordDate, type: form.type })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="grid gap-3 border-t border-line pt-5 sm:grid-cols-2 lg:grid-cols-[auto_auto_1fr_10rem_auto] lg:items-start" noValidate>
      <Field id="recordDate" label="Date" type="date" value={form.recordDate} onChange={set('recordDate')} error={fe.recordDate} />
      <div className="grid gap-1.5">
        <label htmlFor="type" className="text-sm font-medium">Type</label>
        <select id="type" value={form.type} onChange={set('type')} className="h-10 rounded-lg border border-line bg-surface px-3 text-sm">
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
      </div>
      <Field id="category" label="Category" value={form.category} onChange={set('category')} error={fe.category} maxLength={60} placeholder="Payroll, Rent, Sales" />
      <Field id="amount" label="Amount (USD)" type="number" inputMode="decimal" min="0.01" step="0.01" value={form.amount} onChange={set('amount')} error={fe.amount} />
      <div className="grid gap-1.5 sm:col-span-2 lg:col-span-1">
        <span className="hidden text-sm lg:block" aria-hidden>&nbsp;</span>
        <Button type="submit" disabled={busy}>{busy ? 'Adding...' : 'Add line'}</Button>
      </div>
      {error && !Object.keys(fe).length && <div className="sm:col-span-2 lg:col-span-5"><ErrorBox error={error} /></div>}
    </form>
  )
}

export default function ReportDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { data: r, error, loading, reload, setData } = useAsync(() => api.report(id), [id])
  const [actionError, setActionError] = useState(null)
  const [modal, setModal] = useState(null) // 'edit' | 'reject' | 'delete'
  const [note, setNote] = useState('')

  async function act(fn) {
    setActionError(null)
    try {
      const next = await fn()
      if (next) setData(next)
      setModal(null)
    } catch (err) {
      setActionError(err)
      if (err.status === 409) reload()
    }
  }

  if (error) {
    return (
      <>
        <Link to="/reports" className="mb-6 inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink"><ArrowLeft size={14} /> Reports</Link>
        {error.status === 404 ? <Empty title="Report not found">It doesn't exist or you don't have access to it.</Empty> : <ErrorBox error={error} onRetry={reload} />}
      </>
    )
  }
  if (loading) return <div className="grid gap-4"><Skeleton className="h-10 w-1/2" /><Skeleton className="h-28" /><Skeleton className="h-64" /></div>

  const result = net(r.totalIncome, r.totalExpense)
  return (
    <div className="fade-in">
      <Link to="/reports" className="mb-6 inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink"><ArrowLeft size={14} aria-hidden /> Reports</Link>

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight md:text-3xl">{r.title}</h1>
            <StatusBadge status={r.status} />
          </div>
          <p className="mt-1 text-sm text-ink-2">{r.period} by {r.owner}, updated {formatDateTime(r.updatedAt)}</p>
          {r.description && <p className="mt-3 max-w-[65ch] text-sm text-ink-2">{r.description}</p>}
        </div>
        <div className="flex flex-wrap gap-2">
          {r.can.edit && <Button variant="secondary" onClick={() => setModal('edit')}><PencilSimple size={16} aria-hidden /> Edit</Button>}
          {r.can.submit && <Button onClick={() => act(() => api.submit(r.id))}><PaperPlaneTilt size={16} aria-hidden /> Submit for review</Button>}
          {r.can.review && <Button onClick={() => act(() => api.approve(r.id, null))}><Check size={16} weight="bold" aria-hidden /> Approve</Button>}
          {r.can.review && <Button variant="danger" onClick={() => { setNote(''); setModal('reject') }}><X size={16} weight="bold" aria-hidden /> Reject</Button>}
          {r.can.delete && <Button variant="ghost" onClick={() => setModal('delete')} aria-label="Delete report"><Trash size={18} /></Button>}
        </div>
      </div>

      {r.status === 'REJECTED' && r.reviewNote && (
        <div className="mt-6 rounded-lg border border-critical/30 bg-danger-soft px-4 py-3 text-sm">
          <p className="font-medium text-critical">Rejected by the reviewer</p>
          <p className="mt-1 text-ink">{r.reviewNote}</p>
          <p className="mt-1 text-ink-2">Fix the lines and submit again.</p>
        </div>
      )}
      {r.status === 'PENDING' && !r.can.review && <p className="mt-6 text-sm text-ink-2">Waiting for an admin to review. Lines are locked until then.</p>}
      {actionError && <div className="mt-6"><ErrorBox error={actionError} /></div>}

      <section className="mt-8 grid gap-6 border-y border-line py-6 sm:grid-cols-2 lg:grid-cols-4" aria-label="Totals">
        <div><p className="text-sm text-ink-2">Income</p><p className="mt-1 font-mono text-xl">{formatMoney(r.totalIncome)}</p></div>
        <div><p className="text-sm text-ink-2">Expense</p><p className="mt-1 font-mono text-xl">{formatMoney(r.totalExpense)}</p></div>
        <div><p className="text-sm text-ink-2">Net result</p><p className={`mt-1 font-mono text-xl ${result < 0 ? 'text-critical' : ''}`}>{formatMoney(result)}</p></div>
        <div><p className="text-sm text-ink-2">Risk score</p><p className="mt-1 flex items-center gap-3"><span className="font-mono text-xl">{r.riskScore}</span><RiskBadge level={r.riskLevel} /></p></div>
      </section>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_20rem]">
        <section className="min-w-0">
          <h2 className="mb-4 font-medium">Lines</h2>
          {r.records.length === 0 ? (
            <Empty title="No lines yet">{r.can.edit ? 'Add income and expenses below. The risk score updates with each line.' : 'This report has no lines.'}</Empty>
          ) : (
            <div className="relative overflow-x-auto">
              <table className="w-full min-w-[32rem] text-left text-sm">
                <thead className="text-ink-3">
                  <tr><th className="py-2 font-medium">Date</th><th className="font-medium">Category</th><th className="font-medium">Type</th><th className="text-right font-medium">Amount</th>{r.can.edit && <th><span className="sr-only">Actions</span></th>}</tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {r.records.map((rec) => (
                    <tr key={rec.id}>
                      <td className="py-2.5 text-ink-2">{formatDate(rec.recordDate)}</td>
                      <td>{rec.category}{rec.description && <span className="block text-xs text-ink-3">{rec.description}</span>}</td>
                      <td className={rec.type === 'INCOME' ? 'text-income' : 'text-expense'}>{rec.type === 'INCOME' ? 'Income' : 'Expense'}</td>
                      <td className="text-right font-mono">{rec.type === 'EXPENSE' ? '-' : ''}{formatMoney(rec.amount)}</td>
                      {r.can.edit && (
                        <td className="w-10 text-right">
                          <button onClick={() => act(() => api.deleteRecord(r.id, rec.id))} className="rounded-lg p-1.5 text-ink-3 hover:bg-line/50 hover:text-critical" aria-label={`Delete ${rec.category} line`}><Trash size={16} /></button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {r.can.edit && <div className="mt-6"><RecordForm onAdd={async (data) => setData(await api.addRecord(r.id, data))} /></div>}
        </section>

        <aside>
          <h2 className="mb-4 font-medium">Why this score</h2>
          {r.riskFactors.length === 0 ? (
            <p className="text-sm text-ink-2">No risk factor triggered. Expenses stay under 80% of income and no category dominates.</p>
          ) : (
            <ul className="grid gap-4">
              {r.riskFactors.map((f) => (
                <li key={f.factor} className="flex gap-3">
                  <span className="mt-0.5 font-mono text-sm text-serious">+{f.points}</span>
                  <span className="text-sm">{f.explanation}</span>
                </li>
              ))}
            </ul>
          )}
          <p className="mt-6 text-xs text-ink-3">Score is capped at 100. Low below 25, medium below 50, high below 75, critical from 75.</p>
        </aside>
      </div>

      {modal === 'edit' && (
        <Modal title="Edit report" onClose={() => setModal(null)}>
          <ReportForm initial={r} submitLabel="Save changes" onCancel={() => setModal(null)}
            onSubmit={async (form) => { setData(await api.updateReport(r.id, form)); setModal(null) }} />
        </Modal>
      )}
      {modal === 'reject' && (
        <Modal title="Reject report" onClose={() => setModal(null)}>
          <form className="grid gap-4" onSubmit={(e) => { e.preventDefault(); act(() => api.reject(r.id, note)) }}>
            <Field id="note" as="textarea" label="What needs to change?" required value={note} onChange={(e) => setNote(e.target.value)} hint="The owner sees this note on the report." />
            {actionError && <ErrorBox error={actionError} />}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={() => setModal(null)}>Cancel</Button>
              <Button type="submit" variant="danger" disabled={!note.trim()}>Reject</Button>
            </div>
          </form>
        </Modal>
      )}
      {modal === 'delete' && (
        <Modal title="Delete this report?" onClose={() => setModal(null)}>
          <p className="text-sm text-ink-2">"{r.title}" and its {r.records.length} lines will be removed. The audit trail keeps a record of the deletion.</p>
          {actionError && <div className="mt-4"><ErrorBox error={actionError} /></div>}
          <div className="mt-6 flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setModal(null)}>Cancel</Button>
            <Button variant="danger" onClick={() => act(async () => { await api.deleteReport(r.id); navigate('/reports', { replace: true }) })}>Delete report</Button>
          </div>
        </Modal>
      )}
    </div>
  )
}
