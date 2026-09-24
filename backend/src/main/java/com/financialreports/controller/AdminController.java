package com.financialreports.controller;

import com.financialreports.dto.AdminDtos.*;
import com.financialreports.dto.AuthDtos.UserResponse;
import com.financialreports.dto.ReportDtos.PageResponse;
import com.financialreports.entity.User;
import com.financialreports.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /api/v1/admin/** exige ROLE_ADMIN no SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return admin.users();
    }

    @PatchMapping("/users/{id}")
    public UserResponse update(@AuthenticationPrincipal User me, @PathVariable Long id,
                               @RequestBody UserUpdateRequest req) {
        return admin.update(me, id, req);
    }

    @GetMapping("/audit-logs")
    public PageResponse<AuditEntry> auditLogs(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return admin.auditLog(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));
    }
}
