package com.unionsg.xaccounting.communication.dto;

import com.unionsg.xaccounting.communication.enums.EmailProvider;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MailConfigurationResponse {

    private Long id;
    private String companyId;
    private EmailProvider provider;
    private String smtpHost;
    private Integer smtpPort;
    private String username;
    private String fromEmail;
    private String fromName;
    private boolean useTls;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

