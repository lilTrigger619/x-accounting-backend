package com.unionsg.xaccounting.documenttemplate.service;

import com.unionsg.xaccounting.documenttemplate.dto.request.*;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateResponse;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentTemplateService {

    DocumentTemplateResponse createTemplate(CreateDocumentTemplateRequest request);

    DocumentTemplateResponse getTemplate(Long id);

    Page<DocumentTemplateResponse> listTemplates(DocumentType type, Pageable pageable);

    DocumentTemplateResponse updateTemplate(Long id, UpdateDocumentTemplateRequest request);

    void deleteTemplate(Long id);

    DocumentTemplateResponse updateDesign(Long id, UpdateDesignRequest request);

    DocumentTemplateResponse updateContent(Long id, UpdateContentRequest request);

    DocumentTemplateResponse updateStandardEmail(Long id, UpdateEmailRequest request);

    DocumentTemplateResponse updateReminderEmail(Long id, UpdateEmailRequest request);

    DocumentTemplateResponse setDefaultTemplate(Long id);
}

