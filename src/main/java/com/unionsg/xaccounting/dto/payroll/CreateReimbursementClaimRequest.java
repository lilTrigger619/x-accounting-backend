package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateReimbursementClaimRequest {
    private Long employeeId;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate claimDate;
    private boolean alreadyRecordedElsewhere;
}
