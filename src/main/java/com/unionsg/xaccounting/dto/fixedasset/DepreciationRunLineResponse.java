package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationLineStatus;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepreciationRunLineResponse {
    private Long id;
    private Long assetId;
    private String assetNumber;
    private String assetName;
    private String categoryName;
    private DepreciationLineStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;
    private DepreciationMethod method;
    private DepreciationConvention convention;
    private BigDecimal openingCost;
    private BigDecimal openingAccumulatedDepreciation;
    private BigDecimal openingAccumulatedImpairment;
    private BigDecimal openingCarryingAmount;
    private BigDecimal residualValue;
    private Integer usefulLifeMonths;
    private BigDecimal remainingLifeMonths;
    private BigDecimal monthsCharged;
    private BigDecimal annualRate;
    private BigDecimal depreciationAmount;
    private BigDecimal closingAccumulatedDepreciation;
    private BigDecimal closingCarryingAmount;
    private AccountRef expenseAccount;
    private AccountRef accumulatedDepreciationAccount;
    private String explanation;
    private String message;
}
