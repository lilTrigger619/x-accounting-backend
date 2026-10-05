package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatch;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BankReconciliationMatchRepository extends JpaRepository<BankReconciliationMatch, Long> {

    List<BankReconciliationMatch> findByReconciliationIdOrderByIdDesc(Long reconciliationId);

    List<BankReconciliationMatch> findByReconciliationIdAndStatusIn(Long reconciliationId, Collection<MatchStatus> statuses);

    boolean existsByReconciliationIdAndStatusIn(Long reconciliationId, Collection<MatchStatus> statuses);

    long countByReconciliationIdAndStatus(Long reconciliationId, MatchStatus status);
}
