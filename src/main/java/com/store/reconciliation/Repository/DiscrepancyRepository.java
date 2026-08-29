package com.store.reconciliation.Repository;

import com.store.reconciliation.Models.Discrepancy;
import com.store.reconciliation.Models.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DiscrepancyRepository extends JpaRepository<Discrepancy, Long>, JpaSpecificationExecutor<Discrepancy> {

    List<Discrepancy> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM Discrepancy d WHERE d.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM Discrepancy d WHERE d.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.differenceAmount), 0) FROM Discrepancy d WHERE d.userId = :userId AND d.riskLevel = :risk")
    BigDecimal sumDifferenceByRiskLevel(@Param("userId") Long userId, @Param("risk") RiskLevel risk);

    @Query("SELECT d.type, COUNT(d), COALESCE(SUM(d.differenceAmount), 0) FROM Discrepancy d WHERE d.userId = :userId GROUP BY d.type")
    List<Object[]> getDiscrepancyTypeBreakdown(@Param("userId") Long userId);
}