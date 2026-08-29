package com.store.reconciliation.Models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "discrepancies", indexes = {
        @Index(name = "idx_disc_user", columnList = "user_id"),
        @Index(name = "idx_disc_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discrepancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String orderId;
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscrepancyType type;

    @Column(precision = 15, scale = 2)
    private BigDecimal orderAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal differenceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String llmRootCause;

    @Column(columnDefinition = "TEXT")
    private String llmBusinessImpact;

    @Column(columnDefinition = "TEXT")
    private String llmRecommendedAction;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
