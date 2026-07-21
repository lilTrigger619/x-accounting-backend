package com.unionsg.xaccounting.documenttemplate.dto.request;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentLayout;
import com.unionsg.xaccounting.documenttemplate.enums.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateDocumentTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private DocumentLayout layout;

    private TemplateStatus status;
}

