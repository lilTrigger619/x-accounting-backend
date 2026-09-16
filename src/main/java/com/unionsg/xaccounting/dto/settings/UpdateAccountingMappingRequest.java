package com.unionsg.xaccounting.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountingMappingRequest {
    private String accountCode;
    private String reason;
}
