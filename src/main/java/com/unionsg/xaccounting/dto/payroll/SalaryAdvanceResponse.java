package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.AdvanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SalaryAdvanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private BigDecimal amountIssued;
    private LocalDate dateIssued;
    private BigDecimal outstandingBalance;
    private BigDecimal recurringDeductionAmount;
    private AdvanceStatus status;
    private Long issuanceJournalId;
    private String reason;
}
