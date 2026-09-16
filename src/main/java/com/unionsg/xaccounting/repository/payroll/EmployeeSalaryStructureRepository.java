package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryStructureRepository extends JpaRepository<EmployeeSalaryStructure, Long> {

    List<EmployeeSalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    Optional<EmployeeSalaryStructure> findByEmployeeIdAndCurrentTrue(Long employeeId);

    /** The compensation assignment in force on a given date - what a historical payroll run must use. */
    @org.springframework.data.jpa.repository.Query("""
            select ess from EmployeeSalaryStructure ess
            where ess.employee.id = :employeeId
              and ess.effectiveFrom <= :asOf
              and (ess.effectiveTo is null or ess.effectiveTo >= :asOf)
            """)
    Optional<EmployeeSalaryStructure> findEffectiveAsOf(Long employeeId, LocalDate asOf);
}
