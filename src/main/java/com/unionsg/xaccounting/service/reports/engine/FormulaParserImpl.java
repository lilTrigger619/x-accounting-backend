package com.unionsg.xaccounting.service.reports.engine;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.Token;
import static com.unionsg.xaccounting.service.reports.engine.FormulaParser.TokenType;

@Component
public class FormulaParserImpl implements FormulaParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?<WS>\\s+)|" +
                    "(?<NUMBER>\\d+(?:\\.\\d+)?)|" +
                    "(?<CODE>[A-Za-z_][A-Za-z0-9_]*)|" +
                    "(?<OP>[+\\-*/])|" +
                    "(?<LPAREN>\\()|" +
                    "(?<RPAREN>\\))"
    );

    @Override
    public List<Token> parseToRpn(@NotNull String formula) {
        String normalized = formula.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Formula is empty");
        }

        // Tokenize
        List<Token> tokens = tokenize(normalized);

        // Shunting-yard: convert infix tokens -> RPN
        List<Token> output = new ArrayList<>();
        Deque<Token> ops = new ArrayDeque<>();

        for (Token t : tokens) {
            switch (t.type()) {
                case NUMBER, CODE -> output.add(t);
                case OPERATOR -> {
                    char op = t.text().charAt(0);
                    while (!ops.isEmpty() && ops.peek().type() == TokenType.OPERATOR) {
                        char topOp = ops.peek().text().charAt(0);
                        if (precedence(topOp) >= precedence(op)) {
                            output.add(ops.pop());
                        } else {
                            break;
                        }
                    }
                    ops.push(t);
                }
                case LPAREN -> ops.push(t);
                case RPAREN -> {
                    boolean found = false;
                    while (!ops.isEmpty()) {
                        Token top = ops.pop();
                        if (top.type() == TokenType.LPAREN) {
                            found = true;
                            break;
                        }
                        output.add(top);
                    }
                    if (!found) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                }
            }
        }

        while (!ops.isEmpty()) {
            Token top = ops.pop();
            if (top.type() == TokenType.LPAREN || top.type() == TokenType.RPAREN) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(top);
        }

        if (output.isEmpty()) {
            throw new IllegalArgumentException("Invalid formula");
        }

        return output;
    }

    private List<Token> tokenize(String formula) {
        List<Token> out = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(formula);

        int idx = 0;
        while (m.find()) {
            if (m.start() != idx) {
                // uncovered substring => invalid syntax / unexpected char
                String bad = formula.substring(idx, m.start());
                throw new IllegalArgumentException("Invalid syntax near: '" + bad + "'");
            }

            if (m.group("WS") != null) {
                // ignore
            } else if (m.group("NUMBER") != null) {
                BigDecimal n = new BigDecimal(m.group("NUMBER"));
                out.add(Token.number(n));
            } else if (m.group("CODE") != null) {
                String code = m.group("CODE").toUpperCase(Locale.ROOT);
                out.add(Token.code(code));
            } else if (m.group("OP") != null) {
                out.add(Token.op(m.group("OP").charAt(0)));
            } else if (m.group("LPAREN") != null) {
                out.add(Token.lparen());
            } else if (m.group("RPAREN") != null) {
                out.add(Token.rparen());
            } else {
                throw new IllegalArgumentException("Invalid syntax");
            }

            idx = m.end();
        }

        if (idx != formula.length()) {
            throw new IllegalArgumentException("Invalid syntax near: '" + formula.substring(idx) + "'");
        }

        // Validate that parentheses/operand structure is syntactically plausible
        // (full semantic validation happens in FormulaValidator)
        return out;
    }

    private int precedence(char op) {
        return switch (op) {
            case '*', '/' -> 2;
            case '+', '-' -> 1;
            default -> 0;
        };
    }
}

