package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.RecurringJournalFrequency;
import com.unionsg.xaccounting.enums.RecurringJournalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJournalTemplateResponse {
    private Long id;
    private String description;
    private String reference;
    private JournalType journalType;
    private RecurringJournalFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxOccurrences;
    private int occurrencesGenerated;
    private LocalDate nextRunDate;
    private RecurringJournalStatus status;
    private List<RecurringJournalTemplateLineResponse> lines;
}
