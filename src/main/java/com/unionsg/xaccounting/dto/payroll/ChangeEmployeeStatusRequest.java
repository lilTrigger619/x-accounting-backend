package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.EmploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ChangeEmployeeStatusRequest {
    private EmploymentStatus status;
    private LocalDate terminationDate;
    private String reason;
}
