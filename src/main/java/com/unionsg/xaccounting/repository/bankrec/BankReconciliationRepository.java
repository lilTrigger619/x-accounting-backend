package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankReconciliation;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankReconciliationRepository
        extends JpaRepository<BankReconciliation, Long>, JpaSpecificationExecutor<BankReconciliation> {

    Optional<BankReconciliation> findByIdAndDeletedFalse(Long id);

    List<BankReconciliation> findByBankAccountIdAndStatusInAndDeletedFalse(
            Long bankAccountId, Collection<ReconciliationStatus> statuses);

    @Query("""
            select r from BankReconciliation r
            where r.bankAccount.id = :bankAccountId and r.status = :status and r.deleted = false
            order by r.periodEnd desc, r.id desc
            """)
    List<BankReconciliation> findByAccountAndStatusLatestFirst(
            @Param("bankAccountId") Long bankAccountId, @Param("status") ReconciliationStatus status);

    @Query("""
            select r from BankReconciliation r
            where r.bankAccount.id = :bankAccountId and r.deleted = false
            order by r.periodEnd desc, r.id desc
            """)
    List<BankReconciliation> findHistory(@Param("bankAccountId") Long bankAccountId);

    boolean existsByBankAccountIdAndDeletedFalse(Long bankAccountId);
}
