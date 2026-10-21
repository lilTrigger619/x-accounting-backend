package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Applies an annual rate to the opening carrying amount, pro-rated for the months charged. When the
 * asset has no rate of its own, the rate that brings cost down to residual over the useful life is
 * used: 1 - (residual / cost)^(12 / life months). With no residual value that formula gives 100%,
 * so double-declining (2 / life in years) is used instead.
 */
@Component
public class ReducingBalanceCalculator implements DepreciationMethodCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    @Override
    public DepreciationMethod method() {
        return DepreciationMethod.REDUCING_BALANCE;
    }

    @Override
    public MethodResult calculate(DepreciationContext c) {
        BigDecimal ratePercent = rateFor(c.input());
        BigDecimal charge = c.openingCarrying().max(BigDecimal.ZERO)
                .multiply(ratePercent).divide(HUNDRED, MathContext.DECIMAL64)
                .multiply(c.monthsCharged()).divide(TWELVE, MathContext.DECIMAL64);
        String working = "Reducing balance: carrying " + Money.format(c.openingCarrying()) + " x "
                + ratePercent.stripTrailingZeros().toPlainString() + "% a year x " + Money.months(c.monthsCharged()) + " / 12";
        return new MethodResult(charge, ratePercent, working);
    }

    /** The asset's own rate, else the rate derived from cost, residual and useful life. */
    public static BigDecimal rateFor(DepreciationInput in) {
        if (in.annualRatePercent() != null && in.annualRatePercent().signum() > 0) {
            return in.annualRatePercent();
        }
        int life = in.usefulLifeMonths() != null ? in.usefulLifeMonths() : 0;
        if (life <= 0) {
            throw new IllegalArgumentException("Reducing balance needs an annual rate or a useful life");
        }
        BigDecimal cost = in.cost();
        BigDecimal residual = in.residualValue() != null ? in.residualValue() : BigDecimal.ZERO;
        double rate;
        if (residual.signum() > 0 && cost.signum() > 0 && residual.compareTo(cost) < 0) {
            rate = 1 - Math.pow(residual.doubleValue() / cost.doubleValue(), 12.0 / life);
        } else {
            rate = Math.min(1.0, 2.0 * 12.0 / life);
        }
        return BigDecimal.valueOf(rate * 100).setScale(4, RoundingMode.HALF_UP);
    }
}
