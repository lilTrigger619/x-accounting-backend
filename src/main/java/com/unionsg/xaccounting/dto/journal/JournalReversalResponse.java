package com.unionsg.xaccounting.dto.journal;

import lombok.*;

import java.time.LocalDate;

/** The entry that reversed a journal, shown on the reversed journal. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalReversalResponse {
    private Long journalId;
    private String journalNumber;
    private LocalDate reverseDate;
    private String reason;
    private String reversedBy;
}
