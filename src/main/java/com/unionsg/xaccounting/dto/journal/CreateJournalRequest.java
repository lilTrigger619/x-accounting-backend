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
public class CreateJournalRequest {

    @NotNull(message = "Journal date is required")
    private LocalDate journalDate;

    @Size(max = 100)
    private String reference;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Journal type is required")
    private JournalType journalType;

    @Builder.Default
    @Size(max = 10)
    private String currencyCode = "GHS";

    @Valid
    @NotEmpty(message = "Journal must contain at least one line")
    private List<CreateJournalLineRequest> lines;

}
