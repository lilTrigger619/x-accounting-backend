package com.unionsg.xaccounting.documenttemplate.dto.response;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private DocumentType documentType;
    private DocumentLayout layout;
    private boolean isDefault;
    private TemplateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DocumentTemplateDesignResponse design;
    private DocumentTemplateContentResponse content;
    private List<DocumentTemplateEmailResponse> emails;
}

