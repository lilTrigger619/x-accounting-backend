package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FormulaEvaluatorImpl implements FormulaEvaluator {

    // Tokens are ReportSection.code (letters, digits, underscore)
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @Override
    public Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode,
                                            List<ReportSection> sections) {

        Map<String, ReportSection> byCode = new HashMap<>();
        for (ReportSection s : sections) {
            byCode.put(s.getCode(), s);
        }

        Map<String, BigDecimal> memo = new HashMap<>(baseByCode);
        Set<String> visiting = new HashSet<>();

        // Evaluate for all codes
        for (String code : byCode.keySet()) {
            compute(code, byCode, memo, visiting);
        }

        return memo;
    }

    private BigDecimal compute(String code,
                                Map<String, ReportSection> byCode,
                                Map<String, BigDecimal> memo,
                                Set<String> visiting) {

        if (memo.containsKey(code) && (byCode.get(code).getFormula() == null || byCode.get(code).getFormula().isBlank())) {
            return memo.getOrDefault(code, BigDecimal.ZERO);
        }

        ReportSection section = byCode.get(code);
        if (section == null) {
            return BigDecimal.ZERO;
        }

        String formula = section.getFormula();
        if (formula == null || formula.isBlank()) {
            return memo.getOrDefault(code, BigDecimal.ZERO);
        }

        if (memo.containsKey(code) && memo.get(code) != null) {
            // still allow recompute if formula exists; but memo should be authoritative
            // (we don't overwrite if already computed)
            return memo.getOrDefault(code, BigDecimal.ZERO);
        }

        if (!visiting.add(code)) {
            throw new IllegalStateException("Cycle detected in report formula graph at: " + code);
        }

        // Replace tokens with numeric values
        Matcher m = TOKEN.matcher(formula);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String token = m.group();
            if (byCode.containsKey(token) || memo.containsKey(token)) {
                BigDecimal val = compute(token, byCode, memo, visiting);
                m.appendReplacement(sb, val.toPlainString());
            }
        }
        m.appendTail(sb);

        BigDecimal result = safeEval(sb.toString());
        memo.put(code, result);
        visiting.remove(code);
        return result;
    }

    private BigDecimal safeEval(String expr) {
        // Very small evaluator for + - * / and parentheses using a shunting-yard approach.
        // For safety, only allow digits, dot, whitespace, operators, parentheses.
        if (!expr.matches("[0-9+\\-*/().\\s]+")) {
            throw new IllegalArgumentException("Invalid expression after token resolution: " + expr);
        }

        List<String> tokens = tokenize(expr);
        Deque<BigDecimal> values = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();

        for (String t : tokens) {
            if (t.isBlank()) continue;
            char c = t.charAt(0);
            if (t.length() == 1 && (c == '+' || c == '-' || c == '*' || c == '/')) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) {
                    apply(values, ops.pop());
                }
                ops.push(c);
            } else if (t.equals("(")) {
                ops.push('(');
            } else if (t.equals(")")) {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    apply(values, ops.pop());
                }
                if (ops.isEmpty() || ops.pop() != '(') {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
            } else {
                values.push(new BigDecimal(t));
            }
        }

        while (!ops.isEmpty()) {
            char op = ops.pop();
            if (op == '(') throw new IllegalArgumentException("Mismatched parentheses");
            apply(values, op);
        }

        if (values.size() != 1) throw new IllegalArgumentException("Invalid expression: " + expr);
        return values.pop();
    }

    private List<String> tokenize(String expr) {
        List<String> out = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                if (num.length() > 0) {
                    out.add(num.toString());
                    num.setLength(0);
                }
                continue;
            }
            if (Character.isDigit(c) || c == '.') {
                num.append(c);
                continue;
            }
            if (num.length() > 0) {
                out.add(num.toString());
                num.setLength(0);
            }
            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')') {
                out.add(String.valueOf(c));
            } else {
                throw new IllegalArgumentException("Unexpected char in expression: " + c);
            }
        }
        if (num.length() > 0) out.add(num.toString());
        return out;
    }

    private int precedence(char op) {
        return switch (op) {
            case '*', '/' -> 2;
            case '+', '-' -> 1;
            default -> 0;
        };
    }

    private void apply(Deque<BigDecimal> values, char op) {
        BigDecimal b = values.pop();
        BigDecimal a = values.pop();
        BigDecimal r = switch (op) {
            case '+' -> a.add(b);
            case '-' -> a.subtract(b);
            case '*' -> a.multiply(b);
            case '/' -> a.divide(b, 10, java.math.RoundingMode.HALF_UP);
            default -> throw new IllegalArgumentException("Unknown operator: " + op);
        };
        values.push(r);
    }
}

