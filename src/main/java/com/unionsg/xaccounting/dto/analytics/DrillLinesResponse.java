package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Account → journal entry → source transaction level of a drill-down. */
public record DrillLinesResponse(
        Long accountId,
        String accountCode,
        String accountName,
        String classLabel,
        LocalDate from,
        LocalDate to,
        BigDecimal total,
        long totalLines,
        int page,
        int size,
        List<Line> lines
) {
    public record Line(Long lineId, Long journalId, String journalNumber, LocalDate journalDate, String status,
                       String description, String reference, BigDecimal debit, BigDecimal credit,
                       BigDecimal amount, String sourceModule, Long sourceEntityId, Long reversalOfJournalId) {
    }
}
