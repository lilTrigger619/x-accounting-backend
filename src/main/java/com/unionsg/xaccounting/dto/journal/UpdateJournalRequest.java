package com.unionsg.xaccounting.dto.journal;
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

    @Valid
    @NotEmpty(message = "Journal must contain at least one line")
    private List<CreateJournalLineRequest> lines;

}
