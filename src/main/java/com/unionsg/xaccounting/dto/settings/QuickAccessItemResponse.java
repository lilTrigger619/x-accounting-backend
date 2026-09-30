package com.unionsg.xaccounting.dto.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuickAccessItemResponse {
    private Long id;
    private String label;
    private String path;
    private Integer sortOrder;
}
