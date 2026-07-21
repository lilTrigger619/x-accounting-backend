package com.unionsg.xaccounting.document.service;

import com.unionsg.xaccounting.document.context.DocumentContext;
import com.unionsg.xaccounting.document.context.DocumentContextBuilder;
import com.unionsg.xaccounting.document.ByteArrayMultipartFile;
import com.unionsg.xaccounting.document.dto.DocumentGenerateResponse;
import com.unionsg.xaccounting.document.entity.GeneratedDocument;
import com.unionsg.xaccounting.document.pdf.PdfGenerationService;
import com.unionsg.xaccounting.document.renderer.DocumentRenderer;
import com.unionsg.xaccounting.document.renderer.DocumentRendererFactory;
import com.unionsg.xaccounting.document.repository.GeneratedDocumentRepository;
import com.unionsg.xaccounting.document.template.ThymeleafDocumentRenderer;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.dto.FileResponseDto;
import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.enums.EntityType;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.FileService.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates the full document generation pipeline:
 * 1. Receive document request
 * 2. Load entity (done by caller/bridge service)
 * 3. Build DocumentContext
 * 4. Resolve renderer
 * 5. Generate HTML
 * 6. Generate PDF
 * 7. Store PDF using File Module
 * 8. Save GeneratedDocument
 * 9. Return result
 */
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private final DocumentContextBuilder contextBuilder;
    private final DocumentRendererFactory rendererFactory;
    private final ThymeleafDocumentRenderer thymeleafRenderer;
    private final PdfGenerationService pdfGenerationService;
    private final FileService fileService;
    private final GeneratedDocumentRepository generatedDocumentRepository;

    /**
     * Generates a PDF document for the given entity and template.
     * Saves the PDF via the File Upload module and tracks it in GeneratedDocument.
     */
    @Transactional
    public DocumentGenerateResponse generate(
            Object document,
            DocumentTemplate template,
            DocumentType documentType,
            String entityTypeName,
            Long entityId
    ) {
        // Build context
        DocumentContext context = contextBuilder.build(document, template);

        // Resolve renderer
        DocumentLayout layout = template.getLayout() != null ? template.getLayout() : DocumentLayout.CLASSIC;
        DocumentRenderer renderer = rendererFactory.getRenderer(layout);

        // Generate HTML
        String html = thymeleafRenderer.render(renderer, context);

        // Generate PDF
        byte[] pdfBytes = pdfGenerationService.generatePdf(html);

        // Store via File Module
        UUID currentUserId = SecurityUtils.getCurrentUser() != null
                ? SecurityUtils.getCurrentUser().getId()
                : UUID.randomUUID();

        String fileName = documentType.name() + "_" + entityId + "_" + System.currentTimeMillis() + ".pdf";

        MultipartFile multipartFile = new ByteArrayMultipartFile(
                "file",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );

        FileUploadRequestDto uploadRequest = FileUploadRequestDto.builder()
                .entityType(EntityType.GENERATED_DOCUMENT)
                .entityId(entityId.toString())
                .description("Auto-generated " + documentType.name() + " document")
                .uploadedBy(currentUserId)
                .build();

        List<FileResponseDto> uploadedFiles = fileService.uploadFile(
                new MultipartFile[]{multipartFile},
                uploadRequest
        );

        if (uploadedFiles.isEmpty()) {
            throw new RuntimeException("Failed to store generated PDF");
        }

        String fileId = uploadedFiles.get(0).getId().toString();

        // Save generated document tracking record
        GeneratedDocument generatedDoc = GeneratedDocument.builder()
                .documentType(documentType)
                .entityType(entityTypeName)
                .entityId(entityId)
                .templateId(template.getId())
                .templateVersion(template.getVersion() != null ? template.getVersion() : 0L)
                .fileId(fileId)
                .generatedAt(LocalDateTime.now())
                .generatedBy(currentUserId.toString())
                .build();

        generatedDocumentRepository.save(generatedDoc);

        return DocumentGenerateResponse.builder()
                .fileId(fileId)
                .documentType(documentType)
                .generatedAt(generatedDoc.getGeneratedAt())
                .build();
    }

    /**
     * Generates a preview PDF without saving anything.
     */
    public byte[] preview(Object document, DocumentTemplate template) {
        DocumentContext context = contextBuilder.build(document, template);

        DocumentLayout layout = template.getLayout() != null ? template.getLayout() : DocumentLayout.CLASSIC;
        DocumentRenderer renderer = rendererFactory.getRenderer(layout);

        String html = thymeleafRenderer.render(renderer, context);

        return pdfGenerationService.generatePdf(html);
    }
}

