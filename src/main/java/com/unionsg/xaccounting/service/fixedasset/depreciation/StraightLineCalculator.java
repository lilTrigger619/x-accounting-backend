package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Spreads what is left to depreciate (carrying amount above residual) evenly over the useful life
 * that is left. With no changes this is (cost - residual) / life every month; after an impairment
 * or a change of estimate the new figures apply from then on without restating the past (IAS 8).
 */
@Component
public class StraightLineCalculator implements DepreciationMethodCalculator {

    @Override
    public DepreciationMethod method() {
        return DepreciationMethod.STRAIGHT_LINE;
    }

    @Override
    public MethodResult calculate(DepreciationContext c) {
        BigDecimal remaining = c.remainingLifeMonths();
        if (remaining == null || remaining.signum() <= 0) {
            return new MethodResult(c.depreciableAmount(), null,
                    "Straight line: no useful life left, so the remaining " + Money.format(c.depreciableAmount()) + " is charged");
        }
        BigDecimal charge = c.depreciableAmount().multiply(c.monthsCharged())
                .divide(remaining, MathContext.DECIMAL64);
        String working = "Straight line: (carrying " + Money.format(c.openingCarrying()) + " - residual "
                + Money.format(c.input().residualValue()) + ") x " + Money.months(c.monthsCharged())
                + " month(s) / " + Money.months(remaining) + " remaining month(s)";
        return new MethodResult(charge, null, working);
    }
}
