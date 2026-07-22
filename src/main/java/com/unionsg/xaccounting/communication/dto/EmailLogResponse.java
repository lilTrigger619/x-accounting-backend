package com.unionsg.xaccounting.communication.dto;

import com.unionsg.xaccounting.communication.enums.EmailStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailLogResponse {

    private Long id;
    private String companyId;
    private String entityType;
    private String entityId;
    private String recipient;
    private String subject;
    private EmailStatus status;
    private String providerMessageId;
    private LocalDateTime sentAt;
    private String failedReason;
    private LocalDateTime createdAt;
}

