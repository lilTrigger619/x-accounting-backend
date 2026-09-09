package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.communication.template.EmailTemplateRenderer;
import com.unionsg.xaccounting.communication.template.EmailVariableResolver;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateEmailRepository;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves the subject + HTML body for an invoice email, either from the chosen template's
 * configured email content (DocumentTemplateEmail) or from user-supplied overrides.
 * Shared by the actual send flow and the preview endpoint so both render identically.
 */
@Service
@RequiredArgsConstructor
public class InvoiceEmailContentService {

    private final DocumentTemplateEmailRepository emailTemplateRepository;
    private final EmailVariableResolver variableResolver;
    private final EmailTemplateRenderer emailRenderer;

    public InvoiceEmailContent resolveContent(
            Invoice invoice,
            DocumentTemplate template,
            String subjectOverride,
            String messageOverride
    ) {
        Customer customer = invoice.getCustomer();
        String customerName = customer != null
                ? (customer.getDisplayName() != null ? customer.getDisplayName()
                        : ((customer.getFirstName() != null ? customer.getFirstName() : "") + " " + (customer.getLastName() != null ? customer.getLastName() : "")).trim())
                : "";

        Map<String, Object> variables = variableResolver.buildInvoiceVariables(
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                customerName,
                customer != null ? customer.getFirstName() : "",
                customer != null ? customer.getLastName() : "",
                "Your Company", // TODO: resolve from CompanyInfoResolver
                invoice.getTotalAmount(),
                invoice.getTotalDue(),
                "Net 30" // TODO: resolve from payment terms
        );

        DocumentTemplateEmail emailTemplate = template != null
                ? emailTemplateRepository.findByTemplateAndEmailType(template, EmailType.STANDARD).orElse(null)
                : null;

        String salutation = emailTemplate != null
                ? emailRenderer.resolveSalutation(emailTemplate, variables)
                : ("Dear " + customerName).trim();
        variables.put("salutation", salutation);

        String subject;
        if (subjectOverride != null && !subjectOverride.isBlank()) {
            subject = variableResolver.resolve(subjectOverride, variables);
        } else if (emailTemplate != null) {
            subject = emailRenderer.resolveSubject(emailTemplate, variables);
        } else {
            subject = "Invoice " + invoice.getInvoiceNumber();
        }

        String bodyHtml;
        if (messageOverride != null && !messageOverride.isBlank()) {
            bodyHtml = "<p>" + salutation + ",</p><p>" + toHtmlParagraph(variableResolver.resolve(messageOverride, variables)) + "</p>";
        } else if (emailTemplate != null) {
            bodyHtml = emailRenderer.renderBody("email/invoice-standard", variables);
        } else {
            bodyHtml = "<p>" + salutation + ",</p>"
                    + "<p>Please find attached invoice " + invoice.getInvoiceNumber() + " for your reference.</p>"
                    + "<p>Total Amount: " + variables.get("total") + "</p>"
                    + "<p>Due Date: " + variables.get("dueDate") + "</p>";
        }

        return new InvoiceEmailContent(subject, bodyHtml);
    }

    private String toHtmlParagraph(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
    }
}
