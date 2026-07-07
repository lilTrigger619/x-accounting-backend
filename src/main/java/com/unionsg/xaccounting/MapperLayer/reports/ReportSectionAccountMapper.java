package com.unionsg.xaccounting.MapperLayer.reports;

import com.unionsg.xaccounting.dto.reports.ReportSectionAccountResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportSectionAccount;
import org.springframework.stereotype.Component;

@Component
public class ReportSectionAccountMapper {

    public ReportSectionAccountResponseDto toResponse(ReportSectionAccount entity) {
        if (entity == null) {
            return null;
        }

        return new ReportSectionAccountResponseDto(
                entity.getId(),
                entity.getReportSection() != null ? entity.getReportSection().getId() : null,
                entity.getAccount() != null ? entity.getAccount().getId() : null,
                entity.getDisplayOrder()
        );
    }
}

