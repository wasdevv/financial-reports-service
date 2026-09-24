package com.financialreports.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        long totalReports,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        double averageRiskScore,
        Map<String, Long> byStatus,
        Map<String, Long> byRiskLevel,
        List<PeriodPoint> byPeriod,
        List<ReportDtos.ReportSummary> topRisks) {

    public record PeriodPoint(String period, BigDecimal income, BigDecimal expense, double averageRiskScore) {}
}
