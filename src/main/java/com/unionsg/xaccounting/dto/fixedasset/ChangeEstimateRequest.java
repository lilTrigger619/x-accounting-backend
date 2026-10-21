package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEstimateRequest {
    private Integer usefulLifeMonths;
    private BigDecimal residualValue;
    private DepreciationMethod depreciationMethod;
    private DepreciationConvention depreciationConvention;
    private BigDecimal annualRate;
    private String reason;
}
