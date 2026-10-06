package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositAllocationResponse {
    private Long id;
    private Long depositId;
    private String depositNumber;
    private DepositDirection direction;
    private String counterpartyName;
    private String currency;
    private DepositAllocationType allocationType;
    private String allocationTypeLabel;
    private BigDecimal amount;
    private LocalDate allocationDate;
    private Long invoiceId;
    private String invoiceNumber;
    private Long billId;
    private String billNumber;
    private Long targetDepositId;
    private String targetDepositNumber;
    private Long bankAccountId;
    private String bankAccountName;
    private String reference;
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
