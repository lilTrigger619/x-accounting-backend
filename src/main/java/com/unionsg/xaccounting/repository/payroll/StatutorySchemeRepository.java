package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.StatutoryScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StatutorySchemeRepository extends JpaRepository<StatutoryScheme, Long> {
    boolean existsByCode(String code);

    List<StatutoryScheme> findByActiveTrue();

    @org.springframework.data.jpa.repository.Query("""
            select s from StatutoryScheme s
            where s.active = true
              and s.effectiveFrom <= :asOf
              and (s.effectiveTo is null or s.effectiveTo >= :asOf)
            """)
    List<StatutoryScheme> findEffectiveAsOf(LocalDate asOf);
}
