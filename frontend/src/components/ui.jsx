import { CheckCircle, Warning, WarningOctagon, Info, SealWarning } from '@phosphor-icons/react'

const cx = (...c) => c.filter(Boolean).join(' ')

export function Button({ variant = 'primary', className, ...props }) {
  const styles = {
    primary: 'bg-accent text-white hover:bg-accent-strong dark:text-zinc-950',
    secondary: 'border border-line bg-surface text-ink hover:border-ink-3',
    danger: 'border border-critical/40 text-critical hover:bg-danger-soft',
    ghost: 'text-ink-2 hover:text-ink hover:bg-line/50',
  }
  return (
    <button
      className={cx(
        'inline-flex h-10 items-center justify-center gap-2 whitespace-nowrap rounded-lg px-4 text-sm font-medium transition active:translate-y-px disabled:pointer-events-none disabled:opacity-50',
        styles[variant],
        className,
      )}
      {...props}
    />
  )
}

export function Field({ label, error, hint, id, as = 'input', className, ...props }) {
  const Tag = as
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={cx('grid gap-1.5', className)}>
      <label htmlFor={id} className="text-sm font-medium text-ink">{label}</label>
      <Tag
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className={cx(
          'w-full rounded-lg border bg-surface px-3 text-sm text-ink placeholder:text-ink-3',
          as === 'textarea' ? 'min-h-20 py-2' : 'h-10',
          error ? 'border-critical' : 'border-line hover:border-ink-3',
        )}
        {...props}
      />
      {error ? <p id={`${id}-error`} className="text-sm text-critical">{error}</p>
        : hint ? <p id={`${id}-hint`} className="text-xs text-ink-3">{hint}</p> : null}
    </div>
  )
}

export const RISK = {
  LOW: { label: 'Low', text: 'text-good', icon: CheckCircle },
  MEDIUM: { label: 'Medium', text: 'text-warning', icon: Info },
  HIGH: { label: 'High', text: 'text-serious', icon: Warning },
  CRITICAL: { label: 'Critical', text: 'text-critical', icon: WarningOctagon },
}

export function RiskBadge({ level, score }) {
  const r = RISK[level] ?? RISK.LOW
  const Icon = r.icon
  return (
    <span className={cx('inline-flex items-center gap-1.5 text-sm font-medium', r.text)}>
      <Icon size={16} weight="fill" aria-hidden />
      {r.label}
      {score != null && <span className="font-mono text-xs text-ink-3">{score}</span>}
    </span>
  )
}

const STATUS = {
  DRAFT: 'bg-line/60 text-ink-2',
  PENDING: 'bg-accent-soft text-accent',
  APPROVED: 'bg-good/10 text-good',
  REJECTED: 'bg-critical/10 text-critical',
}

export function StatusBadge({ status }) {
  return (
    <span className={cx('inline-flex rounded-md px-2 py-0.5 text-xs font-medium capitalize', STATUS[status])}>
      {status?.toLowerCase()}
    </span>
  )
}

export function Skeleton({ className }) {
  return <div className={cx('animate-pulse rounded-lg bg-line/60 motion-reduce:animate-none', className)} aria-hidden />
}

export function ErrorBox({ error, onRetry }) {
  return (
    <div role="alert" className="flex flex-wrap items-center gap-3 rounded-lg border border-critical/30 bg-danger-soft px-4 py-3 text-sm text-critical">
      <SealWarning size={18} aria-hidden />
      <span className="flex-1">{error?.message ?? String(error)}</span>
      {onRetry && <Button variant="secondary" className="h-8" onClick={onRetry}>Retry</Button>}
    </div>
  )
}

export function Empty({ title, children, action }) {
  return (
    <div className="rounded-xl border border-dashed border-line px-6 py-12 text-center">
      <p className="font-medium">{title}</p>
      {children && <p className="mx-auto mt-1 max-w-sm text-sm text-ink-2">{children}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function PageHeader({ title, children, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight md:text-3xl">{title}</h1>
        {children && <p className="mt-1 max-w-[65ch] text-sm text-ink-2">{children}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}
