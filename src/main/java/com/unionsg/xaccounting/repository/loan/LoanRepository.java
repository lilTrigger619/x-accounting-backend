package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
}
