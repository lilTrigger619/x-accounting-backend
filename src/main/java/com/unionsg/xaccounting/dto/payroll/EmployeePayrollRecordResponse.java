package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class EmployeePayrollRecordResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String departmentName;
    private BigDecimal grossPay;
    private BigDecimal totalEmployeeDeductions;
    private BigDecimal netPay;
    private BigDecimal totalEmployerCost;
    private boolean negativeNetPayFlagged;
    private List<PayrollRecordComponentResponse> components;
}
