package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoanIdOrderByRepaymentDateDesc(Long loanId);
}
