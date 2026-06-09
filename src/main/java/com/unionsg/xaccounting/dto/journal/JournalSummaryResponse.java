package com.unionsg.xaccounting.dto.journal;
import com.unionsg.xaccounting.enums.JournalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalSummaryResponse {
    private Long id;

    private String journalNumber;

    private String reference;

    private String description;

    private LocalDate journalDate;

    private JournalStatus status;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;
}
