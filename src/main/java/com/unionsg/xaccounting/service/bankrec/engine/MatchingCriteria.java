package com.unionsg.xaccounting.service.bankrec.engine;

/** The engine's view of a matching rule. */
public record MatchingCriteria(
        String name,
        int dateToleranceDays,
        boolean matchReference,
        boolean matchTransactionNumber,
        boolean matchDescription,
        boolean matchChequeNumber,
        boolean matchCounterparty,
        int autoConfirmThreshold,
        int suggestThreshold
) {
    /** Used when no rule has been configured: exact amount within ±3 days, every criterion on. */
    public static MatchingCriteria standard() {
        return new MatchingCriteria("Standard (exact amount, ±3 days)", 3, true, true, true, true, true, 85, 60);
    }
}
