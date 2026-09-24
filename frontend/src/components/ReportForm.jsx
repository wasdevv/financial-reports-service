import { useState } from 'react'
import { Button, ErrorBox, Field } from './ui'

export default function ReportForm({ initial, onSubmit, onCancel, submitLabel }) {
  const [form, setForm] = useState({ title: initial?.title ?? '', period: initial?.period ?? '', description: initial?.description ?? '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)
  const fe = error?.fieldErrors ?? {}
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onSubmit(form)
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="grid gap-4" noValidate>
      <Field id="title" label="Title" required maxLength={160} value={form.title} onChange={set('title')} error={fe.title} />
      <Field id="period" label="Period" required value={form.period} onChange={set('period')} error={fe.period}
        placeholder="2026-07 or 2026-Q3" hint="A month (YYYY-MM) or a quarter (YYYY-Q1 to Q4)." />
      <Field id="description" as="textarea" label="Description (optional)" maxLength={2000} value={form.description} onChange={set('description')} error={fe.description} />
      {error && !Object.keys(fe).length && <ErrorBox error={error} />}
      <div className="flex justify-end gap-2">
        <Button type="button" variant="ghost" onClick={onCancel}>Cancel</Button>
        <Button type="submit" disabled={busy}>{busy ? 'Saving...' : submitLabel}</Button>
      </div>
    </form>
  )
}
