package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollGroupRepository extends JpaRepository<PayrollGroup, Long> {
    boolean existsByName(String name);
}
