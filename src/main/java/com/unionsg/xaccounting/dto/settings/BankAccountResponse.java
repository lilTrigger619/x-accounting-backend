package com.unionsg.xaccounting.dto.settings;

import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {
    private Long id;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String currency;
    private String glAccountCode;
    private String glAccountName;
    private String branch;
    private BankAccountStatus status;
    private Boolean isDefault;
    private Boolean enableReconciliation;
}
