package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long>, JpaSpecificationExecutor<PayrollRun> {
    boolean existsByRunNumber(String runNumber);
    Optional<PayrollRun> findByRunNumber(String runNumber);
    List<PayrollRun> findByPayrollCalendarPeriodIdAndStatusNot(Long payrollCalendarPeriodId, PayrollRunStatus excludedStatus);
    List<PayrollRun> findByStatus(PayrollRunStatus status);
}
