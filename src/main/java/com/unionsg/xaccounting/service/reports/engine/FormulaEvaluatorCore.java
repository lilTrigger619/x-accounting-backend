package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.Token;
import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.TokenType;

/**
 * Shared evaluation logic between validation and evaluation.
 */
import org.springframework.stereotype.Component;

@Component
public class FormulaEvaluatorCore {

    private final FormulaParser parser;


    public FormulaEvaluatorCore(FormulaParser parser) {
        this.parser = parser;
    }

    public Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode,
                                             List<ReportSection> sections) {
        return evaluateInternal(baseByCode, sections, false);
    }

    Map<String, BigDecimal> evaluateInternal(Map<String, BigDecimal> baseByCode,
                                              List<ReportSection> sections,
                                              boolean divisionProbe) {
        Map<String, ReportSection> byCode = new HashMap<>();
        for (ReportSection s : sections) {
            byCode.put(s.getCode().toUpperCase(java.util.Locale.ROOT), s);
        }

        Map<String, BigDecimal> memo = new HashMap<>(normalizeBase(baseByCode));
        Set<String> visiting = new java.util.HashSet<>();

        for (String code : byCode.keySet()) {
            compute(code, byCode, memo, visiting, divisionProbe);
        }

        return memo;
    }

    private Map<String, BigDecimal> normalizeBase(Map<String, BigDecimal> baseByCode) {
        Map<String, BigDecimal> out = new HashMap<>();
        if (baseByCode == null) return out;
        for (Map.Entry<String, BigDecimal> e : baseByCode.entrySet()) {
            if (e.getKey() == null) continue;
            out.put(e.getKey().toUpperCase(java.util.Locale.ROOT), e.getValue());
        }
        return out;
    }

    private BigDecimal compute(String code,
                                Map<String, ReportSection> byCode,
                                Map<String, BigDecimal> memo,
                                Set<String> visiting,
                                boolean divisionProbe) {
        String key = code.toUpperCase(java.util.Locale.ROOT);

        ReportSection section = byCode.get(key);
        if (section == null) {
            return memo.getOrDefault(key, BigDecimal.ZERO);
        }

        String formula = section.getFormula();
        if (formula == null || formula.trim().isEmpty()) {
            return memo.getOrDefault(key, BigDecimal.ZERO);
        }

        if (memo.containsKey(key)) {
            return memo.getOrDefault(key, BigDecimal.ZERO);
        }

        if (!visiting.add(key)) {
            throw new IllegalStateException("Cycle detected in report formula graph at: " + key);
        }

        List<Token> rpn = parser.parseToRpn(formula);

        // Evaluate RPN
        java.util.Deque<BigDecimal> stack = new java.util.ArrayDeque<>();
        for (Token t : rpn) {
            if (t.type() == TokenType.NUMBER) {
                stack.push(new BigDecimal(t.text()));
            } else if (t.type() == TokenType.CODE) {
                stack.push(compute(t.text(), byCode, memo, visiting, divisionProbe));
            } else if (t.type() == TokenType.OPERATOR) {
                BigDecimal b = stack.pop();
                BigDecimal a = stack.pop();
                char op = t.text().charAt(0);

                BigDecimal r;
                switch (op) {
                    case '+': r = a.add(b); break;
                    case '-': r = a.subtract(b); break;
                    case '*': r = a.multiply(b); break;
                    case '/':
                        if (b.compareTo(BigDecimal.ZERO) == 0) {
                            throw new ArithmeticException("Division by zero detected");
                        }
                        r = a.divide(b, 10, java.math.RoundingMode.HALF_UP);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operator: " + op);
                }
                stack.push(r);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression after parsing: " + formula);
        }

        BigDecimal result = stack.pop();
        memo.put(key, result);
        visiting.remove(key);
        return result;
    }
}

