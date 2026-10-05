package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    boolean existsByLoanTypeId(Long loanTypeId);

    List<Loan> findByDeletedFalseAndStatusIn(Collection<LoanStatus> statuses);
}
