package com.unionsg.xaccounting.documenttemplate.service;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.dto.request.*;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateResponse;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import com.unionsg.xaccounting.documenttemplate.exception.DocumentTemplateException;
import com.unionsg.xaccounting.documenttemplate.mapper.DocumentTemplateMapper;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateEmailRepository;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentTemplateServiceImpl implements DocumentTemplateService {

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateEmailRepository emailRepository;

    // =============================
    // Create Template
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse createTemplate(CreateDocumentTemplateRequest request) {
        // Validate unique name within DocumentType
        if (templateRepository.existsByNameAndDocumentType(request.getName(), request.getDocumentType())) {
            throw new DocumentTemplateException(
                    "A template with name '" + request.getName() + "' already exists for " + request.getDocumentType()
            );
        }

        DocumentTemplate template = DocumentTemplateMapper.toEntity(request);

        // Create default Design, Content, and Email records
        template.setDesign(DocumentTemplateMapper.createDefaultDesign(template));
        template.setContent(DocumentTemplateMapper.createDefaultContent(template));
        template.addEmail(DocumentTemplateMapper.createDefaultEmail(template, EmailType.STANDARD));
        template.addEmail(DocumentTemplateMapper.createDefaultEmail(template, EmailType.REMINDER));

        DocumentTemplate saved = templateRepository.save(template);

        return DocumentTemplateMapper.toResponse(saved);
    }

    // =============================
    // Get Template
    // =============================

    @Override
    @Transactional(readOnly = true)
    public DocumentTemplateResponse getTemplate(Long id) {
        DocumentTemplate template = findTemplateById(id);
        return DocumentTemplateMapper.toResponse(template);
    }

    // =============================
    // List Templates
    // =============================

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentTemplateResponse> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable)
                .map(DocumentTemplateMapper::toResponse);
    }

    // =============================
    // Update Template
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse updateTemplate(Long id, UpdateDocumentTemplateRequest request) {
        DocumentTemplate template = findTemplateById(id);

        // Check name uniqueness if name changed
        if (!template.getName().equals(request.getName())) {
            if (templateRepository.existsByNameAndDocumentType(request.getName(), template.getDocumentType())) {
                throw new DocumentTemplateException(
                        "A template with name '" + request.getName() + "' already exists for " + template.getDocumentType()
                );
            }
            template.setName(request.getName());
        }

        if (request.getLayout() != null) {
            template.setLayout(request.getLayout());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }

        DocumentTemplate saved = templateRepository.save(template);
        return DocumentTemplateMapper.toResponse(saved);
    }

    // =============================
    // Delete Template
    // =============================

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        DocumentTemplate template = findTemplateById(id);

        if (template.isDefault()) {
            throw new DocumentTemplateException("Cannot delete the default template. Set another template as default first.");
        }

        templateRepository.delete(template);
    }

    // =============================
    // Update Design
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse updateDesign(Long id, UpdateDesignRequest request) {
        DocumentTemplate template = findTemplateById(id);

        if (template.getDesign() == null) {
            throw new DocumentTemplateException("Template design not found");
        }

        DocumentTemplateMapper.applyDesignUpdate(template.getDesign(), request);

        DocumentTemplate saved = templateRepository.save(template);
        return DocumentTemplateMapper.toResponse(saved);
    }

    // =============================
    // Update Content
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse updateContent(Long id, UpdateContentRequest request) {
        DocumentTemplate template = findTemplateById(id);

        if (template.getContent() == null) {
            throw new DocumentTemplateException("Template content not found");
        }

        DocumentTemplateMapper.applyContentUpdate(template.getContent(), request);

        DocumentTemplate saved = templateRepository.save(template);
        return DocumentTemplateMapper.toResponse(saved);
    }

    // =============================
    // Update Standard Email
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse updateStandardEmail(Long id, UpdateEmailRequest request) {
        DocumentTemplate template = findTemplateById(id);

        DocumentTemplateEmail email = emailRepository
                .findByTemplateAndEmailType(template, EmailType.STANDARD)
                .orElseThrow(() -> new DocumentTemplateException("Standard email template not found"));

        DocumentTemplateMapper.applyEmailUpdate(email, request);

        emailRepository.save(email);

        return DocumentTemplateMapper.toResponse(templateRepository.findById(id)
                .orElseThrow(() -> new DocumentTemplateException("Template not found")));
    }

    // =============================
    // Update Reminder Email
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse updateReminderEmail(Long id, UpdateEmailRequest request) {
        DocumentTemplate template = findTemplateById(id);

        DocumentTemplateEmail email = emailRepository
                .findByTemplateAndEmailType(template, EmailType.REMINDER)
                .orElseThrow(() -> new DocumentTemplateException("Reminder email template not found"));

        DocumentTemplateMapper.applyEmailUpdate(email, request);

        emailRepository.save(email);

        return DocumentTemplateMapper.toResponse(templateRepository.findById(id)
                .orElseThrow(() -> new DocumentTemplateException("Template not found")));
    }

    // =============================
    // Set Default Template
    // =============================

    @Override
    @Transactional
    public DocumentTemplateResponse setDefaultTemplate(Long id) {
        DocumentTemplate template = findTemplateById(id);

        // Clear default for all other templates of the same DocumentType
        templateRepository.clearDefaultForOtherTemplates(template.getDocumentType(), template.getId());

        // Set this template as default
        template.setDefault(true);
        DocumentTemplate saved = templateRepository.save(template);

        return DocumentTemplateMapper.toResponse(saved);
    }

    // =============================
    // Private Helpers
    // =============================

    private DocumentTemplate findTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new DocumentTemplateException("Document template not found with id: " + id));
    }
}

