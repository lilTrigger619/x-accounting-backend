package com.unionsg.xaccounting.documenttemplate.mapper;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.dto.request.CreateDocumentTemplateRequest;
import com.unionsg.xaccounting.documenttemplate.dto.request.UpdateContentRequest;
import com.unionsg.xaccounting.documenttemplate.dto.request.UpdateDesignRequest;
import com.unionsg.xaccounting.documenttemplate.dto.request.UpdateEmailRequest;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateContentResponse;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateDesignResponse;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateEmailResponse;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateResponse;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;

import java.util.stream.Collectors;

public class DocumentTemplateMapper {

    // =============================
    // Entity → DTO
    // =============================

    public static DocumentTemplateResponse toResponse(DocumentTemplate template) {
        if (template == null) {
            return null;
        }

        DocumentTemplateResponse response = new DocumentTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setDescription(template.getDescription());
        response.setDocumentType(template.getDocumentType());
        response.setLayout(template.getLayout());
        response.setDefault(template.isDefault());
        response.setStatus(template.getStatus());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());

        if (template.getDesign() != null) {
            response.setDesign(toDesignResponse(template.getDesign()));
        }

        if (template.getContent() != null) {
            response.setContent(toContentResponse(template.getContent()));
        }

        if (template.getEmails() != null) {
            response.setEmails(
                    template.getEmails().stream()
                            .map(DocumentTemplateMapper::toEmailResponse)
                            .collect(Collectors.toList())
            );
        }

