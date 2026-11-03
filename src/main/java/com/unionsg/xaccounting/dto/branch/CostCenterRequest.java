package com.unionsg.xaccounting.dto.branch;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CostCenterRequest {
    private String code;
    private String name;
    private String description;
    private Long branchId;
}
