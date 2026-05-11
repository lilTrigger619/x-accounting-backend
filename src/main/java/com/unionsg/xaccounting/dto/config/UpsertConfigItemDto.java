package com.unionsg.xaccounting.dto.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class UpsertConfigItemDto {

    @NotBlank
    private String name;

    private String code;

    private String value;

    private String description;

    private Boolean isDefault;

    private Boolean status;

}