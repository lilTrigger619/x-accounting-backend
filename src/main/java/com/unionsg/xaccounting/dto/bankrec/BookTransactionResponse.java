package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.unionsg.xaccounting.enums.bankrec.BookTransactionStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookTransactionResponse {
    /** Journal line id: what a match refers to. */
    private Long id;
    private Long journalId;
    private String journalNumber;
    private LocalDate journalDate;
    private String reference;
    private String description;
    private String sourceModule;
    private Long sourceEntityId;
    private String counterparty;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    /** Bank point of view: receipts positive, payments negative. */
    private BigDecimal amount;
    private BigDecimal matchedAmount;
    private BigDecimal remainingAmount;
    private BookTransactionStatus status;
}
