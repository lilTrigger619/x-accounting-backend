package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkLocationResponse {
    private Long id;
    private String name;
    private String address;
    private boolean active;
}
