package com.unionsg.xaccounting.dto.settings;

import lombok.Data;

@Data
public class SaveBankAccountRequest {
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String currency;
    private String glAccountCode;
    private String branch;
    private Boolean isDefault;
    private Boolean enableReconciliation;
}
