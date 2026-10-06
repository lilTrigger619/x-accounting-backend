package com.unionsg.xaccounting.dto.banking;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.banking.BankTransferAction;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferActivityResponse {
    private Long id;
    private BankTransferAction action;
    private BankTransferStatus fromStatus;
    private BankTransferStatus toStatus;
    private String details;
    private CreatedByDTO performedBy;
    private LocalDateTime performedAt;
}
