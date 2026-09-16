package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.EmployeePayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeePayrollRecordRepository extends JpaRepository<EmployeePayrollRecord, Long> {
    List<EmployeePayrollRecord> findByPayrollRunId(Long payrollRunId);
    List<EmployeePayrollRecord> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    Optional<EmployeePayrollRecord> findByPayrollRunIdAndEmployeeId(Long payrollRunId, Long employeeId);
}
