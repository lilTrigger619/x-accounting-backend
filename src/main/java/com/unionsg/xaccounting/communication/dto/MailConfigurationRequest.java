package com.unionsg.xaccounting.communication.dto;

import com.unionsg.xaccounting.communication.enums.EmailProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MailConfigurationRequest {

    private String companyId;

    @NotNull(message = "Provider is required")
    private EmailProvider provider;

    @NotBlank(message = "SMTP host is required")
    private String smtpHost;

    @NotNull(message = "SMTP port is required")
    private Integer smtpPort;

    private String username;

    private String password;

    @NotBlank(message = "From email is required")
    private String fromEmail;

    private String fromName;

    private boolean useTls = true;

    private boolean active = true;
}

