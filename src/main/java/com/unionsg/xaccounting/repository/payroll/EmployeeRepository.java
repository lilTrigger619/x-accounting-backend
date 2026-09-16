package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmployeeNumber(String employeeNumber);
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    /** Employees eligible to be included in a new payroll run for this group. */
    List<Employee> findByPayrollGroupIdAndEmploymentStatus(Long payrollGroupId, EmploymentStatus status);
}
