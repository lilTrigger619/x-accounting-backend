package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    boolean existsByName(String name);
}
