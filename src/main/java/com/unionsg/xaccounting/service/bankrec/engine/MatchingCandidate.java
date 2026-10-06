package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One side of a possible match, as the engine sees it. {@code signedAmount} is the remaining
 * amount from the bank's point of view: deposits (statement credits, book debits) positive.
 * {@code number} is the book journal number (null for statement lines); {@code counterparty}
 * and {@code counterpartyReference} come from the customer/supplier payment behind a book line.
 */
public record MatchingCandidate(
        Long id,
        LocalDate date,
        BigDecimal signedAmount,
        String reference,
        String description,
        String number,
        String counterparty,
        String counterpartyReference
) {
}
