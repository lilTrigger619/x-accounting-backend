package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanListItemResponse {
    private Long id;
    private String loanNumber;
    private String loanTypeName;
    private LoanDirection direction;
    private String counterpartyName;
    private BigDecimal principalAmount;
    private BigDecimal outstandingPrincipal;
    private String currency;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LoanStatus status;
}
