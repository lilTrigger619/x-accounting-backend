package com.unionsg.xaccounting.document.template;

import com.unionsg.xaccounting.document.context.DocumentContext;
import com.unionsg.xaccounting.document.renderer.DocumentRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Renders HTML documents using Thymeleaf templates.
 * Receives a DocumentRenderer (strategy) and DocumentContext, then processes the template.
 */
@Service
@RequiredArgsConstructor
public class ThymeleafDocumentRenderer {

    private final TemplateEngine templateEngine;

    public String render(DocumentRenderer renderer, DocumentContext context) {
        Context thymeleafContext = new Context(Locale.US);

        thymeleafContext.setVariable("document", context.getDocument());
        thymeleafContext.setVariable("template", context.getTemplate());
        thymeleafContext.setVariable("design", context.getDesign());
        thymeleafContext.setVariable("content", context.getContent());
        thymeleafContext.setVariable("company", context.getCompany());
        thymeleafContext.setVariable("variables", context.getVariables());

        // Add invoice-specific shorthand for backward compatibility in templates
        thymeleafContext.setVariable("invoice", context.getDocument());

        return templateEngine.process(renderer.getTemplatePath(), thymeleafContext);
    }
}

