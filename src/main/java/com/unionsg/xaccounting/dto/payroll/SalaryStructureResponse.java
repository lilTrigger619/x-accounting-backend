package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SalaryStructureResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private List<SalaryStructureLineResponse> lines;
}
