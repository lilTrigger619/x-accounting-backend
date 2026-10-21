package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetCategoryResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DepreciationMethod defaultMethod;
    private DepreciationConvention defaultConvention;
    private Integer defaultUsefulLifeMonths;
    private BigDecimal defaultResidualRate;
    private BigDecimal defaultAnnualRate;
    private List<AccountRoleResponse> accounts;
    private Boolean active;
    private Long assetCount;
    private LocalDateTime createdAt;
}
