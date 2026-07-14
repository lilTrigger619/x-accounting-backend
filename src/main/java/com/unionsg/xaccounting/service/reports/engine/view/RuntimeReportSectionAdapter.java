package com.unionsg.xaccounting.service.reports.engine.view;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.entity.reports.ReportSectionAccount;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleAccountAssignmentView;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleReportSectionView;

import java.util.Comparator;

public class RuntimeReportSectionAdapter {

    public static ReportSectionView adapt(ReportSection section) {
        if (section == null) return null;

        String parentCode = section.getParentSection() != null ? section.getParentSection().getCode() : null;

        SectionType sectionType = section.getSectionType();

        return new SimpleReportSectionView(
                section.getId(),
                section.getCode(),
                section.getTitle(),
                sectionType,
                section.getFormula(),
                section.getDisplayOrder(),
                parentCode,
                java.util.List.of()
        );
    }

    public static AccountAssignmentView adapt(ReportSectionAccount rsa) {
        if (rsa == null || rsa.getAccount() == null || rsa.getReportSection() == null) return null;

        AccountEntity acc = rsa.getAccount();
        return new SimpleAccountAssignmentView(
                acc.getId(),
                acc.getAccountId(),
                acc.getAccountName(),
                rsa.getReportSection().getCode()
        );
    }
}



