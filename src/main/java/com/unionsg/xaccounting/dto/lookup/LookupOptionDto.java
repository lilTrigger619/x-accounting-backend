package com.unionsg.xaccounting.dto.lookup;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One selectable value of a system-controlled lookup. */
@Getter
@AllArgsConstructor
public class LookupOptionDto {
    private String value;
    private String label;
}
