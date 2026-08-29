package com.store.reconciliation.Dtos;

public record LlmDiagnosisDto(
        String rootCause,
        String businessImpact,
        String recommendedAction
) {}
