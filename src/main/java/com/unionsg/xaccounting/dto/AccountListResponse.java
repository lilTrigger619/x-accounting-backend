package com.unionsg.xaccounting.dto;

import com.unionsg.xaccounting.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountListResponse {
    private String accountNumber;
    private String accountName;
    private String accountType;
    private String subType;
    private AccountStatus status;
    private BigDecimal balance;
}

