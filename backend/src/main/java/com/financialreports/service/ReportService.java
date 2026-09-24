package com.financialreports.service;

import com.financialreports.dto.ReportDtos.*;
import com.financialreports.entity.FinancialRecord;
import com.financialreports.entity.Report;
import com.financialreports.entity.Report.Status;
import com.financialreports.entity.RiskAssessment;
import com.financialreports.entity.User;
import com.financialreports.exception.ApiException;
import com.financialreports.repository.ReportRepository;
import com.financialreports.risk.RiskCalculator;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de acesso (docs/api.md):
 * - ADMIN vê tudo; os demais veem os próprios relatórios e os APPROVED de qualquer um.
 * - Relatório invisível responde 404 (não vaza que o id existe); visível mas sem permissão, 403.
 * - Só o dono edita, e só em DRAFT/REJECTED. Só ADMIN revisa, e nunca o próprio relatório.
 */
@Service
public class ReportService {

    private final ReportRepository reports;
    private final AuditService audit;

    public ReportService(ReportRepository reports, AuditService audit) {
        this.reports = reports;
        this.audit = audit;
    }

    static Specification<Report> visibleTo(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> cb.or(
                cb.equal(root.get("owner").get("id"), user.getId()),
                cb.equal(root.get("status"), Status.APPROVED));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportSummary> list(User user, Status status, Report.RiskLevel risk, String q, boolean mine,
                                            Pageable pageable) {
        Specification<Report> spec = visibleTo(user);
        if (mine) spec = spec.and((root, cq, cb) -> cb.equal(root.get("owner").get("id"), user.getId()));
        if (status != null) spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        if (risk != null) spec = spec.and((root, cq, cb) -> cb.equal(root.get("riskLevel"), risk));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("title")), like, '\\'));
        }
        return PageResponse.from(reports.findAll(spec, pageable).map(ReportSummary::from));
    }

    @Transactional(readOnly = true)
    public ReportDetail get(User user, Long id) {
        return detail(user, visible(user, id));
    }

    @Transactional
    public ReportDetail create(User user, ReportRequest req) {
        if (user.getRole() == User.Role.USER) {
            throw ApiException.forbidden("Viewers can't create reports");
        }
        Report r = new Report();
        r.setOwner(user);
        apply(r, req);
        reports.saveAndFlush(r);
        audit.record(user, "REPORT_CREATED", "REPORT", r.getId(), r.getTitle());
        return detail(user, r);
    }

    @Transactional
    public ReportDetail update(User user, Long id, ReportRequest req) {
        Report r = editable(user, id);
        apply(r, req);
        r.setStatus(Status.DRAFT);
        audit.record(user, "REPORT_UPDATED", "REPORT", id, r.getTitle());
        return detail(user, reports.saveAndFlush(r));
    }

    @Transactional
    public void delete(User user, Long id) {
        Report r = visible(user, id);
        if (!isAdmin(user) && !(isOwner(user, r) && r.isEditable())) {
            throw ApiException.forbidden("Only drafts or rejected reports can be deleted by their owner");
        }
        reports.delete(r);
        audit.record(user, "REPORT_DELETED", "REPORT", id, r.getTitle());
    }

    @Transactional
    public ReportDetail addRecord(User user, Long id, RecordRequest req) {
        Report r = editable(user, id);
        FinancialRecord f = new FinancialRecord();
        f.setReport(r);
        f.setRecordDate(req.recordDate());
        f.setType(req.type());
        f.setCategory(req.category().trim());
        f.setAmount(req.amount());
        f.setDescription(req.description());
        r.getRecords().add(f);
        recalculate(r);
        audit.record(user, "RECORD_ADDED", "REPORT", id, req.type() + " " + req.amount() + " " + f.getCategory());
        return detail(user, reports.saveAndFlush(r));
    }

    @Transactional
    public ReportDetail deleteRecord(User user, Long id, Long recordId) {
        Report r = editable(user, id);
        FinancialRecord f = r.getRecords().stream()
                .filter(x -> x.getId().equals(recordId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Record"));
        r.getRecords().remove(f);
        recalculate(r);
        audit.record(user, "RECORD_DELETED", "REPORT", id, f.getType() + " " + f.getAmount() + " " + f.getCategory());
        return detail(user, reports.saveAndFlush(r));
    }

    @Transactional
    public ReportDetail submit(User user, Long id) {
        Report r = editable(user, id);
        if (r.getRecords().isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Add at least one record before submitting");
        }
        r.setStatus(Status.PENDING);
        r.setReviewNote(null);
        audit.record(user, "REPORT_SUBMITTED", "REPORT", id, "risk " + r.getRiskScore());
        return detail(user, reports.saveAndFlush(r));
    }

    @Transactional
    public ReportDetail review(User user, Long id, boolean approve, String note) {
        if (!isAdmin(user)) {
            throw ApiException.forbidden("Only admins review reports");
        }
        Report r = visible(user, id);
        if (isOwner(user, r)) {
            throw ApiException.forbidden("You can't review your own report");
        }
        if (r.getStatus() != Status.PENDING) {
            throw ApiException.conflict("Only pending reports can be reviewed (current: " + r.getStatus() + ")");
        }
        if (!approve && (note == null || note.isBlank())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "A rejection needs a note explaining why");
        }
        r.setStatus(approve ? Status.APPROVED : Status.REJECTED);
        r.setReviewNote(note == null || note.isBlank() ? null : note.trim());
        audit.record(user, approve ? "REPORT_APPROVED" : "REPORT_REJECTED", "REPORT", id, r.getReviewNote());
        return detail(user, reports.saveAndFlush(r));
    }

    public static void recalculate(Report r) {
        RiskCalculator.Result result = RiskCalculator.assess(r.getRecords());
        r.setTotalIncome(result.income());
        r.setTotalExpense(result.expense());
        r.setRiskScore(result.score());
        r.setRiskLevel(result.level());
        r.getRiskFactors().clear();
        result.factors().forEach(f ->
                r.getRiskFactors().add(new RiskAssessment(r, f.factor(), f.points(), f.explanation())));
    }

    private static void apply(Report r, ReportRequest req) {
        r.setTitle(req.title().trim());
        r.setDescription(req.description());
        r.setPeriod(req.period());
    }

    private Report visible(User user, Long id) {
        Report r = reports.findById(id).orElseThrow(() -> ApiException.notFound("Report"));
        if (!isAdmin(user) && !isOwner(user, r) && r.getStatus() != Status.APPROVED) {
            throw ApiException.notFound("Report");
        }
        return r;
    }

    private Report editable(User user, Long id) {
        Report r = visible(user, id);
        if (!isOwner(user, r)) {
            throw ApiException.forbidden("Only the owner can change this report");
        }
        if (!r.isEditable()) {
            throw ApiException.conflict("Report is " + r.getStatus() + " and can no longer be changed");
        }
        return r;
    }

    private ReportDetail detail(User user, Report r) {
        boolean owner = isOwner(user, r);
        boolean admin = isAdmin(user);
        Permissions can = new Permissions(
                owner && r.isEditable(),
                owner && r.isEditable() && !r.getRecords().isEmpty(),
                admin && !owner && r.getStatus() == Status.PENDING,
                admin || (owner && r.isEditable()));
        return new ReportDetail(r.getId(), r.getTitle(), r.getDescription(), r.getPeriod(), r.getStatus().name(),
                r.getReviewNote(), r.getTotalIncome(), r.getTotalExpense(), r.getRiskScore(), r.getRiskLevel().name(),
                r.getOwner().getUsername(), r.getCreatedAt(), r.getUpdatedAt(),
                r.getRecords().stream().map(RecordResponse::from).toList(),
                r.getRiskFactors().stream().map(FactorResponse::from).toList(), can);
    }

    private static boolean isOwner(User user, Report r) {
        return Objects.equals(r.getOwner().getId(), user.getId());
    }

    private static boolean isAdmin(User user) {
        return user.getRole() == User.Role.ADMIN;
    }
}
