package com.unionsg.xaccounting.document.dto;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentGenerateRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotNull(message = "Entity ID is required")
    private Long entityId;
}

