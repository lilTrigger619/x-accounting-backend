package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollCalendarPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollCalendarPeriodRepository extends JpaRepository<PayrollCalendarPeriod, Long> {
    List<PayrollCalendarPeriod> findByPayrollGroupIdOrderByPeriodStartDesc(Long payrollGroupId);
}
