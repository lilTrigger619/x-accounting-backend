package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateTaxConfigurationRequest {
    private String name;
    private String jurisdiction;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String taxPayableAccountCode;
    private List<TaxBracketRequest> brackets;
}
