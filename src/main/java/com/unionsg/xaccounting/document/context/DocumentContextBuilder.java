package com.unionsg.xaccounting.document.context;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateContent;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateDesign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Builds a DocumentContext from a business entity, template, and company info.
 * This is the bridge that assembles all data required by the renderer.
 */
@Component
@RequiredArgsConstructor
public class DocumentContextBuilder {

    private final CompanyInfoResolver companyInfoResolver;

    public DocumentContext build(Object document, DocumentTemplate template) {
        DocumentTemplateDesign design = template.getDesign();
        DocumentTemplateContent content = template.getContent();

        if (design == null) {
            throw new IllegalStateException("Template design not configured for template: " + template.getId());
        }
        if (content == null) {
            throw new IllegalStateException("Template content not configured for template: " + template.getId());
        }

        CompanyInfo company = companyInfoResolver.resolve();

        return DocumentContext.builder()
                .document(document)
                .template(template)
                .design(design)
                .content(content)
                .company(company)
                .variables(new HashMap<>())
                .build();
    }
}

