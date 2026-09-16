package com.unionsg.xaccounting.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupCompletenessResponse {
    private int completeCount;
    private int totalCount;
    private boolean readyToOperate;
    private List<SetupChecklistItem> items;
}
