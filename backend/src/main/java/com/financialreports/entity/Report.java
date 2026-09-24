package com.financialreports.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
public class Report {

    public enum Status { DRAFT, PENDING, APPROVED, REJECTED }

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    private String title;
    private String description;
    private String period;

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private int riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    private String reviewNote;

    @Version
    private long version;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recordDate ASC, id ASC")
    private List<FinancialRecord> records = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("points DESC")
    private List<RiskAssessment> riskFactors = new ArrayList<>();

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isEditable() {
        return status == Status.DRAFT || status == Status.REJECTED;
    }
}
