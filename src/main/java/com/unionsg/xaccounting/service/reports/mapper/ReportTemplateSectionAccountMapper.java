package com.unionsg.xaccounting.service.reports.mapper;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountResponseDto;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateSectionAccountMapper {

    public ReportTemplateSectionAccountResponseDto toResponse(ReportTemplateSectionAccount entity) {
        if (entity == null) return null;

        return new ReportTemplateSectionAccountResponseDto(
                entity.getId(),
                entity.getReportTemplateSection() != null ? entity.getReportTemplateSection().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                entity.getDisplayOrder()
        );
    }

    public ReportTemplateSectionAccount toEntityForCreate(ReportTemplateSectionAccountRequestDto dto,
                                                            ReportTemplateSection section,
                                                            AccountEntity account) {
        return ReportTemplateSectionAccount.builder()
                .reportTemplateSection(section)
                .account(account)
                .displayOrder(dto.displayOrder())
                .build();
    }

    public void applyUpdates(ReportTemplateSectionAccount entity,
                                ReportTemplateSectionAccountRequestDto dto,
                                ReportTemplateSection section,
                                AccountEntity account) {
        entity.setReportTemplateSection(section);
        entity.setAccount(account);
        entity.setDisplayOrder(dto.displayOrder());
    }
}

