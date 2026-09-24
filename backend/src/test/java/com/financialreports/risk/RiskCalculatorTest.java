package com.financialreports.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.financialreports.entity.FinancialRecord;
import com.financialreports.entity.Report.RiskLevel;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskCalculatorTest {

    private static FinancialRecord rec(FinancialRecord.Type type, String category, String amount) {
        FinancialRecord f = new FinancialRecord();
        f.setType(type);
        f.setCategory(category);
        f.setAmount(new BigDecimal(amount));
        return f;
    }

    private static FinancialRecord in(String amount) {
        return rec(FinancialRecord.Type.INCOME, "Sales", amount);
    }

    private static FinancialRecord out(String category, String amount) {
        return rec(FinancialRecord.Type.EXPENSE, category, amount);
    }

    private static List<String> factors(RiskCalculator.Result r) {
        return r.factors().stream().map(RiskCalculator.Factor::factor).toList();
    }

    @Test
    void noRecordsIsZeroRisk() {
        var r = RiskCalculator.assess(List.of());
        assertThat(r.score()).isZero();
        assertThat(r.level()).isEqualTo(RiskLevel.LOW);
        assertThat(r.income()).isEqualByComparingTo("0");
    }

    @Test
    void incomeOnlyIsZeroRisk() {
        assertThat(RiskCalculator.assess(List.of(in("100"))).score()).isZero();
    }

    @Test
    void healthyDiversifiedIsLow() {
        var r = RiskCalculator.assess(List.of(in("1000"), out("Rent", "300"), out("Payroll", "300")));
        assertThat(r.score()).isZero();
        assertThat(r.expense()).isEqualByComparingTo("600");
    }

    @Test
    void expensesWithoutIncomeNeverDivideByZero() {
        var r = RiskCalculator.assess(List.of(out("Ads", "10"), out("Fees", "10")));
        assertThat(factors(r)).containsExactly("NO_INCOME");
        assertThat(r.score()).isEqualTo(70);
        assertThat(r.level()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void ratioBoundaries() {
        // exatamente 80%: não é margem apertada; exatamente 100%: margem apertada, não déficit
        assertThat(factors(RiskCalculator.assess(List.of(in("100"), out("A", "40"), out("B", "40"))))).isEmpty();
        assertThat(factors(RiskCalculator.assess(List.of(in("100"), out("A", "50"), out("B", "50")))))
                .containsExactly("THIN_MARGIN");
        assertThat(factors(RiskCalculator.assess(List.of(in("100"), out("A", "34"), out("B", "33"), out("C", "33.01")))))
                .containsExactly("DEFICIT");
        // exatamente 2x: déficit, mas não severo
        assertThat(factors(RiskCalculator.assess(List.of(in("100"), out("A", "100"), out("B", "100")))))
                .containsExactly("DEFICIT");
    }

    @Test
    void everythingAtOnceIsCriticalAndCapped() {
        var r = RiskCalculator.assess(List.of(in("100"), out("Fuel", "300")));
        assertThat(factors(r)).containsExactly("DEFICIT", "SEVERE_DEFICIT", "CONCENTRATION");
        assertThat(r.score()).isEqualTo(75);
        assertThat(r.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(RiskCalculator.assess(List.of(out("Fuel", "1"))).score()).isEqualTo(85);
    }

    @Test
    void concentrationGroupsCategoriesIgnoringCase() {
        var r = RiskCalculator.assess(List.of(in("1000"), out("Fuel", "200"), out(" fuel ", "200"), out("Rent", "300")));
        assertThat(factors(r)).containsExactly("CONCENTRATION");
    }

    @ParameterizedTest
    @CsvSource({"0,LOW", "24,LOW", "25,MEDIUM", "49,MEDIUM", "50,HIGH", "74,HIGH", "75,CRITICAL", "100,CRITICAL"})
    void levelThresholds(int score, RiskLevel level) {
        assertThat(RiskCalculator.level(score)).isEqualTo(level);
    }
}
