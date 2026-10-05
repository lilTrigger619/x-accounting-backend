package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.LoanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanTypeRepository extends JpaRepository<LoanType, Long> {
    List<LoanType> findByActiveTrueAndDeletedFalse();
    List<LoanType> findByDeletedFalse();
    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);
}
