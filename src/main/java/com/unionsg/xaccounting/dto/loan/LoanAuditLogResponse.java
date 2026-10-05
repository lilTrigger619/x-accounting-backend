package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanAuditAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoanAuditLogResponse {
    private Long id;
    private LoanAuditAction action;
    private String previousStatus;
    private String newStatus;
    private String details;
    private String performedBy;
    private LocalDateTime performedAt;
}
