package com.store.reconciliation.services;

import com.store.reconciliation.Dtos.DashboardSummaryDto;
import com.store.reconciliation.Models.Discrepancy;
import com.store.reconciliation.Models.DiscrepancyType;
import com.store.reconciliation.Models.RiskLevel;
import com.store.reconciliation.Repository.DiscrepancyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.store.reconciliation.Utils.CsvParserUtil.OrderRow;
import com.store.reconciliation.Utils.CsvParserUtil.PaymentRow;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final DiscrepancyRepository discrepancyRepository;
    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("0.05");

    @Transactional
    public DashboardSummaryDto reconcileAndStore(Long userId, List<OrderRow> orders, List<PaymentRow> payments) {
        discrepancyRepository.deleteByUserId(userId);

        Map<String, List<OrderRow>> ordersByRef = orders.stream()
                .collect(Collectors.groupingBy(OrderRow::getCleanId));

        Map<String, List<PaymentRow>> paymentsByRef = payments.stream()
                .collect(Collectors.groupingBy(PaymentRow::getCleanOrderRef));

        List<Discrepancy> discrepancies = new ArrayList<>();
        Set<String> evaluatedPaymentRefs = new HashSet<>();

        BigDecimal totalReconciled = BigDecimal.ZERO;
        BigDecimal totalAtRisk = BigDecimal.ZERO;
        int matchedCount = 0;

        for (Map.Entry<String, List<OrderRow>> entry : ordersByRef.entrySet()) {
            String orderRef = entry.getKey();
            List<OrderRow> orderList = entry.getValue();
            OrderRow order = orderList.get(0);

            if (orderList.size() > 1) {
                discrepancies.add(build(userId, orderRef, null, DiscrepancyType.DUPLICATE_ORDER_RECORD,
                        order.netAmount(), BigDecimal.ZERO, order.netAmount(), RiskLevel.MEDIUM));
                totalAtRisk = totalAtRisk.add(order.netAmount());
            }

            List<PaymentRow> linkedPayments = paymentsByRef.getOrDefault(orderRef, Collections.emptyList());
            evaluatedPaymentRefs.add(orderRef);

            if (linkedPayments.isEmpty()) {
                if ("completed".equalsIgnoreCase(order.status())) {
                    discrepancies.add(build(userId, orderRef, null, DiscrepancyType.UNPAID_COMPLETED_ORDER,
                            order.netAmount(), BigDecimal.ZERO, order.netAmount(), RiskLevel.HIGH));
                    totalAtRisk = totalAtRisk.add(order.netAmount());
                }
                continue;
            }

            List<PaymentRow> charges = linkedPayments.stream().filter(p -> "charge".equalsIgnoreCase(p.type())).toList();
            List<PaymentRow> refunds = linkedPayments.stream().filter(p -> "refund".equalsIgnoreCase(p.type())).toList();

            // Duplicate Charge Detection
            if (charges.size() > 1) {
                BigDecimal totalCharged = charges.stream().map(PaymentRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal extra = totalCharged.subtract(order.netAmount());
                discrepancies.add(build(userId, orderRef, "MULTIPLE", DiscrepancyType.DUPLICATE_PAYMENT_CHARGE,
                        order.netAmount(), totalCharged, extra.abs(), RiskLevel.HIGH));
                totalAtRisk = totalAtRisk.add(extra.abs());
                continue;
            }

            PaymentRow primaryCharge = charges.isEmpty() ? null : charges.get(0);

            if (primaryCharge != null) {
                // Currency mismatch
                if (!order.currency().equalsIgnoreCase(primaryCharge.currency())) {
                    discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.CURRENCY_MISMATCH,
                            order.netAmount(), primaryCharge.amount(), order.netAmount(), RiskLevel.HIGH));
                    totalAtRisk = totalAtRisk.add(order.netAmount());
                    continue;
                }

                // Payment Status failed on completed order
                if ("failed".equalsIgnoreCase(primaryCharge.status()) && "completed".equalsIgnoreCase(order.status())) {
                    discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.PAYMENT_FAILED_ON_COMPLETED,
                            order.netAmount(), primaryCharge.amount(), order.netAmount(), RiskLevel.HIGH));
                    totalAtRisk = totalAtRisk.add(order.netAmount());
                    continue;
                }

                // Payment Status pending on completed order
                if ("pending".equalsIgnoreCase(primaryCharge.status()) && "completed".equalsIgnoreCase(order.status())) {
                    discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.PAYMENT_PENDING_ON_COMPLETED,
                            order.netAmount(), primaryCharge.amount(), order.netAmount(), RiskLevel.MEDIUM));
                    totalAtRisk = totalAtRisk.add(order.netAmount());
                    continue;
                }

                // Amount mismatch
                BigDecimal diff = order.netAmount().subtract(primaryCharge.amount()).abs();
                if (diff.compareTo(BigDecimal.ZERO) > 0) {
                    if (diff.compareTo(ROUNDING_TOLERANCE) <= 0) {
                        discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.ROUNDING_VARIANCE,
                                order.netAmount(), primaryCharge.amount(), diff, RiskLevel.LOW));
                        totalReconciled = totalReconciled.add(order.netAmount());
                    } else {
                        discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.AMOUNT_MISMATCH,
                                order.netAmount(), primaryCharge.amount(), diff, RiskLevel.HIGH));
                        totalAtRisk = totalAtRisk.add(diff);
                    }
                    continue;
                }

                // Lifecycle Cancelled / Refunded checks
                if ("cancelled".equalsIgnoreCase(order.status()) && "settled".equalsIgnoreCase(primaryCharge.status()) && refunds.isEmpty()) {
                    discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.CANCELLED_ORDER_PAYMENT_SETTLED,
                            order.netAmount(), primaryCharge.amount(), primaryCharge.amount(), RiskLevel.HIGH));
                    totalAtRisk = totalAtRisk.add(primaryCharge.amount());
                    continue;
                }

                if ("refunded".equalsIgnoreCase(order.status())) {
                    BigDecimal totalRefunded = refunds.stream().map(PaymentRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (totalRefunded.compareTo(order.netAmount()) != 0) {
                        discrepancies.add(build(userId, orderRef, primaryCharge.transactionRef(), DiscrepancyType.PARTIAL_REFUND_MISMATCH,
                                order.netAmount(), totalRefunded, order.netAmount().subtract(totalRefunded).abs(), RiskLevel.MEDIUM));
                        totalAtRisk = totalAtRisk.add(order.netAmount().subtract(totalRefunded).abs());
                        continue;
                    }
                }

                matchedCount++;
                totalReconciled = totalReconciled.add(order.netAmount());
            }
        }

        // Identify Orphan Payments
        for (Map.Entry<String, List<PaymentRow>> entry : paymentsByRef.entrySet()) {
            if (!evaluatedPaymentRefs.contains(entry.getKey())) {
                for (PaymentRow p : entry.getValue()) {
                    discrepancies.add(build(userId, entry.getKey(), p.transactionRef(), DiscrepancyType.UNRECORDED_ORPHAN_PAYMENT,
                            BigDecimal.ZERO, p.amount(), p.amount(), RiskLevel.HIGH));
                    totalAtRisk = totalAtRisk.add(p.amount());
                }
            }
        }

        discrepancyRepository.saveAll(discrepancies);

        List<DashboardSummaryDto.DiscrepancyTypeSummary> breakdown = discrepancies.stream()
                .collect(Collectors.groupingBy(d -> d.getType().name()))
                .entrySet().stream()
                .map(e -> new DashboardSummaryDto.DiscrepancyTypeSummary(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream().map(Discrepancy::getDifferenceAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        return new DashboardSummaryDto(orders.size(), payments.size(), matchedCount, discrepancies.size(), totalReconciled, totalAtRisk, breakdown);
    }

    private Discrepancy build(Long userId, String orderId, String paymentId, DiscrepancyType type,
                              BigDecimal oAmt, BigDecimal pAmt, BigDecimal diff, RiskLevel risk) {
        return Discrepancy.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentId(paymentId)
                .type(type)
                .orderAmount(oAmt)
                .paymentAmount(pAmt)
                .differenceAmount(diff)
                .riskLevel(risk)
                .build();
    }
}
