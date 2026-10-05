package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DownpaymentRefundResponse {
    private Long id;
    private Long downpaymentId;
    private String refundNumber;
    private BigDecimal amount;
    private LocalDate refundDate;
    private Long bankAccountId;
    private String bankAccountName;
    private String reference;
    private String reason;
    private Long journalId;
    private String journalNumber;
    private Boolean reversed;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private LocalDateTime createdAt;
}
