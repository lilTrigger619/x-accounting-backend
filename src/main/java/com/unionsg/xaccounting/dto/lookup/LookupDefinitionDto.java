package com.unionsg.xaccounting.dto.lookup;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** A system-controlled lookup and its values, as served to pickers and filters. */
@Getter
@AllArgsConstructor
public class LookupDefinitionDto {
    private String key;
    private String title;
    private List<LookupOptionDto> options;
}
