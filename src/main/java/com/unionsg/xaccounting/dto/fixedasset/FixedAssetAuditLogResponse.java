package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.FixedAssetAuditAction;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FixedAssetAuditLogResponse {
    private Long id;
    private Long assetId;
    private Long runId;
    private FixedAssetAuditAction action;
    private String previousStatus;
    private String newStatus;
    private String details;
    private String performedBy;
    private LocalDateTime performedAt;
}
