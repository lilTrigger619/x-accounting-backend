package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;
import java.util.List;

/** A one-to-one pairing the engine found. {@code autoConfirm} is false for ambiguous or low scores. */
public record MatchProposal(
        Long statementId,
        Long bookId,
        BigDecimal amount,
        int confidence,
        boolean autoConfirm,
        String ruleName,
        List<String> reasons
) {
}
