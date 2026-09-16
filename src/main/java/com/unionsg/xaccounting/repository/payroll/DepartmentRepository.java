package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByName(String name);
}
