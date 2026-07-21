package com.unionsg.xaccounting.documenttemplate.dto.request;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDocumentTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotNull(message = "Layout is required")
    private DocumentLayout layout;
}

