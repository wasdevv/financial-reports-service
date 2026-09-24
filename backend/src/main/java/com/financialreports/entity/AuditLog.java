package com.financialreports.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "audit_logs")
@Getter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private User actor;

    private String action;
    private String entityType;
    private Long entityId;
    private String details;

    @Column(updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(User actor, String action, String entityType, Long entityId, String details) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.createdAt = Instant.now();
    }
}
