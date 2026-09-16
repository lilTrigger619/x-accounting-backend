package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateSalaryStructureRequest {
    private String name;
    private String description;
    private List<SalaryStructureLineRequest> lines;
}
