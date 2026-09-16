package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePositionRequest {
    private String title;
    private Long departmentId;
    private String description;
}
