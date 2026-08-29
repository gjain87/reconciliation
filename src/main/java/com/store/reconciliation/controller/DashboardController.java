package com.store.reconciliation.controller;

import com.store.reconciliation.Dtos.DiscrepancyDto;
import com.store.reconciliation.Models.Discrepancy;
import com.store.reconciliation.Repository.DiscrepancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DiscrepancyRepository discrepancyRepository;

    @GetMapping("/discrepancies")
    public ResponseEntity<List<DiscrepancyDto>> getDiscrepancies(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        List<Discrepancy> list = discrepancyRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<DiscrepancyDto> dtos = list.stream().map(d -> new DiscrepancyDto(
                d.getId(),
                d.getOrderId(),
                d.getPaymentId(),
                d.getType(),
                d.getOrderAmount(),
                d.getPaymentAmount(),
                d.getDifferenceAmount(),
                d.getRiskLevel(),
                d.getLlmRootCause(),
                d.getLlmBusinessImpact(),
                d.getLlmRecommendedAction()
        )).toList();
        return ResponseEntity.ok(dtos);
    }
}
