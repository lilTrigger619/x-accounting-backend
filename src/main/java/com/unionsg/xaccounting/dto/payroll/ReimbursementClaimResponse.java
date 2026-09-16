package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.ReimbursementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReimbursementClaimResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String description;
    private String category;
    private BigDecimal amount;
    private LocalDate claimDate;
    private ReimbursementStatus status;
    private boolean alreadyRecordedElsewhere;
    private String approvedByName;
    private LocalDateTime approvedAt;
}
