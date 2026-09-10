package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.RecurringJournalFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateRecurringJournalTemplateRequest {

    @NotNull(message = "Description is required")
    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String reference;

    @NotNull(message = "Journal type is required")
    private JournalType journalType;

    @NotNull(message = "Frequency is required")
    private RecurringJournalFrequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer maxOccurrences;

    @Valid
    @NotEmpty(message = "Template must contain at least one line")
    private List<RecurringJournalTemplateLineRequest> lines;
}
