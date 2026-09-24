package com.financialreports.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private Report report;

    private String factor;
    private int points;
    private String explanation;

    public RiskAssessment(Report report, String factor, int points, String explanation) {
        this.report = report;
        this.factor = factor;
        this.points = points;
        this.explanation = explanation;
    }
}
