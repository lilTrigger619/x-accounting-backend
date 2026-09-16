package com.unionsg.xaccounting.repository.payroll;

import com.unionsg.xaccounting.entity.payroll.PayComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayComponentRepository extends JpaRepository<PayComponent, Long> {
    boolean existsByCode(String code);
    Optional<PayComponent> findByCode(String code);
}
