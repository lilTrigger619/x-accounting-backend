package com.unionsg.xaccounting.MapperLayer;


import com.unionsg.xaccounting.dto.journal.JournalLineResponse;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.journal.JournalSummaryResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
//import org.springframework.web.bind.annotation.Mapping;
//import org.mapstruct.Mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JournalMapper {
    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "reversal", ignore = true)
    @Mapping(target = "createdBy", expression = "java(CreatedByMapper.toDto(entity.getCreatedBy()))")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchCode", source = "branch.code")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "costCenterId", source = "costCenter.id")
    @Mapping(target = "costCenterCode", source = "costCenter.code")
    @Mapping(target = "costCenterName", source = "costCenter.name")
    @Mapping(target = "submittedByName", expression = "java(entity.getSubmittedBy() != null ? entity.getSubmittedBy().getFullName() : null)")
    @Mapping(target = "approvedByName", expression = "java(entity.getApprovedBy() != null ? entity.getApprovedBy().getFullName() : null)")
    @Mapping(target = "rejectedByName", expression = "java(entity.getRejectedBy() != null ? entity.getRejectedBy().getFullName() : null)")
    @Mapping(target = "balancedPerBranch", ignore = true)
    @Mapping(target = "canSubmit", ignore = true)
    @Mapping(target = "canApprove", ignore = true)
    @Mapping(target = "canPost", ignore = true)
    JournalResponse toResponse(JournalEntry entity);

    List<JournalResponse> toResponseList(List<JournalEntry> entities);

    JournalSummaryResponse toSummary(JournalEntry entity);

    List<JournalSummaryResponse> toSummaryList(
            List<JournalEntry> entities
    );

    @Mapping(target = "accountId", source = "account.id")
//    @Mapping(target = "accountCode", source = "account.code")
    @Mapping(target = "accountCode", source = "account.accountId")
//    @Mapping(target = "accountName", source = "account.name")
    @Mapping(target = "accountName", source = "account.accountName")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchCode", source = "branch.code")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "costCenterId", source = "costCenter.id")
    @Mapping(target = "costCenterCode", source = "costCenter.code")
    @Mapping(target = "costCenterName", source = "costCenter.name")
    @Mapping(target = "counterpartyBranchId", source = "counterpartyBranch.id")
    @Mapping(target = "counterpartyBranchName", source = "counterpartyBranch.name")
    JournalLineResponse toLineResponse(JournalLine line);

    List<JournalLineResponse> toLineResponseList(
            List<JournalLine> lines
    );
}
