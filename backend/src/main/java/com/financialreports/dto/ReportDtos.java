package com.financialreports.dto;

import com.financialreports.entity.FinancialRecord;
import com.financialreports.entity.Report;
import com.financialreports.entity.RiskAssessment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ReportDtos {

    private ReportDtos() {}

    // Totais, score e dono nunca vêm do cliente: são derivados no servidor.
    public record ReportRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 2000) String description,
            @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2]|Q[1-4])$", message = "use YYYY-MM or YYYY-Q1..Q4")
            String period) {}

    public record RecordRequest(
            @NotNull LocalDate recordDate,
            @NotNull FinancialRecord.Type type,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @Size(max = 255) String description) {}

    public record ReviewRequest(@Size(max = 1000) String note) {}

    public record ReportSummary(
            Long id, String title, String period, String status, BigDecimal totalIncome, BigDecimal totalExpense,
            int riskScore, String riskLevel, String owner, Instant updatedAt) {
        public static ReportSummary from(Report r) {
            return new ReportSummary(r.getId(), r.getTitle(), r.getPeriod(), r.getStatus().name(), r.getTotalIncome(),
                    r.getTotalExpense(), r.getRiskScore(), r.getRiskLevel().name(), r.getOwner().getUsername(),
                    r.getUpdatedAt());
        }
    }

    public record RecordResponse(
            Long id, LocalDate recordDate, String type, String category, BigDecimal amount, String description) {
        public static RecordResponse from(FinancialRecord f) {
            return new RecordResponse(f.getId(), f.getRecordDate(), f.getType().name(), f.getCategory(), f.getAmount(),
                    f.getDescription());
        }
    }

    public record FactorResponse(String factor, int points, String explanation) {
        public static FactorResponse from(RiskAssessment a) {
            return new FactorResponse(a.getFactor(), a.getPoints(), a.getExplanation());
        }
    }

    public record Permissions(boolean edit, boolean submit, boolean review, boolean delete) {}

    public record ReportDetail(
            Long id, String title, String description, String period, String status, String reviewNote,
            BigDecimal totalIncome, BigDecimal totalExpense, int riskScore, String riskLevel, String owner,
            Instant createdAt, Instant updatedAt, List<RecordResponse> records, List<FactorResponse> riskFactors,
            Permissions can) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> from(Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
