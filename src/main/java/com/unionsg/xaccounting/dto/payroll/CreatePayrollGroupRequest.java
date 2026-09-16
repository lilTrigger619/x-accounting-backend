package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayFrequency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePayrollGroupRequest {
    private String name;
    private PayFrequency payFrequency;
    private String description;
}
