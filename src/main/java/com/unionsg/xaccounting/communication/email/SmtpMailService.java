package com.unionsg.xaccounting.communication.email;

import com.unionsg.xaccounting.communication.domain.MailConfiguration;
import com.unionsg.xaccounting.communication.dto.EmailSendResult;
import com.unionsg.xaccounting.communication.enums.EmailStatus;
import com.unionsg.xaccounting.communication.repository.MailConfigurationRepository;
import com.unionsg.xaccounting.communication.util.EncryptionService;
import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.FileService.FileStorageService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);

    private final MailConfigurationRepository configRepository;
    private final EncryptionService encryptionService;
    private final FileService fileService;
    private final FileStorageService fileStorageService;

    @Override
    public EmailSendResult send(EmailMessage message) {
        try {
            MailConfiguration config = configRepository.findByActiveTrue()
                    .orElseThrow(() -> new RuntimeException("No active mail configuration found"));

            JavaMailSender mailSender = createMailSender(config);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setFrom(config.getFromEmail(), config.getFromName());

            if (message.isHtml()) {
                helper.setText(message.getBody(), true);
            } else {
                helper.setText(message.getBody());
            }

            // Attach files using File Service
            if (message.getAttachmentFileIds() != null) {
                for (Long fileIdLong : message.getAttachmentFileIds()) {
                    try {
                        // Convert Long to UUID (files stored as string UUIDs)
                        String uuidStr = fileIdLong.toString();
                        // Try to get file info
                        FileResponseDto fileInfo = fileService.getFile(uuidStr);
                        Resource resource = fileStorageService.download(fileInfo.getStoragePath());
                        if (resource != null && resource.exists()) {
                            helper.addAttachment(
                                    fileInfo.getOriginalName() != null ? fileInfo.getOriginalName() : "attachment-" + fileIdLong,
                                    resource
                            );
                        }
                    } catch (Exception e) {
                        log.warn("Failed to attach file {}: {}", fileIdLong, e.getMessage());
                    }
                }
            }

            mailSender.send(mimeMessage);

            return EmailSendResult.builder()
                    .success(true)
                    .providerMessageId(mimeMessage.getMessageID())
                    .status(EmailStatus.SENT)
                    .build();

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", message.getTo(), e.getMessage());
            return EmailSendResult.builder()
                    .success(false)
                    .status(EmailStatus.FAILED)
                    .failedReason(e.getMessage())
                    .build();
        }
    }

    private JavaMailSender createMailSender(MailConfiguration config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort() != null ? config.getSmtpPort() : 587);

        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            mailSender.setUsername(config.getUsername());
            if (config.getEncryptedPassword() != null) {
                mailSender.setPassword(encryptionService.decrypt(config.getEncryptedPassword()));
            }
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", config.getUsername() != null ? "true" : "false");
        props.put("mail.smtp.starttls.enable", config.isUseTls() ? "true" : "false");
        props.put("mail.debug", "false");

        return mailSender;
    }
}

