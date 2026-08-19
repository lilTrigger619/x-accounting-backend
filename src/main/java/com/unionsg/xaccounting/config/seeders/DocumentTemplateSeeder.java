package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateEmailRepository;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTemplateSeeder implements ApplicationRunner {

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateEmailRepository emailRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (templateRepository.existsByTemplateCode("INVOICE_CLASSIC")) {
            log.info("Document templates already seeded, skipping...");
            return;
        }

        log.info("Seeding default document templates...");

        createClassicTemplate();
        createModernTemplate();
        createProfessionalTemplate();

        log.info("Document template seeding complete.");
    }

    // =============================
    // CLASSIC Invoice
    // =============================

    private void createClassicTemplate() {
        DocumentTemplate template = new DocumentTemplate();
        template.setTemplateCode("INVOICE_CLASSIC");
        template.setName("Classic Invoice");
        template.setDescription("Traditional professional invoice layout");
        template.setDocumentType(DocumentType.INVOICE);
        template.setLayout(DocumentLayout.CLASSIC);
        template.setSystem(true);
        template.setDefault(false);
        template.setStatus(TemplateStatus.ACTIVE);

        // Design
        DocumentTemplateDesign design = DocumentTemplateDesign.builder()
                .template(template)
                .primaryColor("#000000")
                .secondaryColor("#666666")
                .accentColor("#eeeeee")
                .fontFamily("Arial")
                .fontSize(12)
                .fontColor("#000000")
                .logoPosition("LEFT")
                .logoSize("MEDIUM")
                .build();
        template.setDesign(design);

        // Content
        DocumentTemplateContent content = DocumentTemplateContent.builder()
                .template(template)
                .formTitle("INVOICE")
                .showCompanyName(true)
                .showPhone(true)
                .showEmail(true)
                .showWebsite(true)
                .showAddress(true)
                .showBillingAddress(true)
                .showShippingAddress(true)
                .showTerms(true)
                .showDueDate(true)
                .showPaymentMethod(true)
                .build();
        template.setContent(content);

        // Emails
        template.addEmail(createStandardEmail(template,
                "Invoice {{invoiceNumber}}",
                "Dear {{customerFirstName}},\n\n" +
                "Please find attached your invoice {{invoiceNumber}}.\n\n" +
                "Invoice Amount: {{total}}\n" +
                "Due Date: {{dueDate}}\n\n" +
                "Thank you for your business.\n\n" +
                "{{companyName}}"
        ));

        template.addEmail(createReminderEmail(template,
                "Reminder: Invoice {{invoiceNumber}} payment due",
                "Dear {{customerFirstName}},\n\n" +
                "This is a friendly reminder that invoice {{invoiceNumber}} remains unpaid.\n\n" +
                "Outstanding Amount: {{balanceDue}}\n\n" +
                "Please arrange payment at your earliest convenience.\n\n" +
                "{{companyName}}"
        ));

        templateRepository.save(template);
        log.info("Seeded template: {}", template.getTemplateCode());
    }

    // =============================
    // MODERN Invoice
    // =============================

    private void createModernTemplate() {
        DocumentTemplate template = new DocumentTemplate();
        template.setTemplateCode("INVOICE_MODERN");
        template.setName("Modern Invoice");
        template.setDescription("Clean modern invoice layout");
        template.setDocumentType(DocumentType.INVOICE);
        template.setLayout(DocumentLayout.MODERN);
        template.setSystem(true);
        template.setDefault(false);
        template.setStatus(TemplateStatus.ACTIVE);

        // Design
        DocumentTemplateDesign design = DocumentTemplateDesign.builder()
                .template(template)
                .primaryColor("#2563eb")
                .secondaryColor("#64748b")
                .accentColor("#eff6ff")
                .fontFamily("Inter")
                .fontSize(12)
                .fontColor("#111827")
                .logoPosition("RIGHT")
                .logoSize("MEDIUM")
                .build();
        template.setDesign(design);

        // Content
        DocumentTemplateContent content = DocumentTemplateContent.builder()
                .template(template)
                .formTitle("INVOICE")
                .showCompanyName(true)
                .showPhone(true)
                .showEmail(true)
                .showWebsite(true)
                .showAddress(true)
                .showBillingAddress(true)
                .showShippingAddress(true)
                .showTerms(true)
                .showDueDate(true)
                .showPaymentMethod(true)
                .build();
        template.setContent(content);

        // Emails
        template.addEmail(createStandardEmail(template,
                "Invoice {{invoiceNumber}}",
                "Dear {{customerFirstName}},\n\n" +
                "Please find attached your invoice {{invoiceNumber}}.\n\n" +
                "Invoice Amount: {{total}}\n" +
                "Due Date: {{dueDate}}\n\n" +
                "Thank you for your business.\n\n" +
                "{{companyName}}"
        ));

        template.addEmail(createReminderEmail(template,
                "Reminder: Invoice {{invoiceNumber}} payment due",
                "Dear {{customerFirstName}},\n\n" +
                "This is a friendly reminder that invoice {{invoiceNumber}} remains unpaid.\n\n" +
                "Outstanding Amount: {{balanceDue}}\n\n" +
                "Please arrange payment at your earliest convenience.\n\n" +
                "{{companyName}}"
        ));

        templateRepository.save(template);
        log.info("Seeded template: {}", template.getTemplateCode());
    }

    // =============================
    // PROFESSIONAL Invoice (default)
    // =============================

    private void createProfessionalTemplate() {
        DocumentTemplate template = new DocumentTemplate();
        template.setTemplateCode("INVOICE_PROFESSIONAL");
        template.setName("Professional Invoice");
        template.setDescription("Corporate invoice layout");
        template.setDocumentType(DocumentType.INVOICE);
        template.setLayout(DocumentLayout.PROFESSIONAL);
        template.setSystem(true);
        template.setDefault(true);  // This is the default template
        template.setStatus(TemplateStatus.ACTIVE);

        // Design
        DocumentTemplateDesign design = DocumentTemplateDesign.builder()
                .template(template)
                .primaryColor("#111827")
                .secondaryColor("#374151")
                .accentColor("#f3f4f6")
                .fontFamily("Helvetica")
                .fontSize(11)
                .fontColor("#111827")
                .logoPosition("CENTER")
                .logoSize("LARGE")
                .build();
        template.setDesign(design);

        // Content
        DocumentTemplateContent content = DocumentTemplateContent.builder()
                .template(template)
                .formTitle("INVOICE")
                .showCompanyName(true)
                .showPhone(true)
                .showEmail(true)
                .showWebsite(true)
                .showAddress(true)
                .showBillingAddress(true)
                .showShippingAddress(true)
                .showTerms(true)
                .showDueDate(true)
                .showPaymentMethod(true)
                .build();
        template.setContent(content);

        // Emails
        template.addEmail(createStandardEmail(template,
                "Invoice {{invoiceNumber}}",
                "Dear {{customerFirstName}},\n\n" +
                "Please find attached your invoice {{invoiceNumber}}.\n\n" +
                "Invoice Amount: {{total}}\n" +
                "Due Date: {{dueDate}}\n\n" +
                "Thank you for your business.\n\n" +
                "{{companyName}}"
        ));

        template.addEmail(createReminderEmail(template,
                "Reminder: Invoice {{invoiceNumber}} payment due",
                "Dear {{customerFirstName}},\n\n" +
                "This is a friendly reminder that invoice {{invoiceNumber}} remains unpaid.\n\n" +
                "Outstanding Amount: {{balanceDue}}\n\n" +
                "Please arrange payment at your earliest convenience.\n\n" +
                "{{companyName}}"
        ));

        templateRepository.save(template);
        log.info("Seeded template: {}", template.getTemplateCode());
    }

    // =============================
    // Email Helpers
    // =============================

    private DocumentTemplateEmail createStandardEmail(DocumentTemplate template, String subject, String body) {
        DocumentTemplateEmail email = new DocumentTemplateEmail();
        email.setTemplate(template);
        email.setEmailType(EmailType.STANDARD);
        email.setSubject(subject);
        email.setUseGreeting(true);
        email.setSalutation("DEAR");
        email.setNameFormat("FIRST_NAME");
        email.setBody(body);
        return email;
    }

    private DocumentTemplateEmail createReminderEmail(DocumentTemplate template, String subject, String body) {
        DocumentTemplateEmail email = new DocumentTemplateEmail();
        email.setTemplate(template);
        email.setEmailType(EmailType.REMINDER);
        email.setSubject(subject);
        email.setUseGreeting(true);
        email.setSalutation("DEAR");
        email.setNameFormat("FIRST_NAME");
        email.setBody(body);
        return email;
    }
}

