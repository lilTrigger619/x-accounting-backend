package com.unionsg.xaccounting.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecentTransactionDto(

        Long journalId,
        String journalNumber,
        LocalDate journalDate,
        String description,
        String reference,
        String journalType,
        String sourceModule,
        BigDecimal amount

) {
}
