package com.unionsg.xaccounting.dto.loan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class LoanSchedulePreviewResponse {
    private LocalDate maturityDate;
    private BigDecimal financedAmount;
    private BigDecimal totalPrincipal;
    private BigDecimal totalInterest;
    private BigDecimal totalFees;
    private BigDecimal totalPayable;
    private List<LoanLineResponse> lines;
}
