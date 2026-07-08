package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.Token;
import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.TokenType;

@Component
public class FormulaValidatorImpl implements FormulaValidator {

    private final FormulaParser parser;
    private final FormulaEvaluatorCore core;

    public FormulaValidatorImpl(FormulaParser parser, FormulaEvaluatorCore core) {
        this.parser = parser;
        this.core = core;
    }

    @Override
    public void validate(String sectionCode,
                           String formula,
                           Map<String, ReportSection> allSectionsByCode,
                           List<ReportSection> sectionsInGraph,
                           Map<String, BigDecimal> baseByCode) {
        if (formula == null || formula.trim().isEmpty()) return;

        String codeKey = sectionCode == null ? null : sectionCode.toUpperCase(Locale.ROOT);

        // Parser validation (invalid syntax / mismatched parentheses)
        List<Token> rpn = parser.parseToRpn(formula);

        // Unknown/self reference detection (semantic, based on tokens)
        for (Token t : rpn) {
            if (t.type() == TokenType.CODE) {
                String ref = t.text();

                if (codeKey != null && ref.equals(codeKey)) {
                    throw new IllegalArgumentException("Self-references are not allowed in formula for section: " + sectionCode);
                }

                if (!allSectionsByCode.containsKey(ref) && !baseByCode.containsKey(ref)) {
                    throw new IllegalArgumentException("Unknown section code in formula: " + ref);
                }
            }
        }

        // Cycle detection: detect cycles in graph induced by section-to-section references
        // We do this by performing a DFS over tokens references (semantic dependency graph).
        Set<String> visiting = new java.util.HashSet<>();
        Set<String> visited = new java.util.HashSet<>();
        dfsValidateDependencies(codeKey, allSectionsByCode, visiting, visited);

        // Division by zero detection during evaluation.
        // We reuse the shared evaluator (same parser/eval logic) so divide-by-zero is consistent.
        // If evaluation fails due to division by zero (or any other invalid expression), it will throw here.
        core.evaluate(baseByCode, sectionsInGraph);

    }

    private void dfsValidateDependencies(String startCode,
                                         Map<String, ReportSection> allSectionsByCode,
                                         Set<String> visiting,
                                         Set<String> visited) {
        if (startCode == null) return;
        if (!allSectionsByCode.containsKey(startCode)) return;
        visit(startCode, allSectionsByCode, visiting, visited);
    }

    private void visit(String code,
                       Map<String, ReportSection> allSectionsByCode,
                       Set<String> visiting,
                       Set<String> visited) {
        if (visited.contains(code)) return;
        if (!visiting.add(code)) {
            throw new IllegalStateException("Circular reference detected in report formula graph at: " + code);
        }

        ReportSection section = allSectionsByCode.get(code);
        String formula = section.getFormula();
        if (formula == null || formula.trim().isEmpty()) {
            visiting.remove(code);
            visited.add(code);
            return;
        }

        // Collect referenced codes from parser output
        List<Token> rpn = parser.parseToRpn(formula);
        for (Token t : rpn) {
            if (t.type() == TokenType.CODE) {
                String ref = t.text();
                if (allSectionsByCode.containsKey(ref)) {
                    visit(ref, allSectionsByCode, visiting, visited);
                }
            }
        }

        visiting.remove(code);
        visited.add(code);
    }
}

