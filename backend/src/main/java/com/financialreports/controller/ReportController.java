package com.financialreports.controller;

import com.financialreports.dto.AnalyticsResponse;
import com.financialreports.dto.ReportDtos.*;
import com.financialreports.entity.Report;
import com.financialreports.entity.User;
import com.financialreports.service.AnalyticsService;
import com.financialreports.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reports;
    private final AnalyticsService analytics;

    public ReportController(ReportService reports, AnalyticsService analytics) {
        this.reports = reports;
        this.analytics = analytics;
    }

    @GetMapping("/reports")
    public PageResponse<ReportSummary> list(@AuthenticationPrincipal User user,
                                            @RequestParam(required = false) Report.Status status,
                                            @RequestParam(required = false) Report.RiskLevel riskLevel,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "false") boolean mine,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return reports.list(user, status, riskLevel, q, mine, pageable);
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetail create(@AuthenticationPrincipal User user, @Valid @RequestBody ReportRequest req) {
        return reports.create(user, req);
    }

    @GetMapping("/reports/{id}")
    public ReportDetail get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return reports.get(user, id);
    }

    @PutMapping("/reports/{id}")
    public ReportDetail update(@AuthenticationPrincipal User user, @PathVariable Long id,
                               @Valid @RequestBody ReportRequest req) {
        return reports.update(user, id, req);
    }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        reports.delete(user, id);
    }

    @PostMapping("/reports/{id}/records")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetail addRecord(@AuthenticationPrincipal User user, @PathVariable Long id,
                                  @Valid @RequestBody RecordRequest req) {
        return reports.addRecord(user, id, req);
    }

    @DeleteMapping("/reports/{id}/records/{recordId}")
    public ReportDetail deleteRecord(@AuthenticationPrincipal User user, @PathVariable Long id,
                                     @PathVariable Long recordId) {
        return reports.deleteRecord(user, id, recordId);
    }

    @PostMapping("/reports/{id}/submit")
    public ReportDetail submit(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return reports.submit(user, id);
    }

    @PostMapping("/reports/{id}/approve")
    public ReportDetail approve(@AuthenticationPrincipal User user, @PathVariable Long id,
                                @Valid @RequestBody(required = false) ReviewRequest req) {
        return reports.review(user, id, true, req == null ? null : req.note());
    }

    @PostMapping("/reports/{id}/reject")
    public ReportDetail reject(@AuthenticationPrincipal User user, @PathVariable Long id,
                               @Valid @RequestBody(required = false) ReviewRequest req) {
        return reports.review(user, id, false, req == null ? null : req.note());
    }

    @GetMapping("/analytics/summary")
    public AnalyticsResponse summary(@AuthenticationPrincipal User user) {
        return analytics.summary(user);
    }
}
