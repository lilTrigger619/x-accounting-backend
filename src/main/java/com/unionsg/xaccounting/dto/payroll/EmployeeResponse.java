package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.EmploymentStatus;
import com.unionsg.xaccounting.enums.EmploymentType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class EmployeeResponse {
    private Long id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String fullName;
    private String workEmail;
    private String personalEmail;
    private String phone;
    private LocalDate dateOfBirth;
    private String nationalId;

    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionTitle;
    private Long workLocationId;
    private String workLocationName;

    private EmploymentType employmentType;
    private EmploymentStatus employmentStatus;
    private LocalDate dateOfEmployment;
    private LocalDate terminationDate;

    private Long payrollGroupId;
    private String payrollGroupName;

    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private String currency;

    private String taxIdentifier;
    private String statutoryId;
    private String notes;

    private BigDecimal currentBasicSalary;
    private String currentSalaryStructureName;
}
