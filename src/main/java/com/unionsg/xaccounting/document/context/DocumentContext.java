package com.unionsg.xaccounting.document.context;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic rendering context for document generation.
 * Everything the renderer needs is pre-loaded here — no repository calls inside renderers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContext {

    private Object document;

    private DocumentTemplate template;

    private DocumentTemplateDesign design;

    private DocumentTemplateContent content;

    private CompanyInfo company;

    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();
}

