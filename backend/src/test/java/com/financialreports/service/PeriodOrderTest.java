package com.financialreports.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PeriodOrderTest {

    @Test
    void monthsAndQuartersSortChronologically() {
        List<String> sorted = List.of("2026-Q3", "2026-04", "2025-12", "2026-Q1", "2026-01", "2026-07").stream()
                .sorted(AnalyticsService.CHRONOLOGICAL).toList();
        assertThat(sorted).containsExactly("2025-12", "2026-01", "2026-Q1", "2026-04", "2026-07", "2026-Q3");
    }
}
