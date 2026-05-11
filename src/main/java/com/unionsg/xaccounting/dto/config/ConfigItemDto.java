package com.unionsg.xaccounting.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigItemDto {

    private String id;

    private String name;

    private String code;

    private String value;

    private String description;

    private Boolean isDefault;

    private String status;

}