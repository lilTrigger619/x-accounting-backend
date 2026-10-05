package com.unionsg.xaccounting.dto.banking;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferReasonRequest {

    @Size(max = 500, message = "Reason can be at most 500 characters")
    private String reason;
}
