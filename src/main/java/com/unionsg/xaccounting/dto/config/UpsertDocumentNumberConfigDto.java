package com.unionsg.xaccounting.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertDocumentNumberConfigDto {

    @NotBlank(message = "Prefix is required")
    private String prefix;

    private Integer padding = 5;

    private Boolean includeYear = false;

    private Boolean includeMonth = false;

    private Boolean resetYearly = false;

    private Boolean resetMonthly = false;

    private String separator = "-";

}

