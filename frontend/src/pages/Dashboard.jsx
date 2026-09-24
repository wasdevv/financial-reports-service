import { Link } from 'react-router'
import { ArrowRight } from '@phosphor-icons/react'
import { api } from '../lib/api'
import { useAsync } from '../lib/useAsync'
import { formatMoney, net } from '../lib/format'
import { useAuth } from '../auth'
import { Empty, ErrorBox, PageHeader, RiskBadge, Skeleton, StatusBadge, Button } from '../components/ui'
import { IncomeExpenseChart, RiskDistribution } from '../components/Charts'

function Stat({ label, value, sub }) {
  return (
    <div className="min-w-0 border-l-2 border-line pl-4">
      <p className="text-sm text-ink-2">{label}</p>
      <p className="mt-1 truncate font-mono text-lg font-medium tracking-tight sm:text-2xl lg:text-3xl">{value}</p>
      {sub && <p className="mt-0.5 text-xs text-ink-3">{sub}</p>}
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const { data, error, loading, reload } = useAsync(() => api.summary(), [])
  const scope = user.role === 'ADMIN' ? 'All reports in the system.' : 'Your reports plus every approved report.'

  if (error) return <><PageHeader title="Dashboard" /><ErrorBox error={error} onRetry={reload} /></>
  if (loading) {
    return (
      <>
        <PageHeader title="Dashboard">{scope}</PageHeader>
        <div className="grid grid-cols-2 gap-6 lg:grid-cols-4">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-16" />)}</div>
        <div className="mt-8 grid gap-6 lg:grid-cols-[2fr_1fr]"><Skeleton className="h-80" /><Skeleton className="h-80" /></div>
      </>
    )
  }
  if (data.totalReports === 0) {
    return (
      <>
        <PageHeader title="Dashboard">{scope}</PageHeader>
        <Empty title="No reports yet" action={user.role !== 'USER' && <Link to="/reports?new=1"><Button>New report</Button></Link>}>
          Create a report, add income and expense lines, and the risk score shows up here.
        </Empty>
      </>
    )
  }

  const result = net(data.totalIncome, data.totalExpense)
  return (
    <div className="fade-in">
      <PageHeader title="Dashboard">{scope}</PageHeader>

      <section className="grid grid-cols-2 gap-6 lg:grid-cols-4" aria-label="Totals">
        <Stat label="Reports" value={data.totalReports} sub={`${data.byStatus.PENDING} awaiting review`} />
        <Stat label="Income" value={formatMoney(data.totalIncome)} />
        <Stat label="Net result" value={formatMoney(result)} sub={result < 0 ? 'Expenses exceed income' : 'Income covers expenses'} />
        <Stat label="Average risk" value={data.averageRiskScore} sub="Score from 0 to 100" />
      </section>

      <div className="mt-10 grid gap-6 lg:grid-cols-[2fr_1fr]">
        <section className="min-w-0 rounded-xl border border-line bg-surface p-5 md:p-6">
          <h2 className="mb-4 font-medium">Income and expense by period</h2>
          <IncomeExpenseChart points={data.byPeriod} />
        </section>
        <section className="min-w-0 rounded-xl border border-line bg-surface p-5 md:p-6">
          <h2 className="mb-5 font-medium">Risk distribution</h2>
          <RiskDistribution counts={data.byRiskLevel} />
          <h3 className="mt-8 mb-3 text-sm font-medium text-ink-2">Highest risk</h3>
          {data.topRisks.length === 0 ? <p className="text-sm text-ink-3">No report carries risk right now.</p> : (
            <ul className="grid gap-1">
              {data.topRisks.map((r) => (
                <li key={r.id}>
                  <Link to={`/reports/${r.id}`} className="group -mx-2 flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-line/40">
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-sm font-medium">{r.title}</span>
                      <span className="mt-0.5 flex items-center gap-2"><StatusBadge status={r.status} /><span className="text-xs text-ink-3">{r.period}</span></span>
                    </span>
                    <RiskBadge level={r.riskLevel} score={r.riskScore} />
                    <ArrowRight size={14} className="text-ink-3 transition group-hover:translate-x-0.5" aria-hidden />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
