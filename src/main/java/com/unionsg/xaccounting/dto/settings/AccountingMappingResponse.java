package com.unionsg.xaccounting.dto.settings;

import com.unionsg.xaccounting.enums.settings.MappingGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingMappingResponse {
    private String mappingKey;
    private MappingGroup group;
    private String label;
    private String description;
    private String accountCode;
    private String accountName;
    private boolean configured;
}
