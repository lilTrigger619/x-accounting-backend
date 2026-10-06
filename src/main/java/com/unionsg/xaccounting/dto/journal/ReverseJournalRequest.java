package com.unionsg.xaccounting.dto.journal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseJournalRequest {
    @NotBlank(message = "Reversal reason is required")
    @Size(max = 500)
    private String reason;

    /** Date of the reversing entry; today when omitted. */
    private LocalDate reverseDate;

    /** Reference for the reversing entry; "REV-" plus the original journal number when omitted. */
    @Size(max = 100)
    private String reference;
}
