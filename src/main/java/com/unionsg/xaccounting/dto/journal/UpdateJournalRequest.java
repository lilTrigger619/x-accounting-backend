package com.unionsg.xaccounting.dto.journal;

import com.unionsg.xaccounting.enums.JournalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateJournalRequest {
    @NotNull(message = "Journal date is required")
    private LocalDate journalDate;

    @Size(max = 100)
    private String reference;

    @Size(max = 500)
    private String description;

    /** Changes the journal type when given; must be a type that can be entered by hand. */
    private JournalType journalType;

    /** Changes the currency when given; must be one of the configured currencies. */
    @Size(max = 10)
    private String currencyCode;

    @Valid
    @NotEmpty(message = "Journal must contain at least one line")
    private List<CreateJournalLineRequest> lines;

}
