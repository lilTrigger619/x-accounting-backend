package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollRecordComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollRecordComponentRepository extends JpaRepository<PayrollRecordComponent, Long> {
    List<PayrollRecordComponent> findByEmployeePayrollRecordId(Long employeePayrollRecordId);
}
