package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.AssetAcquisitionSource;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationConvention;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveFixedAssetRequest {
    private String assetNumber;
    private String name;
    private Long categoryId;
    private String description;
    private String serialNumber;
    private Long locationId;
    private Long departmentId;
    private Long supplierId;
    private LocalDate acquisitionDate;
    private LocalDate availableForUseDate;
    private LocalDate depreciationStartDate;
    private BigDecimal acquisitionCost;
    private BigDecimal residualValue;
    private Integer usefulLifeMonths;
    private DepreciationMethod depreciationMethod;
    private DepreciationConvention depreciationConvention;
    private BigDecimal annualRate;
    private AssetAcquisitionSource acquisitionSource;
    private Long bankAccountId;
    private Long offsetAccountId;
    private Long sourceJournalId;
    private BigDecimal openingAccumulatedDepreciation;
    private LocalDate openingDepreciationDate;
    private Long costAccountId;
    private Long accumulatedDepreciationAccountId;
    private Long depreciationExpenseAccountId;
    private Long accumulatedImpairmentAccountId;
    private Long impairmentLossAccountId;
    private Long disposalGainAccountId;
    private Long disposalLossAccountId;
    private String notes;
}
