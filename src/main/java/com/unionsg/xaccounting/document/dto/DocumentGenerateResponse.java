package com.unionsg.xaccounting.document.dto;

import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerateResponse {

    private String fileId;
    private DocumentType documentType;
    private LocalDateTime generatedAt;
}

