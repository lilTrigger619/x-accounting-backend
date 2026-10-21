package com.unionsg.xaccounting.service.fixedasset.depreciation;

import java.math.BigDecimal;

/**
 * What a method calculator proposes before the engine applies the residual-value floor and the
 * end-of-life rule.
 *
 * @param charge            the unrounded charge
 * @param annualRatePercent the rate used, for methods that use one
 * @param working           the formula with its numbers, for the run line's explanation
 */
public record MethodResult(BigDecimal charge, BigDecimal annualRatePercent, String working) {
}
