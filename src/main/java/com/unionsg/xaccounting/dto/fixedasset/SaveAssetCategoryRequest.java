package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveAssetCategoryRequest {
    private String code;
    private String name;
    private String description;
    private DepreciationMethod defaultMethod;
    private DepreciationConvention defaultConvention;
    private Integer defaultUsefulLifeMonths;
    private BigDecimal defaultResidualRate;
    private BigDecimal defaultAnnualRate;
    private Long costAccountId;
    private Long accumulatedDepreciationAccountId;
    private Long depreciationExpenseAccountId;
    private Long accumulatedImpairmentAccountId;
    private Long impairmentLossAccountId;
    private Long disposalGainAccountId;
    private Long disposalLossAccountId;
    private Boolean active;
}
