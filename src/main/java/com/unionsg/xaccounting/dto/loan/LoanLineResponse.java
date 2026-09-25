package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanInstallmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanLineResponse {
    private Long id;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal openingPrincipal;
    private BigDecimal principalDue;
    private BigDecimal interestDue;
    private BigDecimal totalInstallment;
    private BigDecimal closingPrincipal;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private LoanInstallmentStatus status;
}
