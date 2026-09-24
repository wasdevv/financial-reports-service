package com.financialreports.risk;

import com.financialreports.entity.FinancialRecord;
import com.financialreports.entity.Report.RiskLevel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Score 0–100 derivado só dos lançamentos. Regras e limiares documentados em docs/api.md.
 * Função pura: sem banco, sem Spring, testável por valor.
 */
public final class RiskCalculator {

    public record Factor(String factor, int points, String explanation) {}

    public record Result(BigDecimal income, BigDecimal expense, int score, RiskLevel level, List<Factor> factors) {}

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal THIN = new BigDecimal("0.80");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal HALF = new BigDecimal("0.50");

    private RiskCalculator() {}

    public static Result assess(Collection<FinancialRecord> records) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (FinancialRecord r : records) {
            if (r.getType() == FinancialRecord.Type.INCOME) {
                income = income.add(r.getAmount());
            } else {
                expense = expense.add(r.getAmount());
                byCategory.merge(r.getCategory().trim().toLowerCase(), r.getAmount(), BigDecimal::add);
            }
        }

        List<Factor> factors = new ArrayList<>();
        if (expense.signum() > 0) {
            if (income.signum() == 0) {
                factors.add(new Factor("NO_INCOME", 70, "Expenses recorded with no income in the period"));
            } else {
                BigDecimal ratio = expense.divide(income, 4, RoundingMode.HALF_UP);
                String pct = ratio.movePointRight(2).setScale(0, RoundingMode.HALF_UP) + "%";
                if (ratio.compareTo(ONE) > 0) {
                    factors.add(new Factor("DEFICIT", 40, "Expenses are " + pct + " of income"));
                } else if (ratio.compareTo(THIN) > 0) {
                    factors.add(new Factor("THIN_MARGIN", 20, "Expenses are " + pct + " of income (above 80%)"));
                }
                if (ratio.compareTo(TWO) > 0) {
                    factors.add(new Factor("SEVERE_DEFICIT", 20, "Expenses exceed twice the income"));
                }
            }
            BigDecimal total = expense;
            byCategory.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .filter(top -> top.getValue().divide(total, 4, RoundingMode.HALF_UP).compareTo(HALF) > 0)
                    .ifPresent(top -> factors.add(new Factor("CONCENTRATION", 15,
                            "Category '" + top.getKey() + "' is more than half of all expenses")));
        }

        int score = Math.min(100, factors.stream().mapToInt(Factor::points).sum());
        return new Result(income, expense, score, level(score), factors);
    }

    public static RiskLevel level(int score) {
        if (score < 25) return RiskLevel.LOW;
        if (score < 50) return RiskLevel.MEDIUM;
        if (score < 75) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }
}
