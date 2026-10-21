package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetEstimateChangeResponse {
    private Long id;
    private LocalDate effectiveDate;
    private Integer previousUsefulLifeMonths;
    private Integer newUsefulLifeMonths;
    private BigDecimal previousResidualValue;
    private BigDecimal newResidualValue;
    private DepreciationMethod previousMethod;
    private DepreciationMethod newMethod;
    private DepreciationConvention previousConvention;
    private DepreciationConvention newConvention;
    private BigDecimal previousAnnualRate;
    private BigDecimal newAnnualRate;
    private String reason;
    private String createdBy;
    private LocalDateTime createdAt;
}
