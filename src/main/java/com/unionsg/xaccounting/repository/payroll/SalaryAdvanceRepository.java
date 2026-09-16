package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.SalaryAdvance;
import com.unionsg.xaccounting.enums.AdvanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {
    List<SalaryAdvance> findByEmployeeIdAndStatusIn(Long employeeId, List<AdvanceStatus> statuses);
    List<SalaryAdvance> findByStatusIn(List<AdvanceStatus> statuses);
}
