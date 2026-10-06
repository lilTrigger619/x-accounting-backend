package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DownpaymentAllocationResponse {
    private Long id;
    private Long downpaymentId;
    private String downpaymentNumber;
    private String counterpartyName;
    private SettlementDocumentType documentType;
    private Long documentId;
    private String documentNumber;
    private BigDecimal documentTotal;
    private BigDecimal documentBalance;
    private BigDecimal amount;
    private LocalDate allocationDate;
    private String notes;
    private Long journalId;
    private String journalNumber;
    private Boolean reversed;
    private LocalDateTime reversedAt;
    private String reversalReason;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private LocalDateTime createdAt;
}