        return response;
    }

    public static DocumentTemplateDesignResponse toDesignResponse(DocumentTemplateDesign design) {
        if (design == null) {
            return null;
        }

        DocumentTemplateDesignResponse response = new DocumentTemplateDesignResponse();
        response.setId(design.getId());
        response.setLogoFileId(design.getLogoFileId());
        response.setLogoPosition(design.getLogoPosition());
        response.setLogoWidth(design.getLogoWidth());
        response.setLogoHeight(design.getLogoHeight());
        response.setPrimaryColor(design.getPrimaryColor());
        response.setSecondaryColor(design.getSecondaryColor());
        response.setFontFamily(design.getFontFamily());
        response.setFontSize(design.getFontSize());

        return response;
    }

    public static DocumentTemplateContentResponse toContentResponse(DocumentTemplateContent content) {
        if (content == null) {
            return null;
        }

        DocumentTemplateContentResponse response = new DocumentTemplateContentResponse();
        response.setId(content.getId());
        response.setFormTitle(content.getFormTitle());
        response.setShowCompanyName(content.isShowCompanyName());
        response.setShowPhone(content.isShowPhone());
        response.setShowEmail(content.isShowEmail());
        response.setShowWebsite(content.isShowWebsite());
        response.setShowAddress(content.isShowAddress());
        response.setShowBillingAddress(content.isShowBillingAddress());
        response.setShowShippingAddress(content.isShowShippingAddress());
        response.setShowTerms(content.isShowTerms());
        response.setShowDueDate(content.isShowDueDate());
        response.setShowPaymentMethod(content.isShowPaymentMethod());

        return response;
    }

    public static DocumentTemplateEmailResponse toEmailResponse(DocumentTemplateEmail email) {
        if (email == null) {
            return null;
        }

        DocumentTemplateEmailResponse response = new DocumentTemplateEmailResponse();
        response.setId(email.getId());
        response.setEmailType(email.getEmailType());
        response.setSubject(email.getSubject());
        response.setUseGreeting(email.isUseGreeting());
        response.setSalutation(email.getSalutation());
        response.setNameFormat(email.getNameFormat());
        response.setBody(email.getBody());

        return response;
    }

    // =============================
    // Request → Entity (updates)
    // =============================

    public static DocumentTemplate toEntity(CreateDocumentTemplateRequest request) {
        DocumentTemplate template = new DocumentTemplate();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setDocumentType(request.getDocumentType());
        template.setLayout(request.getLayout());
        template.setDefault(false);
        template.setStatus(TemplateStatus.ACTIVE);
        return template;
    }

    public static void applyDesignUpdate(DocumentTemplateDesign design, UpdateDesignRequest request) {
        if (design == null || request == null) {
            return;
        }

        if (request.getLogoFileId() != null) {
            design.setLogoFileId(request.getLogoFileId());
        }
        if (request.getLogoPosition() != null) {
            design.setLogoPosition(request.getLogoPosition());
        }
        if (request.getLogoWidth() != null) {
            design.setLogoWidth(request.getLogoWidth());
        }
        if (request.getLogoHeight() != null) {
            design.setLogoHeight(request.getLogoHeight());
        }
        if (request.getPrimaryColor() != null) {
            design.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            design.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getFontFamily() != null) {
            design.setFontFamily(request.getFontFamily());
        }
        if (request.getFontSize() != null) {
            design.setFontSize(request.getFontSize());
        }
    }

    public static void applyContentUpdate(DocumentTemplateContent content, UpdateContentRequest request) {
        if (content == null || request == null) {
            return;
        }

        if (request.getFormTitle() != null) {
            content.setFormTitle(request.getFormTitle());
        }
        if (request.getShowCompanyName() != null) {
            content.setShowCompanyName(request.getShowCompanyName());
        }
        if (request.getShowPhone() != null) {
            content.setShowPhone(request.getShowPhone());
        }
        if (request.getShowEmail() != null) {
            content.setShowEmail(request.getShowEmail());
        }
        if (request.getShowWebsite() != null) {
            content.setShowWebsite(request.getShowWebsite());
        }
        if (request.getShowAddress() != null) {
            content.setShowAddress(request.getShowAddress());
        }
        if (request.getShowBillingAddress() != null) {
            content.setShowBillingAddress(request.getShowBillingAddress());
        }
        if (request.getShowShippingAddress() != null) {
            content.setShowShippingAddress(request.getShowShippingAddress());
        }
        if (request.getShowTerms() != null) {
            content.setShowTerms(request.getShowTerms());
        }
        if (request.getShowDueDate() != null) {
            content.setShowDueDate(request.getShowDueDate());
        }
        if (request.getShowPaymentMethod() != null) {
            content.setShowPaymentMethod(request.getShowPaymentMethod());
        }
    }

    public static void applyEmailUpdate(DocumentTemplateEmail email, UpdateEmailRequest request) {
        if (email == null || request == null) {
            return;
        }

        if (request.getSubject() != null) {
            email.setSubject(request.getSubject());
        }
        if (request.getUseGreeting() != null) {
            email.setUseGreeting(request.getUseGreeting());
        }
        if (request.getSalutation() != null) {
            email.setSalutation(request.getSalutation());
        }
        if (request.getNameFormat() != null) {
            email.setNameFormat(request.getNameFormat());
        }
        if (request.getBody() != null) {
            email.setBody(request.getBody());
        }
    }

    // =============================
    // Factory for default child entities
    // =============================

    public static DocumentTemplateDesign createDefaultDesign(DocumentTemplate template) {
        DocumentTemplateDesign design = new DocumentTemplateDesign();
        design.setTemplate(template);
        design.setLogoPosition("top-left");
        design.setLogoWidth(120);
        design.setLogoHeight(60);
        design.setPrimaryColor("#1a73e8");
        design.setSecondaryColor("#34a853");
        design.setFontFamily("Arial, sans-serif");
        design.setFontSize(12);
        return design;
    }

    public static DocumentTemplateContent createDefaultContent(DocumentTemplate template) {
        DocumentTemplateContent content = new DocumentTemplateContent();
        content.setTemplate(template);
        content.setFormTitle("Invoice");
        content.setShowCompanyName(true);
        content.setShowPhone(true);
        content.setShowEmail(true);
        content.setShowWebsite(true);
        content.setShowAddress(true);
        content.setShowBillingAddress(true);
        content.setShowShippingAddress(true);
        content.setShowTerms(true);
        content.setShowDueDate(true);
        content.setShowPaymentMethod(true);
        return content;
    }

    public static DocumentTemplateEmail createDefaultEmail(DocumentTemplate template, EmailType emailType) {
        DocumentTemplateEmail email = new DocumentTemplateEmail();
        email.setTemplate(template);
        email.setEmailType(emailType);
        email.setUseGreeting(true);
        email.setSalutation("Dear");
        email.setNameFormat("FIRST_LAST");

        if (emailType == EmailType.STANDARD) {
            email.setSubject("Invoice {{invoice_number}} from {{company_name}}");
            email.setBody("Dear {{customer_name}},\n\nPlease find attached invoice {{invoice_number}} for your reference.\n\nTotal Amount: {{total_amount}}\nDue Date: {{due_date}}\n\nThank you for your business.");
        } else {
            email.setSubject("Reminder: Invoice {{invoice_number}} is due");
            email.setBody("Dear {{customer_name}},\n\nThis is a reminder that invoice {{invoice_number}} for {{total_amount}} is due on {{due_date}}.\n\nPlease arrange payment at your earliest convenience.\n\nThank you.");
        }

        return email;
    }
}

