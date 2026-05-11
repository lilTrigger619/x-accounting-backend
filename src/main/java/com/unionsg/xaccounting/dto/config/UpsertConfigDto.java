package com.unionsg.xaccounting.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpsertConfigDto {

    @NotBlank
    private String key;

    @NotBlank
    private String title;

    private String description;

    private String itemLabel;

    private Boolean showValueField;

    private String valueFieldLabel;

    private String valueFieldPlaceholder;

}