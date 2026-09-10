package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.RecurringOccurrenceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJournalOccurrenceResponse {
    private Long id;
    private Long templateId;
    private LocalDate scheduledDate;
    private RecurringOccurrenceStatus status;
    private Long generatedJournalId;
    private String failureReason;
    private LocalDateTime generatedAt;
}
