package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;

/**
 * Works out the charge for one {@link DepreciationMethod}. The engine finds calculators by
 * {@link #method()}, so a new method is a new enum constant plus a new calculator bean; nothing
 * else in the module changes. The engine still enforces the residual floor and end of life.
 */
public interface DepreciationMethodCalculator {

    DepreciationMethod method();

    /** Whether the method needs a useful life. */
    default boolean requiresUsefulLife() {
        return true;
    }

    MethodResult calculate(DepreciationContext context);
}
