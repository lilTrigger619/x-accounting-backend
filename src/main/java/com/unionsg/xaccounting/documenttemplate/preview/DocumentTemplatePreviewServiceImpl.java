package com.unionsg.xaccounting.documenttemplate.preview;

import com.unionsg.xaccounting.document.context.DocumentContext;
import com.unionsg.xaccounting.document.context.DocumentContextBuilder;
import com.unionsg.xaccounting.document.renderer.DocumentRenderer;
import com.unionsg.xaccounting.document.renderer.DocumentRendererFactory;
import com.unionsg.xaccounting.document.template.ThymeleafDocumentRenderer;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import com.unionsg.xaccounting.documenttemplate.dto.request.SamplePreviewRequest;
import com.unionsg.xaccounting.documenttemplate.dto.request.UpdateContentRequest;
import com.unionsg.xaccounting.documenttemplate.dto.request.UpdateDesignRequest;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;
import com.unionsg.xaccounting.documenttemplate.mapper.DocumentTemplateMapper;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import com.unionsg.xaccounting.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Generates a sample invoice preview for the Document Template Designer.
 *
 * <p>The flow reuses the SAME rendering pipeline as the real invoice:
 * {@link DocumentContextBuilder} → {@link DocumentRendererFactory} →
 * {@link ThymeleafDocumentRenderer}. The only difference is that the document
 * model is a fabricated in-memory {@link com.unionsg.xaccounting.entity.invoice.Invoice}
 * produced by {@link InvoiceSampleDataProvider}, and the template configuration is
 * a transient copy overlaid with the current unsaved designer state.</p>
 *
 * <p>This service is deliberately NOT transactional and never calls any
 * repository save method, so no database records are created.</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentTemplatePreviewServiceImpl implements DocumentTemplatePreviewService {

    private final DocumentTemplateRepository templateRepository;
    private final DocumentContextBuilder contextBuilder;
    private final DocumentRendererFactory rendererFactory;
    private final ThymeleafDocumentRenderer thymeleafRenderer;
    private final InvoiceSampleDataProvider invoiceSampleDataProvider;

    @Override
    public String samplePreview(Long templateId, SamplePreviewRequest request) {
        // 1. Retrieve and validate the template
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BadRequestException("Document template not found with id: " + templateId));

if (Boolean.TRUE.equals(template.getDeleted())) {
            throw new BadRequestException("Document template not found with id: " + templateId);
        }

        if (template.getDocumentType() != DocumentType.INVOICE) {
            throw new BadRequestException(
                    "Sample preview is only supported for INVOICE templates. Template '" + template.getName()
                            + "' is of type " + template.getDocumentType() + "."
            );
        }

        // 2. Build a transient template copy with the unsaved designer configuration applied
        DocumentTemplate previewTemplate = buildPreviewTemplate(template, request);

        // 3. Build in-memory sample invoice data
        Object sampleInvoice = resolveSampleData(template.getDocumentType());

        // 4. Build the rendering context (design/content/company resolved from previewTemplate)
        DocumentContext context = contextBuilder.build(sampleInvoice, previewTemplate);

        // 5. Resolve layout renderer and render HTML
        DocumentLayout layout = previewTemplate.getLayout() != null
                ? previewTemplate.getLayout()
                : DocumentLayout.CLASSIC;
        DocumentRenderer renderer = rendererFactory.getRenderer(layout);

        return thymeleafRenderer.render(renderer, context);
    }

    // =============================
    // Private Helpers
    // =============================

    private Object resolveSampleData(DocumentType documentType) {
        if (documentType == DocumentType.INVOICE) {
            return invoiceSampleDataProvider.buildSampleData();
        }
        throw new BadRequestException("No sample data provider available for document type: " + documentType);
    }

    /**
     * Creates a transient, in-memory copy of the template (and its design/content)
     * so the unsaved designer state can be applied WITHOUT mutating the persisted
     * template. The copy is never saved.
     */
    private DocumentTemplate buildPreviewTemplate(DocumentTemplate source, SamplePreviewRequest request) {
        DocumentTemplate copy = new DocumentTemplate();
        copy.setId(source.getId());
        copy.setTemplateCode(source.getTemplateCode());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setDocumentType(source.getDocumentType());
        copy.setSystem(source.isSystem());
        copy.setDefault(source.isDefault());
        copy.setStatus(source.getStatus() != null ? source.getStatus() : TemplateStatus.ACTIVE);

        // Layout: prefer the unsaved request layout, fall back to the saved layout
        DocumentLayout layout = request != null && request.getLayout() != null
                ? request.getLayout()
                : source.getLayout();
        copy.setLayout(layout);

        // Design: clone the saved design and apply unsaved overrides
        copy.setDesign(cloneDesign(source.getDesign()));
        if (request != null) {
            applyDesignOverrides(copy.getDesign(), request.getDesign());
        }

        // Content: clone the saved content and apply unsaved overrides
        copy.setContent(cloneContent(source.getContent()));
        if (request != null) {
            applyContentOverrides(copy.getContent(), request.getContent());
        }

        return copy;
    }

    private DocumentTemplateDesign cloneDesign(DocumentTemplateDesign design) {
        if (design == null) {
            return DocumentTemplateMapper.createDefaultDesign(null);
        }
        DocumentTemplateDesign clone = new DocumentTemplateDesign();
        clone.setLogoFileId(design.getLogoFileId());
        clone.setLogoPosition(design.getLogoPosition());
        clone.setLogoWidth(design.getLogoWidth());
        clone.setLogoHeight(design.getLogoHeight());
        clone.setPrimaryColor(design.getPrimaryColor());
        clone.setSecondaryColor(design.getSecondaryColor());
        clone.setAccentColor(design.getAccentColor());
        clone.setFontFamily(design.getFontFamily());
        clone.setFontSize(design.getFontSize());
        clone.setFontColor(design.getFontColor());
        clone.setLogoSize(design.getLogoSize());
        return clone;
    }

    private DocumentTemplateContent cloneContent(DocumentTemplateContent content) {
        if (content == null) {
            return DocumentTemplateMapper.createDefaultContent(null);
        }
        DocumentTemplateContent clone = new DocumentTemplateContent();
        clone.setFormTitle(content.getFormTitle());
        clone.setShowCompanyName(content.isShowCompanyName());
        clone.setShowPhone(content.isShowPhone());
        clone.setShowEmail(content.isShowEmail());
        clone.setShowWebsite(content.isShowWebsite());
        clone.setShowAddress(content.isShowAddress());
        clone.setShowBillingAddress(content.isShowBillingAddress());
        clone.setShowShippingAddress(content.isShowShippingAddress());
        clone.setShowTerms(content.isShowTerms());
        clone.setShowDueDate(content.isShowDueDate());
        clone.setShowPaymentMethod(content.isShowPaymentMethod());
        return clone;
    }

    private void applyDesignOverrides(DocumentTemplateDesign design, UpdateDesignRequest request) {
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
        if (request.getFontColor() != null) {
            design.setFontColor(request.getFontColor());
        }
    }

    private void applyContentOverrides(DocumentTemplateContent content, UpdateContentRequest request) {
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
}
