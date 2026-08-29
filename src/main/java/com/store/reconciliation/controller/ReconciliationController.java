package com.store.reconciliation.controller;

import com.store.reconciliation.Dtos.DashboardSummaryDto;
import com.store.reconciliation.Utils.CsvParserUtil;
import com.store.reconciliation.services.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
//    private final LlmExplanationService llmExplanationService;

    @PostMapping("/process")
    public ResponseEntity<DashboardSummaryDto> uploadAndReconcile(
            @RequestParam("orders") MultipartFile ordersFile,
            @RequestParam("payments") MultipartFile paymentsFile,
            Authentication auth
    ) throws Exception {
        Long userId = (Long) auth.getCredentials();
        var orders = CsvParserUtil.parseOrders(ordersFile);
        var payments = CsvParserUtil.parsePayments(paymentsFile);
        return ResponseEntity.ok(reconciliationService.reconcileAndStore(userId, orders, payments));
    }

//    @PostMapping("/explain/{discrepancyId}")
//    public ResponseEntity<LlmDiagnosisDto> explainDiscrepancy(
//            @PathVariable Long discrepancyId,
//            Authentication auth
//    ) {
//        Long userId = (Long) auth.getCredentials();
//        return ResponseEntity.ok(llmExplanationService.explainDiscrepancy(discrepancyId, userId));
//    }
}
