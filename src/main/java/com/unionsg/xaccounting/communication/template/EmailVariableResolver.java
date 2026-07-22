package com.unionsg.xaccounting.communication.template;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailVariableResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    /**
     * Resolves all supported variables in a subject or body string.
     *
     * Supported variables:
     * {{invoiceNumber}}, {{invoiceDate}}, {{dueDate}}, {{customerName}},
     * {{customerFirstName}}, {{customerLastName}}, {{companyName}},
     * {{total}}, {{balanceDue}}, {{paymentTerms}}
     */
    public String resolve(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Builds a standard set of variables for invoice emails.
     */
    public Map<String, Object> buildInvoiceVariables(
            String invoiceNumber,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String customerName,
            String customerFirstName,
            String customerLastName,
            String companyName,
            BigDecimal total,
            BigDecimal balanceDue,
            String paymentTerms
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("invoiceNumber", invoiceNumber != null ? invoiceNumber : "");
        variables.put("invoiceDate", invoiceDate != null ? invoiceDate.format(DATE_FORMATTER) : "");
        variables.put("dueDate", dueDate != null ? dueDate.format(DATE_FORMATTER) : "");
        variables.put("customerName", customerName != null ? customerName : "");
        variables.put("customerFirstName", customerFirstName != null ? customerFirstName : "");
        variables.put("customerLastName", customerLastName != null ? customerLastName : "");
        variables.put("companyName", companyName != null ? companyName : "");
        variables.put("total", total != null ? total.setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
        variables.put("balanceDue", balanceDue != null ? balanceDue.setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
        variables.put("paymentTerms", paymentTerms != null ? paymentTerms : "");
        return variables;
    }
}
