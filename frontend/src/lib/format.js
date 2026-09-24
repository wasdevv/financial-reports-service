const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const compact = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', notation: 'compact', maximumFractionDigits: 1 })
const dateTime = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' })
const date = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeZone: 'UTC' })

// BigDecimal chega como número JSON; Number basta para exibir centavos até 2^53.
export const formatMoney = (v) => money.format(Number(v ?? 0))
export const formatCompact = (v) => compact.format(Number(v ?? 0))
export const formatDateTime = (v) => (v ? dateTime.format(new Date(v)) : '')
export const formatDate = (v) => (v ? date.format(new Date(`${v}T00:00:00Z`)) : '')
export const net = (income, expense) => Number(income ?? 0) - Number(expense ?? 0)
