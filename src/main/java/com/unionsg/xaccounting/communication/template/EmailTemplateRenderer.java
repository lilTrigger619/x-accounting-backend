package com.unionsg.xaccounting.communication.template;

import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private final TemplateEngine templateEngine;
    private final EmailVariableResolver variableResolver;

    /**
     * Renders the email body using a Thymeleaf template with resolved variables.
     *
     * @param templateName the Thymeleaf template name (e.g., "email/invoice-standard")
     * @param variables the variables to inject into the template
     * @return rendered HTML string
     */
    public String renderBody(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    /**
     * Resolves the subject from the email template using variable replacement.
     */
    public String resolveSubject(DocumentTemplateEmail emailTemplate, Map<String, Object> variables) {
        if (emailTemplate == null || emailTemplate.getSubject() == null) {
            return "";
        }
        return variableResolver.resolve(emailTemplate.getSubject(), variables);
    }

    /**
     * Resolves the greeting/salutation.
     */
    public String resolveSalutation(DocumentTemplateEmail emailTemplate, Map<String, Object> variables) {
        if (emailTemplate == null) return "";

        String salutation = emailTemplate.getSalutation() != null ? emailTemplate.getSalutation() : "Dear";
        String nameFormat = emailTemplate.getNameFormat() != null ? emailTemplate.getNameFormat() : "FIRST_LAST";

        String customerName = "";
        if ("FIRST_LAST".equalsIgnoreCase(nameFormat)) {
            String first = getVariable(variables, "customerFirstName");
            String last = getVariable(variables, "customerLastName");
            customerName = (first + " " + last).trim();
        } else if ("FULL".equalsIgnoreCase(nameFormat)) {
            customerName = getVariable(variables, "customerName");
        } else if ("COMPANY".equalsIgnoreCase(nameFormat)) {
            customerName = getVariable(variables, "companyName");
        } else {
            customerName = getVariable(variables, "customerName");
        }

        return salutation + " " + customerName;
    }

    private String getVariable(Map<String, Object> variables, String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : "";
    }
}
