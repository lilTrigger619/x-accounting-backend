package com.unionsg.xaccounting.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentNumberConfigDto {

    private Long id;
    private String module;
    private String prefix;
    private Long lastNumber;
    private Integer padding;
    private Boolean includeYear;
    private Boolean includeMonth;
    private Boolean resetYearly;
    private Boolean resetMonthly;
    private String separator;
    private Integer lastResetYear;
    private Integer lastResetMonth;

}

