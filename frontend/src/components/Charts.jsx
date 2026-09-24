import { RISK } from './ui'
import { formatCompact, formatMoney } from '../lib/format'

const LEVELS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const RISK_BG = { LOW: 'bg-good', MEDIUM: 'bg-warning', HIGH: 'bg-serious', CRITICAL: 'bg-critical' }

// Receita x despesa por período: barras agrupadas, um eixo, legenda + tooltip por grupo + tabela.
export function IncomeExpenseChart({ points }) {
  const max = Math.max(1, ...points.flatMap((p) => [Number(p.income), Number(p.expense)]))
  const ticks = [1, 0.5, 0]
  return (
    <figure>
      <div className="mb-8 flex gap-4 text-sm text-ink-2" aria-hidden>
        <span className="flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-income" /> Income</span>
        <span className="flex items-center gap-2"><span className="h-2.5 w-2.5 rounded-sm bg-expense" /> Expense</span>
      </div>
      <div className="relative h-56 pl-12">
        {ticks.map((t) => (
          <div key={t} className="absolute inset-x-0 flex items-center" style={{ bottom: `${t * 100}%` }} aria-hidden>
            <span className="w-11 -translate-y-1/2 pr-2 text-right font-mono text-[11px] text-ink-3">{formatCompact(max * t)}</span>
            <span className={`h-px flex-1 ${t === 0 ? 'bg-ink-3/60' : 'bg-line'}`} />
          </div>
        ))}
        <div className="relative flex h-full items-end gap-2 pb-px md:gap-6">
          {points.map((p) => (
            <div
              key={p.period}
              tabIndex={0}
              className="group relative flex h-full min-w-0 flex-1 items-end justify-center gap-0.5 rounded-sm outline-offset-4 hover:bg-line/30 focus:bg-line/30"
              aria-label={`${p.period}: income ${formatMoney(p.income)}, expense ${formatMoney(p.expense)}`}
            >
              <span className="w-full max-w-5 rounded-t bg-income" style={{ height: `${(Number(p.income) / max) * 100}%` }} />
              <span className="w-full max-w-5 rounded-t bg-expense" style={{ height: `${(Number(p.expense) / max) * 100}%` }} />
              <div role="tooltip" className="pointer-events-none absolute bottom-full z-10 mb-2 hidden w-44 rounded-lg border border-line bg-surface p-3 text-xs shadow-lg shadow-ink/5 group-hover:block group-focus:block">
                <p className="mb-1 font-medium text-ink">{p.period}</p>
                <p className="flex justify-between text-ink-2"><span>Income</span><span className="font-mono text-ink">{formatMoney(p.income)}</span></p>
                <p className="flex justify-between text-ink-2"><span>Expense</span><span className="font-mono text-ink">{formatMoney(p.expense)}</span></p>
                <p className="flex justify-between text-ink-2"><span>Avg. risk</span><span className="font-mono text-ink">{p.averageRiskScore}</span></p>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="mt-2 flex gap-2 pl-12 md:gap-6" aria-hidden>
        {points.map((p) => <span key={p.period} className="min-w-0 flex-1 truncate text-center font-mono text-[10px] text-ink-3 sm:text-[11px]">{p.period}</span>)}
      </div>
      <details className="mt-4 text-sm">
        <summary className="cursor-pointer text-ink-2 hover:text-ink">Show as table</summary>
        <div className="relative mt-2 overflow-x-auto">
          <table className="w-full text-left">
            <thead className="text-ink-3"><tr><th className="py-1 font-medium">Period</th><th className="font-medium">Income</th><th className="font-medium">Expense</th><th className="font-medium">Avg. risk</th></tr></thead>
            <tbody className="font-mono text-xs">
              {points.map((p) => (
                <tr key={p.period} className="border-t border-line"><td className="py-1.5">{p.period}</td><td>{formatMoney(p.income)}</td><td>{formatMoney(p.expense)}</td><td>{p.averageRiskScore}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </figure>
  )
}

// Distribuição por nível: barra segmentada com gap de 2px; cor de status sempre com ícone + rótulo.
export function RiskDistribution({ counts }) {
  const total = LEVELS.reduce((s, l) => s + (counts[l] ?? 0), 0)
  return (
    <figure>
      <div className="flex h-3 gap-0.5 overflow-hidden rounded" role="img"
        aria-label={LEVELS.map((l) => `${RISK[l].label}: ${counts[l] ?? 0}`).join(', ')}>
        {total === 0 ? <span className="flex-1 bg-line" /> : LEVELS.filter((l) => counts[l]).map((l) => (
          <span key={l} className={RISK_BG[l]} style={{ flexGrow: counts[l] }} title={`${RISK[l].label}: ${counts[l]}`} />
        ))}
      </div>
      <ul className="mt-5 grid grid-cols-2 gap-x-6 gap-y-4">
        {LEVELS.map((l) => {
          const Icon = RISK[l].icon
          return (
            <li key={l} className="flex items-center gap-2">
              <Icon size={18} weight="fill" className={RISK[l].text} aria-hidden />
              <span className="text-sm text-ink-2">{RISK[l].label}</span>
              <span className="ml-auto font-mono text-sm">{counts[l] ?? 0}</span>
            </li>
          )
        })}
      </ul>
    </figure>
  )
}
