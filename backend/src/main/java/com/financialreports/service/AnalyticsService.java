package com.financialreports.service;

import com.financialreports.dto.AnalyticsResponse;
import com.financialreports.dto.AnalyticsResponse.PeriodPoint;
import com.financialreports.dto.ReportDtos.ReportSummary;
import com.financialreports.entity.Report;
import com.financialreports.entity.User;
import com.financialreports.repository.ReportRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    // "2026-Q1" começa em janeiro: ordena por (ano, mês inicial), mês antes do trimestre que o contém.
    static final Comparator<String> CHRONOLOGICAL = Comparator
            .comparing((String p) -> p.substring(0, 4))
            .thenComparingInt(p -> p.charAt(5) == 'Q' ? (p.charAt(6) - '1') * 3 + 1 : Integer.parseInt(p.substring(5)))
            .thenComparing(p -> p.charAt(5) == 'Q');

    private final ReportRepository reports;

    public AnalyticsService(ReportRepository reports) {
        this.reports = reports;
    }

    // ponytail: agrega em memória sobre os relatórios visíveis (sem lançamentos). Com dezenas de
    // milhares de relatórios por usuário, trocar por GROUP BY no banco.
    @Transactional(readOnly = true)
    public AnalyticsResponse summary(User user) {
        List<Report> visible = reports.findAll(ReportService.visibleTo(user));

        BigDecimal income = visible.stream().map(Report::getTotalIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = visible.stream().map(Report::getTotalExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        double avg = visible.stream().mapToInt(Report::getRiskScore).average().orElse(0);

        Map<String, Long> byStatus = zeroed(Report.Status.values());
        visible.forEach(r -> byStatus.merge(r.getStatus().name(), 1L, Long::sum));
        Map<String, Long> byRisk = zeroed(Report.RiskLevel.values());
        visible.forEach(r -> byRisk.merge(r.getRiskLevel().name(), 1L, Long::sum));

        List<PeriodPoint> byPeriod = visible.stream()
                .collect(Collectors.groupingBy(Report::getPeriod, () -> new TreeMap<>(CHRONOLOGICAL), Collectors.toList()))
                .entrySet().stream()
                .map(e -> new PeriodPoint(e.getKey(),
                        e.getValue().stream().map(Report::getTotalIncome).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().stream().map(Report::getTotalExpense).reduce(BigDecimal.ZERO, BigDecimal::add),
                        round(e.getValue().stream().mapToInt(Report::getRiskScore).average().orElse(0))))
                .toList();

        List<ReportSummary> top = visible.stream()
                .filter(r -> r.getRiskScore() > 0)
                .sorted(Comparator.comparingInt(Report::getRiskScore).reversed().thenComparing(Report::getId))
                .limit(5)
                .map(ReportSummary::from)
                .toList();

        return new AnalyticsResponse(visible.size(), income, expense, round(avg), byStatus, byRisk, byPeriod, top);
    }

    private static Map<String, Long> zeroed(Enum<?>[] values) {
        Map<String, Long> m = new LinkedHashMap<>();
        Arrays.stream(values).forEach(v -> m.put(v.name(), 0L));
        return m;
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
