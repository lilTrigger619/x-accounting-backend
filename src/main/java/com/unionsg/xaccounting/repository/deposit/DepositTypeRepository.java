package com.unionsg.xaccounting.repository.deposit;

import com.unionsg.xaccounting.entity.deposit.DepositType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositTypeRepository extends JpaRepository<DepositType, Long> {
    List<DepositType> findByActiveTrueAndDeletedFalse();
    List<DepositType> findByDeletedFalse();
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);
}
