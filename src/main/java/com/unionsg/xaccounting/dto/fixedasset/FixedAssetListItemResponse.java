package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FixedAssetListItemResponse {
    private Long id;
    private String assetNumber;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String locationName;
    private String departmentName;
    private FixedAssetStatus status;
    private DepreciationMethod depreciationMethod;
    private LocalDate acquisitionDate;
    private LocalDate availableForUseDate;
    private BigDecimal acquisitionCost;
    private BigDecimal costBalance;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal accumulatedImpairment;
    private BigDecimal carryingAmount;
    private LocalDate depreciatedThrough;
}
