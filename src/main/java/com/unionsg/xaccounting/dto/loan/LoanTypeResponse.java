package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanTypeResponse {
    private Long id;
    private String name;
    private String description;
    private LoanDirection defaultDirection;
    private Boolean active;
}
