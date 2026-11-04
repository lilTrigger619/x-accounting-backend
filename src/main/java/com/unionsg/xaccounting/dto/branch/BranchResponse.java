package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BranchResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String address;
    private String phone;
    private String email;
    private Boolean headOffice;
    private Boolean active;
    private Long defaultCostCenterId;
    private String defaultCostCenterCode;
    private long costCenterCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
