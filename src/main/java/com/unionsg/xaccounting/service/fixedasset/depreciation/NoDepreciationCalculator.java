package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Land and similar assets that are carried at cost and never depreciated. */
@Component
public class NoDepreciationCalculator implements DepreciationMethodCalculator {

    @Override
    public DepreciationMethod method() {
        return DepreciationMethod.NOT_DEPRECIATED;
    }

    @Override
    public boolean requiresUsefulLife() {
        return false;
    }

    @Override
    public MethodResult calculate(DepreciationContext context) {
        return new MethodResult(BigDecimal.ZERO, null, "Not depreciated");
    }
}
