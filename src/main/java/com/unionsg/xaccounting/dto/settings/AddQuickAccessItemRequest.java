package com.unionsg.xaccounting.dto.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddQuickAccessItemRequest {
    private String label;
    private String path;
}
