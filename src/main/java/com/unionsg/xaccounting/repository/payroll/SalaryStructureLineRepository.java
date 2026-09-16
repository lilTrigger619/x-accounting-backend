package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.SalaryStructureLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryStructureLineRepository extends JpaRepository<SalaryStructureLine, Long> {
    List<SalaryStructureLine> findBySalaryStructureId(Long salaryStructureId);
}
