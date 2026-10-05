package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.LoanInterestAccrual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanInterestAccrualRepository extends JpaRepository<LoanInterestAccrual, Long> {
    List<LoanInterestAccrual> findByLoanIdOrderByAccrualDateAscIdAsc(Long loanId);

    List<LoanInterestAccrual> findByLoanIdIn(List<Long> loanIds);
}
