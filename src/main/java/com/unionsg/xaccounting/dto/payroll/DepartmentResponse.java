package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String costCenterCode;
    private String description;
    private boolean active;
}
