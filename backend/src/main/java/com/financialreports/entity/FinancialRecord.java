package com.financialreports.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "financial_records")
@Getter
@Setter
public class FinancialRecord {

    public enum Type { INCOME, EXPENSE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private Report report;

    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String category;
    private BigDecimal amount;
    private String description;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
