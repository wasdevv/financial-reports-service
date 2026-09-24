package com.financialreports.dto;

import com.financialreports.entity.AuditLog;
import com.financialreports.entity.User;
import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {}

    public record UserUpdateRequest(User.Role role, Boolean active) {}

    public record AuditEntry(Long id, String actor, String action, String entityType, Long entityId, String details,
                             Instant createdAt) {
        public static AuditEntry from(AuditLog a) {
            return new AuditEntry(a.getId(), a.getActor().getUsername(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getDetails(), a.getCreatedAt());
        }
    }
}
