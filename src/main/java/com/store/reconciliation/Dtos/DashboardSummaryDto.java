package com.store.reconciliation.Dtos;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDto(int totalOrders,
                                  int totalPayments,
                                  int matchedRecords,
                                  int totalDiscrepancies,
                                  BigDecimal totalReconciledValue,
                                  BigDecimal totalValueAtRisk,
                                  List<DiscrepancyTypeSummary> breakdown
)
{
    public record DiscrepancyTypeSummary(
            String type,
            long count,
            BigDecimal totalValue
    ) {}
}
