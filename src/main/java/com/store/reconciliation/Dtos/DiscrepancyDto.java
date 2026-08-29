package com.store.reconciliation.Dtos;

import com.store.reconciliation.Models.DiscrepancyType;
import com.store.reconciliation.Models.RiskLevel;

import java.math.BigDecimal;

public record DiscrepancyDto(
        Long id,
        String orderId,
        String paymentId,
        DiscrepancyType type,
        BigDecimal orderAmount,
        BigDecimal paymentAmount,
        BigDecimal differenceAmount,
        RiskLevel riskLevel,
        String llmRootCause,
        String llmBusinessImpact,
        String llmRecommendedAction
) {}
