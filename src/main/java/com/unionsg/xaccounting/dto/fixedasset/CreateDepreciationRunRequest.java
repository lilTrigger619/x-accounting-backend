package com.unionsg.xaccounting.dto.fixedasset;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepreciationRunRequest {
    private Long accountingPeriodId;
    private String notes;
}
