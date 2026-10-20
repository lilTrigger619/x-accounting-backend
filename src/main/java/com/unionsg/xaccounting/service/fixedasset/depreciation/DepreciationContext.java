package com.unionsg.xaccounting.service.fixedasset.depreciation;

import java.math.BigDecimal;

/**
 * An input plus what the engine derived from it before handing it to a method calculator.
 *
 * @param openingCarrying     cost - accumulated depreciation - accumulated impairment
 * @param depreciableAmount   opening carrying amount above residual value, never negative
 * @param monthsCharged       months of service in the window under the convention
 * @param remainingLifeMonths useful life left at the start of the window (null when no life is set)
 */
public record DepreciationContext(
        DepreciationInput input,
        BigDecimal openingCarrying,
        BigDecimal depreciableAmount,
        BigDecimal monthsCharged,
        BigDecimal remainingLifeMonths
) {
}
