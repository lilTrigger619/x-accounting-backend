package com.unionsg.xaccounting.service.reports.engine.view;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleAccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleReportSectionView;

public class TemplateReportSectionAdapter {

    public static ReportSectionView adapt(ReportTemplateSection section) {
        if (section == null) return null;

        String parentCode = section.getParentSection() != null ? section.getParentSection().getSectionCode() : null;

        SectionType sectionType = section.getSectionType();

        return new SimpleReportSectionView(
                section.getId(),
                section.getSectionCode(),
                section.getTitle(),
                sectionType,
                section.getFormula(),
                section.getDisplayOrder(),
                parentCode,
                java.util.List.of()
        );
    }

    public static AccountAssignmentView adapt(ReportTemplateSectionAccount rsa) {
        if (rsa == null || rsa.getAccount() == null || rsa.getReportTemplateSection() == null) return null;

        AccountEntity acc = rsa.getAccount();
        return new SimpleAccountAssignmentView(
                acc.getId(),
                acc.getAccountId(),
                acc.getAccountName(),
                rsa.getReportTemplateSection().getSectionCode()
        );
    }
}



