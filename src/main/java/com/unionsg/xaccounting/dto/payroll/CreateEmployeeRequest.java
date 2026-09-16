package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.EmploymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeRequest {
    private String firstName;
    private String lastName;
    private String workEmail;
    private String personalEmail;
    private String phone;
    private LocalDate dateOfBirth;
    private String nationalId;
    private Long departmentId;
    private Long positionId;
    private Long workLocationId;
    private EmploymentType employmentType;
    private LocalDate dateOfEmployment;
    private Long payrollGroupId;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private String currency;
    private String taxIdentifier;
    private String statutoryId;
    private String notes;

    // Initial compensation - creates the first EmployeeSalaryStructure assignment atomically.
    private Long salaryStructureId;
    private BigDecimal basicSalary;
    private LocalDate compensationEffectiveFrom;
}
