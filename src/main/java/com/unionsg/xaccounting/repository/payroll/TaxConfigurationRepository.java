package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.TaxConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, Long> {

    @org.springframework.data.jpa.repository.Query("""
            select t from TaxConfiguration t
            where t.active = true
              and t.effectiveFrom <= :asOf
              and (t.effectiveTo is null or t.effectiveTo >= :asOf)
            order by t.effectiveFrom desc
            """)
    Optional<TaxConfiguration> findEffectiveAsOf(LocalDate asOf);
}
