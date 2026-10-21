package com.unionsg.xaccounting.service.fixedasset.depreciation;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything the engine needs to work out one charge.
 *
 * @param cost                    the asset's cost balance at the start of the window
 * @param accumulatedDepreciation posted accumulated depreciation at the start of the window
 * @param accumulatedImpairment   posted accumulated impairment at the start of the window
 * @param depreciationStart       the first day the asset may be depreciated
 * @param windowFrom              first day to charge (inclusive)
 * @param windowTo                last day to charge (inclusive)
 * @param cutoffDate              the disposal date when the asset is being disposed of, else null
 */
@Builder(toBuilder = true)
public record DepreciationInput(
        DepreciationMethod method,
        DepreciationConvention convention,
        BigDecimal cost,
        BigDecimal accumulatedDepreciation,
        BigDecimal accumulatedImpairment,
        BigDecimal residualValue,
        Integer usefulLifeMonths,
        BigDecimal annualRatePercent,
        LocalDate depreciationStart,
        LocalDate windowFrom,
        LocalDate windowTo,
        LocalDate cutoffDate
) {
}
