package com.unionsg.xaccounting.document.template;

import com.unionsg.xaccounting.document.context.DocumentContext;
import com.unionsg.xaccounting.document.renderer.DocumentRenderer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.AbstractContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Locale;

/**
 * Renders HTML documents using Thymeleaf templates.
 * Receives a DocumentRenderer (strategy) and DocumentContext, then processes the template.
 *
 * <p>When invoked within an HTTP request, this builds a {@link WebContext} so that
 * context-relative link expressions (e.g. {@code @{/css/...}}) in the templates
 * resolve correctly. Outside of a request (e.g. in unit tests) it falls back to a
 * plain {@link Context}.</p>
 */
@Service
@RequiredArgsConstructor
public class ThymeleafDocumentRenderer {

    private final TemplateEngine templateEngine;

    public String render(DocumentRenderer renderer, DocumentContext context) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        AbstractContext thymeleafContext;
        String baseUrl = "";
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            HttpServletResponse response = attrs.getResponse();
            ServletContext servletContext = request.getServletContext();

            JakartaServletWebApplication application =
                    JakartaServletWebApplication.buildApplication(servletContext);
            IServletWebExchange exchange =
                    application.buildExchange(request, response);
            thymeleafContext = new WebContext(exchange, Locale.US);

            // Compute absolute base URL so templates can link static assets absolutely,
            // e.g. http://localhost:8080/css/documents/invoice/common.css
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();

            StringBuilder sb = new StringBuilder(scheme).append("://").append(serverName);
            boolean isHttps = "https".equalsIgnoreCase(scheme);
            if ((isHttps && serverPort != 443) || (!isHttps && serverPort != 80)) {
                sb.append(':').append(serverPort);
            }
            sb.append(contextPath);
            baseUrl = sb.toString();
        } else {
            thymeleafContext = new Context(Locale.US);
        }

        thymeleafContext.setVariable("document", context.getDocument());
        thymeleafContext.setVariable("template", context.getTemplate());
        thymeleafContext.setVariable("design", context.getDesign());
        thymeleafContext.setVariable("content", context.getContent());
        thymeleafContext.setVariable("company", context.getCompany());
        thymeleafContext.setVariable("variables", context.getVariables());
        thymeleafContext.setVariable("baseUrl", baseUrl);

        // Add invoice-specific shorthand for backward compatibility in templates
        thymeleafContext.setVariable("invoice", context.getDocument());

        return templateEngine.process(renderer.getTemplatePath(), thymeleafContext);
    }
}
