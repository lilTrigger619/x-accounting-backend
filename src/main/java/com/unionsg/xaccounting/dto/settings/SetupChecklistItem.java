package com.unionsg.xaccounting.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupChecklistItem {
    private String key;
    private String label;
    private boolean complete;
    private String detail;
    private String settingsPath;
}
