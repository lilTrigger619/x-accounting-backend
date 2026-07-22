package com.unionsg.xaccounting.communication.service;

import com.unionsg.xaccounting.communication.domain.EmailAttachment;
import com.unionsg.xaccounting.communication.domain.EmailLog;
import com.unionsg.xaccounting.communication.dto.EmailLogResponse;
import com.unionsg.xaccounting.communication.enums.EmailStatus;
import com.unionsg.xaccounting.communication.mapper.CommunicationMapper;
import com.unionsg.xaccounting.communication.repository.EmailAttachmentRepository;
import com.unionsg.xaccounting.communication.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailLogService {

    private final EmailLogRepository emailLogRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;

    @Transactional
    public EmailLog createLog(String companyId, String entityType, String entityId,
                              String recipient, String subject) {
        EmailLog log = EmailLog.builder()
                .companyId(companyId)
                .entityType(entityType)
                .entityId(entityId)
                .recipient(recipient)
                .subject(subject)
                .status(EmailStatus.PENDING)
                .build();
        return emailLogRepository.save(log);
    }

    @Transactional
    public void markSending(Long logId) {
        emailLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(EmailStatus.SENDING);
            emailLogRepository.save(log);
        });
    }

    @Transactional
    public void markSent(Long logId, String providerMessageId) {
        emailLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(EmailStatus.SENT);
            log.setProviderMessageId(providerMessageId);
            log.setSentAt(LocalDateTime.now());
            emailLogRepository.save(log);
        });
    }

    @Transactional
    public void markFailed(Long logId, String failedReason) {
        emailLogRepository.findById(logId).ifPresent(log -> {
            log.setStatus(EmailStatus.FAILED);
            log.setFailedReason(failedReason);
            emailLogRepository.save(log);
        });
    }

    @Transactional
    public void addAttachment(Long emailLogId, Long fileId) {
        EmailLog log = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new RuntimeException("Email log not found"));
        EmailAttachment attachment = EmailAttachment.builder()
                .emailLog(log)
                .fileId(fileId)
                .build();
        emailAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public Page<EmailLogResponse> listLogs(String entityType, EmailStatus status, Pageable pageable) {
        Page<EmailLog> logs;

        if (entityType != null && status != null) {
            logs = emailLogRepository.findByEntityTypeAndStatus(entityType, status, pageable);
        } else if (entityType != null) {
            logs = emailLogRepository.findByEntityType(entityType, pageable);
        } else if (status != null) {
            logs = emailLogRepository.findByStatus(status, pageable);
        } else {
            logs = emailLogRepository.findAll(pageable);
        }

        return logs.map(CommunicationMapper::toEmailLogResponse);
    }

    @Transactional(readOnly = true)
    public List<EmailLogResponse> getLogsForEntity(String entityType, String entityId) {
        return emailLogRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .stream()
                .map(CommunicationMapper::toEmailLogResponse)
                .collect(Collectors.toList());
    }
}

