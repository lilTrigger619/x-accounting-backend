package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepositTypeRequest {
    private String name;
    private String description;
    private DepositDirection direction;
    private Boolean refundableByDefault;
    private Boolean interestBearingByDefault;
    /** Chart of Accounts primary key, as the account picker returns it. */
    private Long depositAccountId;
    private Long forfeitureAccountId;
}
