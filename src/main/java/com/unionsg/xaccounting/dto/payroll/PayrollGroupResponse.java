package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayFrequency;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollGroupResponse {
    private Long id;
    private String name;
    private PayFrequency payFrequency;
    private String description;
    private boolean active;
}
