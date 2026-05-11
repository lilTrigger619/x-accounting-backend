package com.unionsg.xaccounting.dto.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigDto {

    private String key;

    private String title;

    private String description;

    private String itemLabel;

    private Boolean showValueField;

    private String valueFieldLabel;

    private String valueFieldPlaceholder;

    private List<ConfigItemDto> items;

}