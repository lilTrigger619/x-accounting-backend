package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAmortizationLineRepository extends JpaRepository<LoanAmortizationLine, Long> {
}
