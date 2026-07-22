package com.unionsg.xaccounting.communication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class EmailMessageRequest {

    @NotBlank(message = "Recipient email is required")
    private String to;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String body;

    private List<Long> attachmentIds;
}

