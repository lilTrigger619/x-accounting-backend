package com.unionsg.xaccounting.dto.settings;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderQuickAccessRequest {
    private List<Long> orderedIds;
}
