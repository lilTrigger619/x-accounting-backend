package com.unionsg.xaccounting.communication.dto;

import com.unionsg.xaccounting.communication.enums.EmailStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailSendResult {

    private boolean success;
    private String providerMessageId;
    private EmailStatus status;
    private String failedReason;
}

