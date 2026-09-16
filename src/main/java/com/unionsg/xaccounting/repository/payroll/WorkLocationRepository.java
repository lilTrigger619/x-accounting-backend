package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.WorkLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkLocationRepository extends JpaRepository<WorkLocation, Long> {
    boolean existsByName(String name);
}
