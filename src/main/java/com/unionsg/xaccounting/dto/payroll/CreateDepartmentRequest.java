package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentRequest {
    private String name;
    private String costCenterCode;
    private String description;
}
