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
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import com.unionsg.xaccounting.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTemplatePreviewServiceImplTest {

    @Mock
    private DocumentTemplateRepository templateRepository;

    @Mock
    private DocumentContextBuilder contextBuilder;

    @Mock
    private DocumentRendererFactory rendererFactory;

    @Mock
    private ThymeleafDocumentRenderer thymeleafRenderer;

    @Mock
    private InvoiceSampleDataProvider invoiceSampleDataProvider;

    @Mock
    private DocumentRenderer renderer;

    private DocumentTemplatePreviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentTemplatePreviewServiceImpl(
                templateRepository,
                contextBuilder,
                rendererFactory,
                thymeleafRenderer,
                invoiceSampleDataProvider
        );
    }

    private DocumentTemplate invoiceTemplate() {
        DocumentTemplate template = new DocumentTemplate();
        template.setId(1L);
        template.setTemplateCode("TMPL-INV-00001");
        template.setName("Default Invoice");
        template.setDocumentType(DocumentType.INVOICE);
        template.setLayout(DocumentLayout.CLASSIC);
        template.setStatus(TemplateStatus.ACTIVE);
        template.setDesign(design());
        template.setContent(content());
        return template;
    }

    private DocumentTemplateDesign design() {
        DocumentTemplateDesign design = new DocumentTemplateDesign();
        design.setPrimaryColor("#1a73e8");
        design.setSecondaryColor("#34a853");
        design.setFontFamily("Arial, sans-serif");
        design.setFontSize(12);
        design.setLogoPosition("top-left");
        return design;
    }

    private DocumentTemplateContent content() {
        DocumentTemplateContent content = new DocumentTemplateContent();
        content.setFormTitle("Invoice");
        content.setShowBillingAddress(true);
        content.setShowDueDate(true);
        content.setShowPaymentMethod(true);
        return content;
    }

    @Test
    void existingTemplateGeneratesSamplePreview() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(invoiceTemplate()));
        when(invoiceSampleDataProvider.buildSampleData()).thenReturn(new Object());
        when(contextBuilder.build(any(), any())).thenReturn(DocumentContext.builder().build());
        when(rendererFactory.getRenderer(DocumentLayout.CLASSIC)).thenReturn(renderer);
        when(thymeleafRenderer.render(any(), any())).thenReturn("<html>sample invoice</html>");

        String html = service.samplePreview(1L, new SamplePreviewRequest());

        assertThat(html).contains("sample invoice");
    }

    @Test
    void nonexistentTemplateThrowsBadRequest() {
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.samplePreview(999L, new SamplePreviewRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void nonInvoiceTemplateIsRejected() {
        DocumentTemplate template = invoiceTemplate();
        template.setDocumentType(DocumentType.QUOTE);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> service.samplePreview(1L, new SamplePreviewRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("INVOICE");
    }

    @Test
    void unsavedConfigAffectsRenderedHtml() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(invoiceTemplate()));
        when(invoiceSampleDataProvider.buildSampleData()).thenReturn(new Object());

        // Capture the template passed to the context builder so we can inspect applied overrides
        when(contextBuilder.build(any(), any())).thenAnswer(invocation -> DocumentContext.builder().build());
        when(rendererFactory.getRenderer(DocumentLayout.MODERN)).thenReturn(renderer);
        when(renderer.getTemplatePath()).thenReturn("documents/invoice/modern");
        when(thymeleafRenderer.render(any(), any())).thenAnswer(invocation -> {
            DocumentRenderer r = invocation.getArgument(0);
            return "rendered:" + r.getTemplatePath();
        });

        SamplePreviewRequest request = new SamplePreviewRequest();
        request.setLayout(DocumentLayout.MODERN);
        UpdateDesignRequest design = new UpdateDesignRequest();
        design.setPrimaryColor("#FF0000");
        request.setDesign(design);
        UpdateContentRequest contentReq = new UpdateContentRequest();
        contentReq.setFormTitle("TAX INVOICE");
        request.setContent(contentReq);

        String html = service.samplePreview(1L, request);

        assertThat(html).contains("modern");

        // Verify overrides were applied to the transient template passed to the context builder
        ArgumentCaptor<DocumentTemplate> templateCaptor = ArgumentCaptor.forClass(DocumentTemplate.class);
        verify(contextBuilder).build(any(), templateCaptor.capture());
        DocumentTemplate previewTemplate = templateCaptor.getValue();
        assertThat(previewTemplate.getLayout()).isEqualTo(DocumentLayout.MODERN);
        assertThat(previewTemplate.getDesign().getPrimaryColor()).isEqualTo("#FF0000");
        assertThat(previewTemplate.getContent().getFormTitle()).isEqualTo("TAX INVOICE");
    }

    @Test
    void fontAndLogoDesignOverridesAreAppliedToPreviewTemplate() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(invoiceTemplate()));
        when(invoiceSampleDataProvider.buildSampleData()).thenReturn(new Object());
        when(contextBuilder.build(any(), any())).thenReturn(DocumentContext.builder().build());
        when(rendererFactory.getRenderer(DocumentLayout.CLASSIC)).thenReturn(renderer);
        when(thymeleafRenderer.render(any(), any())).thenReturn("<html/>");

        SamplePreviewRequest request = new SamplePreviewRequest();
        UpdateDesignRequest design = new UpdateDesignRequest();
        design.setFontFamily("Roboto");
        design.setFontSize(18);
        design.setFontColor("#FF0000");
        design.setLogoWidth(220);
        design.setLogoHeight(100);
        request.setDesign(design);

        service.samplePreview(1L, request);

        ArgumentCaptor<DocumentTemplate> templateCaptor = ArgumentCaptor.forClass(DocumentTemplate.class);
        verify(contextBuilder).build(any(), templateCaptor.capture());
        DocumentTemplateDesign previewDesign = templateCaptor.getValue().getDesign();

        assertThat(previewDesign.getFontFamily()).isEqualTo("Roboto");
        assertThat(previewDesign.getFontSize()).isEqualTo(18);
        assertThat(previewDesign.getFontColor()).isEqualTo("#FF0000");
        assertThat(previewDesign.getLogoWidth()).isEqualTo(220);
        assertThat(previewDesign.getLogoHeight()).isEqualTo(100);
    }

    @Test
    void samplePreviewDoesNotPersistAnything() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(invoiceTemplate()));
        when(invoiceSampleDataProvider.buildSampleData()).thenReturn(new Object());
        when(contextBuilder.build(any(), any())).thenReturn(DocumentContext.builder().build());
        when(rendererFactory.getRenderer(DocumentLayout.CLASSIC)).thenReturn(renderer);
        when(thymeleafRenderer.render(any(), any())).thenReturn("<html/>");

        service.samplePreview(1L, new SamplePreviewRequest());

        // The original template must never be saved/mutated by the preview flow.
        verify(templateRepository, never()).save(any());
        verify(templateRepository, never()).saveAndFlush(any());
    }
}
