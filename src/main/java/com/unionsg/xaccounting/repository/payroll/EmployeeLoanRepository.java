package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.EmployeeLoan;
import com.unionsg.xaccounting.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, Long> {
    List<EmployeeLoan> findByEmployeeIdAndStatus(Long employeeId, LoanStatus status);
    List<EmployeeLoan> findByStatus(LoanStatus status);
}
