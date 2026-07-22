# Document Template Module - Progress

## ✅ Phase 1: Document Template CRUD Module
- Enums, Entities, Repositories, DTOs, Mapper, Service, Exception, Controller

## ✅ Phase 2: Thymeleaf Document Rendering + OpenHTMLToPDF
- Context layer, renderers, PDF service, Thymeleaf templates, CSS, controllers

## ✅ Phase 3: Communication Module + Invoice Email Delivery

### Config
- [x] AsyncConfig.java — @EnableAsync with emailTaskExecutor
- [x] EncryptionService.java — AES encryption for SMTP passwords

### Enums
- [x] EmailProvider.java — SMTP, SENDGRID, AWS_SES, MAILGUN
- [x] EmailStatus.java — PENDING, SENDING, SENT, FAILED

### Entities
- [x] MailConfiguration.java — SMTP settings (host, port, username, encrypted password, from, TLS)
- [x] EmailLog.java — tracks every outgoing email (entityType, entityId, recipient, status)
- [x] EmailAttachment.java — links fileId to emailLog

### Repositories
- [x] MailConfigurationRepository.java — findByActiveTrue()
- [x] EmailLogRepository.java — pagination + filtering by entityType/status
- [x] EmailAttachmentRepository.java — findByEmailLogId

### DTOs
- [x] MailConfigurationRequest.java / MailConfigurationResponse.java
- [x] EmailLogResponse.java
- [x] EmailMessageRequest.java
- [x] EmailSendResult.java

### Mapper
- [x] CommunicationMapper.java — static methods for entity↔DTO

### Mail Service
- [x] MailService.java interface — send(EmailMessage) → EmailSendResult
- [x] SmtpMailService.java — Spring Mail implementation with file attachments
- [x] EmailMessage.java — internal DTO (to, subject, body, html, attachmentFileIds)

### Email Template
- [x] EmailTemplateRenderer.java — Thymeleaf rendering + subject/salutation resolution
- [x] EmailVariableResolver.java — variable replacement for {{invoiceNumber}}, {{customerName}}, {{total}}, etc.

### Events (Invoice Integration)
- [x] InvoiceEmailRequestedEvent.java — invoiceId, customerEmail, fileId
- [x] InvoiceEmailRequestedEventListener.java — @Async listener: render → send → log

### Services
- [x] MailConfigurationService.java — CRUD for SMTP configs
- [x] EmailLogService.java — create log, markSent, markFailed, addAttachment
- [x] InvoiceEmailService.java — validates DRAFT, generates PDF, sets SENT, publishes event

### Controllers
- [x] MailConfigurationController.java — CRUD at /api/mail-configurations
- [x] EmailLogController.java — list with pagination/filtering at /api/email-logs
- [x] InvoiceController.send() — updated to use InvoiceEmailService

### Email Templates
- [x] templates/email/invoice-standard.html — professional HTML invoice email
- [x] templates/email/invoice-reminder.html — payment reminder HTML email

## Architecture Rules Followed
- ✅ Invoice module does NOT know JavaMailSender exists
- ✅ Communication module does NOT know Invoice entity internals
- ✅ Events used between modules (InvoiceEmailRequestedEvent)
- ✅ Thymeleaf used for email templates
- ✅ Spring Mail for SMTP delivery
- ✅ EmailLog for audit trail
- ✅ File Service for attachments
- ✅ Async email sending (doesn't block HTTP request)
- ✅ AES encryption for SMTP passwords

