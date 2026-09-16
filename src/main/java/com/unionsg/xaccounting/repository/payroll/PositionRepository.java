package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
    boolean existsByTitle(String title);
}
