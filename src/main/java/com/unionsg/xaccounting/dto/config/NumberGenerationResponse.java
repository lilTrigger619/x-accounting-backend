package com.unionsg.xaccounting.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumberGenerationResponse {

    private String module;
    private String generatedNumber;

}

