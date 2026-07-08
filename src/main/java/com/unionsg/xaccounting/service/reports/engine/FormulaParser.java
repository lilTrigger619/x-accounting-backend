package com.unionsg.xaccounting.service.reports.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses report formulas into an evaluation-ready token stream (RPN / Reverse Polish Notation)
 * using the shunting-yard algorithm.
 */
public interface FormulaParser {
    List<Token> parseToRpn(String formula);

    enum TokenType { NUMBER, CODE, OPERATOR, LPAREN, RPAREN }

    record Token(TokenType type, String text) {
        public Token {
            if (type == null) throw new IllegalArgumentException("type is required");
            if (text == null) throw new IllegalArgumentException("text is required");
        }

        public static Token number(BigDecimal n) {
            return new Token(TokenType.NUMBER, n.stripTrailingZeros().toPlainString());
        }

        public static Token code(String code) {
            return new Token(TokenType.CODE, code);
        }

        public static Token op(char op) {
            return new Token(TokenType.OPERATOR, String.valueOf(op));
        }

        public static Token lparen() { return new Token(TokenType.LPAREN, "("); }

        public static Token rparen() { return new Token(TokenType.RPAREN, ")"); }
    }
}

