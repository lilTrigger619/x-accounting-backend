package com.unionsg.xaccounting.repository.prepayment;

import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrepaymentTypeRepository extends JpaRepository<PrepaymentType, Long> {
    List<PrepaymentType> findByActiveTrueAndDeletedFalse();
    List<PrepaymentType> findByDeletedFalse();
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);
}
