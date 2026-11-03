package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.MapperLayer.CreatedByMapper;
import com.unionsg.xaccounting.dto.branch.BranchResponse;
import com.unionsg.xaccounting.dto.branch.CostCenterResponse;
import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;

public final class BranchMapper {

    private BranchMapper() {
    }

    public static BranchResponse toResponse(Branch branch, CostCenter defaultCostCenter, long costCenterCount) {
        if (branch == null) {
            return null;
        }
        CreatedByDTO createdBy = CreatedByMapper.toDto(branch.getCreatedBy());
        return BranchResponse.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .description(branch.getDescription())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .headOffice(Boolean.TRUE.equals(branch.getHeadOffice()))
                .active(Boolean.TRUE.equals(branch.getActive()))
                .defaultCostCenterId(defaultCostCenter != null ? defaultCostCenter.getId() : null)
                .defaultCostCenterCode(defaultCostCenter != null ? defaultCostCenter.getCode() : null)
                .costCenterCount(costCenterCount)
                .createdBy(createdBy != null ? createdBy.getFullName() : null)
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }

    public static CostCenterResponse toResponse(CostCenter cc) {
        if (cc == null) {
            return null;
        }
        Branch branch = cc.getBranch();
        CreatedByDTO createdBy = CreatedByMapper.toDto(cc.getCreatedBy());
        return CostCenterResponse.builder()
                .id(cc.getId())
                .code(cc.getCode())
                .name(cc.getName())
                .description(cc.getDescription())
                .branchId(branch != null ? branch.getId() : null)
                .branchCode(branch != null ? branch.getCode() : null)
                .branchName(branch != null ? branch.getName() : null)
                .branchDefault(Boolean.TRUE.equals(cc.getBranchDefault()))
                .active(Boolean.TRUE.equals(cc.getActive()))
                .createdBy(createdBy != null ? createdBy.getFullName() : null)
                .createdAt(cc.getCreatedAt())
                .updatedAt(cc.getUpdatedAt())
                .build();
    }
}
