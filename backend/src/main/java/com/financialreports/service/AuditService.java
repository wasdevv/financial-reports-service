package com.financialreports.service;

import com.financialreports.entity.AuditLog;
import com.financialreports.entity.User;
import com.financialreports.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Grava na transação de quem chamou: se a mudança sofre rollback, o log também, e vice-versa. */
@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User actor, String action, String entityType, Long entityId, String details) {
        logs.save(new AuditLog(actor, action, entityType, entityId, details));
    }
}
