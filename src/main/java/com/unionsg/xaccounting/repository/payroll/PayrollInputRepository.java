package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollInput;
import com.unionsg.xaccounting.enums.PayrollInputStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollInputRepository extends JpaRepository<PayrollInput, Long> {
    List<PayrollInput> findByPayrollCalendarPeriodIdAndStatus(Long payrollCalendarPeriodId, PayrollInputStatus status);
    List<PayrollInput> findByEmployeeIdAndPayrollCalendarPeriodId(Long employeeId, Long payrollCalendarPeriodId);
    List<PayrollInput> findByAppliedToRunId(Long appliedToRunId);
}
