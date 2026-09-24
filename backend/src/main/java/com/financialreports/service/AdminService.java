package com.financialreports.service;

import com.financialreports.dto.AdminDtos.AuditEntry;
import com.financialreports.dto.AdminDtos.UserUpdateRequest;
import com.financialreports.dto.AuthDtos.UserResponse;
import com.financialreports.dto.ReportDtos.PageResponse;
import com.financialreports.entity.User;
import com.financialreports.exception.ApiException;
import com.financialreports.repository.AuditLogRepository;
import com.financialreports.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository users;
    private final AuditLogRepository logs;
    private final AuditService audit;

    public AdminService(UserRepository users, AuditLogRepository logs, AuditService audit) {
        this.users = users;
        this.logs = logs;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> users() {
        return users.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse update(User admin, Long id, UserUpdateRequest req) {
        // Sem isso o último admin pode se rebaixar e ninguém mais revisa nada.
        if (Objects.equals(admin.getId(), id)) {
            throw ApiException.forbidden("You can't change your own role or status");
        }
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (req.role() != null && req.role() != u.getRole()) {
            audit.record(admin, "USER_ROLE_CHANGED", "USER", id, u.getRole() + " -> " + req.role());
            u.setRole(req.role());
        }
        if (req.active() != null && req.active() != u.isActive()) {
            audit.record(admin, req.active() ? "USER_ENABLED" : "USER_DISABLED", "USER", id, u.getUsername());
            u.setActive(req.active());
        }
        return UserResponse.from(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> auditLog(Pageable pageable) {
        return PageResponse.from(logs.findAllByOrderByCreatedAtDesc(pageable).map(AuditEntry::from));
    }
}
