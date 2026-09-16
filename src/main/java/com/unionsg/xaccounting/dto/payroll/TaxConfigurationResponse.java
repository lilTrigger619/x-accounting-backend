package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TaxConfigurationResponse {
    private Long id;
    private String name;
    private String jurisdiction;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private String taxPayableAccountCode;
    private List<TaxBracketResponse> brackets;
}
