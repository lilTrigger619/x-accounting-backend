package com.unionsg.xaccounting.dto.journal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReverseJournalRequest {
    @NotBlank(message = "Reversal reason is required")
    @Size(max = 500)
    private String reason;
}
