package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.LoanStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class EmployeeLoanResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private BigDecimal principal;
    private BigDecimal interestRatePercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal installmentAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private LoanStatus status;
    private Long disbursementJournalId;
    private String reason;
}
