package com.unionsg.xaccounting.communication.mapper;

import com.unionsg.xaccounting.communication.domain.EmailLog;
import com.unionsg.xaccounting.communication.domain.MailConfiguration;
import com.unionsg.xaccounting.communication.dto.EmailLogResponse;
import com.unionsg.xaccounting.communication.dto.MailConfigurationRequest;
import com.unionsg.xaccounting.communication.dto.MailConfigurationResponse;
import com.unionsg.xaccounting.communication.util.EncryptionService;

public class CommunicationMapper {

    public static MailConfiguration toEntity(MailConfigurationRequest request, EncryptionService encryptionService) {
        MailConfiguration config = new MailConfiguration();
        config.setCompanyId(request.getCompanyId());
        config.setProvider(request.getProvider());
        config.setSmtpHost(request.getSmtpHost());
        config.setSmtpPort(request.getSmtpPort());
        config.setUsername(request.getUsername());
        config.setFromEmail(request.getFromEmail());
        config.setFromName(request.getFromName());
        config.setUseTls(request.isUseTls());
        config.setActive(request.isActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            config.setEncryptedPassword(encryptionService.encrypt(request.getPassword()));
        }

        return config;
    }

    public static void applyUpdate(MailConfiguration config, MailConfigurationRequest request, EncryptionService encryptionService) {
        if (request.getCompanyId() != null) config.setCompanyId(request.getCompanyId());
        if (request.getProvider() != null) config.setProvider(request.getProvider());
        if (request.getSmtpHost() != null) config.setSmtpHost(request.getSmtpHost());
        if (request.getSmtpPort() != null) config.setSmtpPort(request.getSmtpPort());
        if (request.getUsername() != null) config.setUsername(request.getUsername());
        if (request.getFromEmail() != null) config.setFromEmail(request.getFromEmail());
        if (request.getFromName() != null) config.setFromName(request.getFromName());
        config.setUseTls(request.isUseTls());
        config.setActive(request.isActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            config.setEncryptedPassword(encryptionService.encrypt(request.getPassword()));
        }
    }

    public static MailConfigurationResponse toResponse(MailConfiguration config) {
        if (config == null) return null;

        MailConfigurationResponse response = new MailConfigurationResponse();
        response.setId(config.getId());
        response.setCompanyId(config.getCompanyId());
        response.setProvider(config.getProvider());
        response.setSmtpHost(config.getSmtpHost());
        response.setSmtpPort(config.getSmtpPort());
        response.setUsername(config.getUsername());
        response.setFromEmail(config.getFromEmail());
        response.setFromName(config.getFromName());
        response.setUseTls(config.isUseTls());
        response.setActive(config.isActive());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());

        return response;
    }

    public static EmailLogResponse toEmailLogResponse(EmailLog log) {
        if (log == null) return null;

        EmailLogResponse response = new EmailLogResponse();
        response.setId(log.getId());
        response.setCompanyId(log.getCompanyId());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setRecipient(log.getRecipient());
        response.setSubject(log.getSubject());
        response.setStatus(log.getStatus());
        response.setProviderMessageId(log.getProviderMessageId());
        response.setSentAt(log.getSentAt());
        response.setFailedReason(log.getFailedReason());
        response.setCreatedAt(log.getCreatedAt());

        return response;
    }
}

