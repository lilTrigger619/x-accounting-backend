package com.unionsg.xaccounting.service.fixedasset.depreciation;

import java.math.BigDecimal;

/**
 * One calculated charge and how it was reached.
 *
 * @param charge      rounded to the cent, never more than the depreciable amount
 * @param endOfLife   true when this charge uses up the rest of the useful life
 * @param explanation a plain-language account of the working, stored on the run line
 */
public record DepreciationResult(
        BigDecimal charge,
        BigDecimal openingCarrying,
        BigDecimal depreciableAmount,
        BigDecimal monthsCharged,
        BigDecimal remainingLifeMonths,
        BigDecimal annualRatePercent,
        boolean endOfLife,
        String explanation
) {
    public BigDecimal closingCarrying() {
        return openingCarrying.subtract(charge);
    }
}
