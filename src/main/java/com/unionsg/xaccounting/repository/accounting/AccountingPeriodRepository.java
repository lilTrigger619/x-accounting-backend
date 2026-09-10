package com.unionsg.xaccounting.repository.accounting;

import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    List<AccountingPeriod> findByFinancialYearIdOrderByPeriodNumberAsc(Long financialYearId);

    @Query("select p from AccountingPeriod p where p.startDate <= :date and p.endDate >= :date")
    Optional<AccountingPeriod> findByDateInRange(@Param("date") LocalDate date);

    @Query("select count(p) > 0 from AccountingPeriod p where p.financialYear.id = :financialYearId and p.id <> :excludeId and p.startDate <= :end and p.endDate >= :start")
    boolean existsOverlapping(
            @Param("financialYearId") Long financialYearId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("excludeId") Long excludeId
    );

    long countByFinancialYearId(Long financialYearId);

    long countByFinancialYearIdAndStatus(Long financialYearId, com.unionsg.xaccounting.enums.AccountingPeriodStatus status);
}
