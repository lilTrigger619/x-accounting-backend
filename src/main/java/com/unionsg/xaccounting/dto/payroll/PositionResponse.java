package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PositionResponse {
    private Long id;
    private String title;
    private Long departmentId;
    private String departmentName;
    private String description;
    private boolean active;
}
