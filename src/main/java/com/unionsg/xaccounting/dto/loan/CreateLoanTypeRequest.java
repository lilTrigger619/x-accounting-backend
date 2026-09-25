package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLoanTypeRequest {
    private String name;
    private String description;
    private LoanDirection defaultDirection;
}
