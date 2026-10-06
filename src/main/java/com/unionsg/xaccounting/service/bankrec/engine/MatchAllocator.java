package com.unionsg.xaccounting.service.bankrec.engine;

import com.unionsg.xaccounting.exception.BusinessException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out how much of each selected item a manual match uses.
 *
 * <p>Supports one-to-one, one-to-many and many-to-one. The match amount is the smaller of the
 * two sides' remaining totals, so a match can be partial: whatever is left on the larger side
 * stays unmatched for a later match or an adjustment. Items are consumed in the order given.
 * Explicit allocations, when supplied, must balance between the sides and stay within each
 * item's remaining amount.</p>
 */
public final class MatchAllocator {

    private MatchAllocator() {
    }

    public record Allocation(Map<Long, BigDecimal> statement, Map<Long, BigDecimal> book, BigDecimal amount) {
    }

    public static Allocation allocate(List<AllocationItem> statement, List<AllocationItem> book) {
        if (statement == null || statement.isEmpty() || book == null || book.isEmpty()) {
            throw new BusinessException("Select at least one bank statement transaction and one book transaction");
        }
        if (statement.size() > 1 && book.size() > 1) {
            throw new BusinessException("Match one statement line to several book lines, or several statement lines to one "
                    + "book line, not many to many");
        }
        int direction = directionOf(statement, "bank statement");
        if (directionOf(book, "book") != direction) {
            throw new BusinessException("A deposit can only be matched to receipts in the books, and a withdrawal to "
                    + "payments");
        }

        boolean explicit = statement.stream().anyMatch(i -> i.requested() != null)
                || book.stream().anyMatch(i -> i.requested() != null);
        if (explicit) {
            Map<Long, BigDecimal> s = explicitSide(statement, "bank statement");
            Map<Long, BigDecimal> b = explicitSide(book, "book");
            BigDecimal sTotal = total(s);
            BigDecimal bTotal = total(b);
            if (sTotal.compareTo(bTotal) != 0) {
                throw new BusinessException("The statement side (" + sTotal.toPlainString() + ") and the book side ("
                        + bTotal.toPlainString() + ") of a match must be equal");
            }
            return new Allocation(s, b, sTotal);
        }

        BigDecimal statementTotal = statement.stream().map(i -> i.signedRemaining().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookTotal = book.stream().map(i -> i.signedRemaining().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = statementTotal.min(bookTotal);
        return new Allocation(consume(statement, amount), consume(book, amount), amount);
    }

    private static int directionOf(List<AllocationItem> items, String side) {
        int direction = 0;
        for (AllocationItem item : items) {
            if (item.signedRemaining() == null || item.signedRemaining().signum() == 0) {
                throw new BusinessException("A selected " + side + " transaction is already fully matched");
            }
            int sign = item.signedRemaining().signum();
            if (direction != 0 && sign != direction) {
                throw new BusinessException("Selected " + side + " transactions mix deposits and withdrawals");
            }
            direction = sign;
        }
        return direction;
    }

    private static Map<Long, BigDecimal> explicitSide(List<AllocationItem> items, String side) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        for (AllocationItem item : items) {
            BigDecimal requested = item.requested() != null ? item.requested() : item.signedRemaining().abs();
            if (requested.signum() <= 0) {
                throw new BusinessException("Allocated amounts must be positive");
            }
            if (requested.compareTo(item.signedRemaining().abs()) > 0) {
                throw new BusinessException("An allocation is larger than the " + side + " transaction's unmatched amount ("
                        + item.signedRemaining().abs().toPlainString() + ")");
            }
            result.put(item.id(), requested);
        }
        return result;
    }

    private static Map<Long, BigDecimal> consume(List<AllocationItem> items, BigDecimal amount) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal left = amount;
        for (AllocationItem item : items) {
            if (left.signum() <= 0) {
                throw new BusinessException("Remove the extra selections: the other side is already fully covered");
            }
            BigDecimal take = item.signedRemaining().abs().min(left);
            result.put(item.id(), take);
            left = left.subtract(take);
        }
        return result;
    }

    private static BigDecimal total(Map<Long, BigDecimal> side) {
        return side.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
