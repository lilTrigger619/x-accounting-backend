package com.unionsg.xaccounting.repository.accounting;

import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {

    Optional<FinancialYear> findByIsCurrentTrue();

    List<FinancialYear> findAllByOrderByStartDateDesc();

    @Query("select fy from FinancialYear fy where fy.startDate <= :date and fy.endDate >= :date")
    Optional<FinancialYear> findByDateInRange(@Param("date") LocalDate date);

    @Query("select count(fy) > 0 from FinancialYear fy where fy.id <> :excludeId and fy.startDate <= :end and fy.endDate >= :start")
    boolean existsOverlapping(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("excludeId") Long excludeId);
}
