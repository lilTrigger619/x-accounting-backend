package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CostCenterResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private Boolean branchDefault;
    private Boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
